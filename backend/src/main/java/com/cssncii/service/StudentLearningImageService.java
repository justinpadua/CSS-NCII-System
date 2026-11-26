package com.cssncii.service;

import com.cssncii.entity.LearningStageContent;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStageProgress;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.LearningStageContentRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentLearningImageService {
    private static final String POST_TEST = "POST_TEST";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String LOCKED = "LOCKED";
    private static final String IMAGE_REFERENCE_PREFIX = "](/api/student/learning/images/";

    private final StudentRepository studentRepository;
    private final LearningStageContentRepository contentRepository;
    private final StudentStageProgressRepository stageProgressRepository;
    private final LearningImageStore imageStorage;

    public StudentLearningImageService(
        StudentRepository studentRepository,
        LearningStageContentRepository contentRepository,
        StudentStageProgressRepository stageProgressRepository,
        LearningImageStore imageStorage
    ) {
        this.studentRepository = studentRepository;
        this.contentRepository = contentRepository;
        this.stageProgressRepository = stageProgressRepository;
        this.imageStorage = imageStorage;
    }

    public LearningImageStore.StoredImage getImageForStudent(Long studentId, String filename) {
        if (!LearningImageValidation.isValidFilename(filename)) {
            throw new EntityNotFoundException("Learning image not found.");
        }
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found."));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new EntityNotFoundException("Learning image not found.");
        }
        String reference = IMAGE_REFERENCE_PREFIX + filename;
        List<LearningStageContent> candidateStages = contentRepository.findActiveContentReferencingImage(reference);
        for (LearningStageContent content : candidateStages) {
            if (!content.isActive()) continue;
            var discussion = content.getDiscussion();
            if (discussion == null || !discussion.isActive()
                || discussion.getCompetency() == null || !discussion.getCompetency().isActive()) continue;

            List<StudentStageProgress> progressRows = stageProgressRepository
                .findByStudent_IdAndDiscussion_Id(studentId, discussion.getId());
            StudentStageProgress stageProgress = progressRows.stream()
                .filter(row -> row.getStageCode().equals(content.getStageCode()))
                .findFirst().orElse(null);
            if (stageProgress == null || LOCKED.equals(stageProgress.getStatus())) continue;
            boolean postTestInProgress = progressRows.stream().anyMatch(row -> POST_TEST.equals(row.getStageCode())
                && IN_PROGRESS.equals(row.getStatus()));
            if (postTestInProgress && !POST_TEST.equals(content.getStageCode())) continue;
            return imageStorage.get(filename);
        }
        throw new EntityNotFoundException("Learning image not found.");
    }
}
