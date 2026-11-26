package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.LearningStageContent;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStageProgress;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.LearningStageContentRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import jakarta.persistence.EntityNotFoundException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class StudentLearningImageServiceTest {
    private static final Long STUDENT_ID = 17L;
    private static final Long DISCUSSION_ID = 29L;

    @TempDir Path uploadDirectory;
    private LearningImageStorageService storage;
    private LearningStageContent stage;
    private List<StudentStageProgress> progressRows;
    private StudentLearningImageService service;

    @BeforeEach
    void setUp() throws Exception {
        storage = new LearningImageStorageService(uploadDirectory.toString());
        storage.prepareStorageDirectory();
        byte[] imageBytes;
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            imageBytes = output.toByteArray();
        } finally {
            image.flush();
        }
        var uploaded = storage.store(new MockMultipartFile("file", "image.png", "image/png", imageBytes));
        assertNotNull(uploaded.filename());

        Student student = new Student();
        student.setStatus(StudentStatus.ACTIVE);
        Competency competency = new Competency();
        competency.setActive(true);
        Discussion discussion = new Discussion();
        discussion.setId(DISCUSSION_ID);
        discussion.setCompetency(competency);
        discussion.setActive(true);
        stage = new LearningStageContent();
        stage.setDiscussion(discussion);
        stage.setStageCode("DEFINITIONS");
        stage.setActive(true);
        stage.setContentBody("![Board](/api/student/learning/images/" + uploaded.filename() + ")");
        progressRows = List.of(progress("DEFINITIONS", "AVAILABLE"));

        StudentRepository students = proxy(StudentRepository.class, (method, args) -> {
            if (method.equals("findById")) return Optional.of(student);
            throw new UnsupportedOperationException(method);
        });
        LearningStageContentRepository contents = proxy(LearningStageContentRepository.class, (method, args) -> {
            if (method.equals("findActiveContentReferencingImage")) {
                return stage.getContentBody().contains((String) args[0]) ? List.of(stage) : List.of();
            }
            throw new UnsupportedOperationException(method);
        });
        StudentStageProgressRepository progress = proxy(StudentStageProgressRepository.class, (method, args) -> {
            if (method.equals("findByStudent_IdAndDiscussion_Id")) return progressRows;
            throw new UnsupportedOperationException(method);
        });
        service = new StudentLearningImageService(students, contents, progress, storage);
    }

    @Test
    void servesImageOnlyWhenAnActiveAccessibleStageReferencesIt() {
        String filename = stage.getContentBody().substring(stage.getContentBody().lastIndexOf('/') + 1,
            stage.getContentBody().length() - 1);
        var result = service.getImageForStudent(STUDENT_ID, filename);

        assertEquals(filename, result.filename());
        assertEquals("image/png", result.contentType());
    }

    @Test
    void rejectsLockedStage() {
        progressRows = List.of(progress("DEFINITIONS", "LOCKED"));
        String filename = referencedFilename();

        assertThrows(EntityNotFoundException.class, () -> service.getImageForStudent(STUDENT_ID, filename));
    }

    @Test
    void checksStudentStageAuthorizationBeforeRetrievingCloudImage() {
        progressRows = List.of(progress("DEFINITIONS", "LOCKED"));
        java.util.concurrent.atomic.AtomicBoolean cloudDownloadCalled = new java.util.concurrent.atomic.AtomicBoolean();
        CloudinaryImageClient client = new CloudinaryImageClient() {
            @Override
            public void upload(byte[] bytes, String publicId, String format, String deliveryType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] download(String publicId, String format, String deliveryType) {
                cloudDownloadCalled.set(true);
                throw new AssertionError("Cloudinary must not be called for a locked stage image.");
            }
        };
        var cloudStorage = new CloudinaryImageStorageService(client);
        service = new StudentLearningImageService(
            proxy(StudentRepository.class, (method, args) -> Optional.of(activeStudent())),
            proxy(LearningStageContentRepository.class, (method, args) -> List.of(stage)),
            proxy(StudentStageProgressRepository.class, (method, args) -> progressRows),
            cloudStorage);

        assertThrows(EntityNotFoundException.class,
            () -> service.getImageForStudent(STUDENT_ID, referencedFilename()));
        assertFalse(cloudDownloadCalled.get());
    }

    @Test
    void rejectsEarlierStageImageAfterDiscussionPostTestHasStarted() {
        progressRows = List.of(progress("DEFINITIONS", "COMPLETED"), progress("POST_TEST", "IN_PROGRESS"));

        assertThrows(EntityNotFoundException.class, () -> service.getImageForStudent(STUDENT_ID, referencedFilename()));
    }

    @Test
    void rejectsInactiveStudents() {
        Student inactive = new Student();
        inactive.setStatus(StudentStatus.DEACTIVATED);
        StudentRepository students = proxy(StudentRepository.class, (method, args) -> Optional.of(inactive));
        service = new StudentLearningImageService(students,
            proxy(LearningStageContentRepository.class, (method, args) -> List.of()),
            proxy(StudentStageProgressRepository.class, (method, args) -> List.of()), storage);

        assertThrows(EntityNotFoundException.class, () -> service.getImageForStudent(STUDENT_ID, referencedFilename()));
    }

    private String referencedFilename() {
        return stage.getContentBody().substring(stage.getContentBody().lastIndexOf('/') + 1,
            stage.getContentBody().length() - 1);
    }

    private StudentStageProgress progress(String stageCode, String status) {
        StudentStageProgress progress = new StudentStageProgress();
        progress.setStageCode(stageCode);
        progress.setStatus(status);
        return progress;
    }

    private Student activeStudent() {
        Student student = new Student();
        student.setStatus(StudentStatus.ACTIVE);
        return student;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, RepositoryCall call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return method.getName();
            return call.invoke(method.getName(), args == null ? new Object[0] : args);
        });
    }

    @FunctionalInterface
    private interface RepositoryCall {
        Object invoke(String method, Object[] args);
    }
}
