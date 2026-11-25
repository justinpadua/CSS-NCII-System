package com.cssncii.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.LearningStageContent;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStageProgress;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.AdminRepository;
import com.cssncii.repository.LearningStageContentRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.security.StudentPrincipal;
import com.cssncii.service.LearningImageStorageService;
import com.cssncii.service.StudentLearningImageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;

@WebMvcTest(StudentLearningImageController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class, StudentLearningImageControllerTest.TestBeans.class})
class StudentLearningImageControllerTest {
    private static final Long STUDENT_ID = 73L;
    @Autowired private MockMvc mockMvc;
    @Autowired private LearningImageStorageService imageStorage;

    @Test
    void imageEndpointRequiresStudentRoleAndServesOnlyTheAuthenticatedStudentsAuthorizedReference() throws Exception {
        String filename = fixtureFilename();
        StudentPrincipal principal = new StudentPrincipal(STUDENT_ID, "STU-000073", "Test Student", "CSS NC II");

        mockMvc.perform(get("/api/student/learning/images/" + filename)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/student/learning/images/" + filename)
                .with(authentication(auth("ROLE_ADMIN"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/student/learning/images/" + filename)
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, "",
                    List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andExpect(content().bytes(Files.readAllBytes(imageStorage.get(filename).path())));
    }

    private String fixtureFilename() {
        return TestBeans.fixtureFilename;
    }

    private UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken("test", "", List.of(new SimpleGrantedAuthority(role)));
    }

    @TestConfiguration
    static class TestBeans {
        private static final Path STORAGE = Path.of(System.getProperty("java.io.tmpdir"),
            "css-ncii-student-image-tests-" + java.util.UUID.randomUUID());
        private static final Long DISCUSSION_ID = 291L;
        private static String fixtureFilename;

        @Bean AdminRepository adminRepository() {
            return proxy(AdminRepository.class, (method, args) -> {
                if (method.equals("findByUsername")) return Optional.empty();
                throw new UnsupportedOperationException(method);
            });
        }

        @Bean LearningImageStorageService learningImageStorageService() { return new LearningImageStorageService(STORAGE.toString()); }

        @Bean StudentRepository studentRepository() {
            Student student = new Student();
            student.setStatus(StudentStatus.ACTIVE);
            return proxy(StudentRepository.class, (method, args) -> {
                if (method.equals("findById")) return Optional.of(student);
                throw new UnsupportedOperationException(method);
            });
        }

        @Bean LearningStageContentRepository learningStageContentRepository(LearningImageStorageService storage) throws Exception {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            byte[] png;
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", output);
                png = output.toByteArray();
            } finally {
                image.flush();
            }
            var uploaded = storage.store(new MockMultipartFile("file", "lesson.png", "image/png", png));
            fixtureFilename = uploaded.filename();
            Competency competency = new Competency();
            competency.setActive(true);
            Discussion discussion = new Discussion();
            discussion.setId(DISCUSSION_ID);
            discussion.setActive(true);
            discussion.setCompetency(competency);
            LearningStageContent stage = new LearningStageContent();
            stage.setActive(true);
            stage.setStageCode("DEFINITIONS");
            stage.setDiscussion(discussion);
            stage.setContentBody("![Lesson](/api/student/learning/images/" + fixtureFilename + ")");
            return proxy(LearningStageContentRepository.class, (method, args) -> {
                if (method.equals("findActiveContentReferencingImage")) {
                    return stage.getContentBody().contains((String) args[0]) ? List.of(stage) : List.of();
                }
                throw new UnsupportedOperationException(method);
            });
        }

        @Bean StudentStageProgressRepository studentStageProgressRepository() {
            StudentStageProgress progress = new StudentStageProgress();
            progress.setStageCode("DEFINITIONS");
            progress.setStatus("AVAILABLE");
            return proxy(StudentStageProgressRepository.class, (method, args) -> {
                if (method.equals("findByStudent_IdAndDiscussion_Id")) return List.of(progress);
                throw new UnsupportedOperationException(method);
            });
        }

        @Bean StudentLearningImageService studentLearningImageService(
            StudentRepository students,
            LearningStageContentRepository contents,
            StudentStageProgressRepository progress,
            LearningImageStorageService storage
        ) {
            return new StudentLearningImageService(students, contents, progress, storage);
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, RepositoryCall call) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> type.getSimpleName() + " test proxy";
                        default -> null;
                    };
                }
                return call.invoke(method.getName(), args == null ? new Object[0] : args);
            });
        }

        @FunctionalInterface
        private interface RepositoryCall { Object invoke(String method, Object[] args); }
    }
}
