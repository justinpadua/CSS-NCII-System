package com.cssncii.service;

import com.cssncii.entity.Assessment;
import com.cssncii.entity.AssessmentAttempt;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentCompetencyProgress;
import com.cssncii.entity.StudentStageProgress;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.RetakeAuthorizationRepository;
import com.cssncii.repository.StudentCompetencyProgressRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentProgressService {
    private static final String LOCKED = "LOCKED";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final String PASSED = "PASSED";
    private static final String SUBMITTED = "SUBMITTED";
    private static final List<String> STAGE_CODES = List.of(
        "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"
    );

    private final StudentRepository studentRepository;
    private final CompetencyRepository competencyRepository;
    private final DiscussionRepository discussionRepository;
    private final StudentStageProgressRepository stageProgressRepository;
    private final StudentCompetencyProgressRepository competencyProgressRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final RetakeAuthorizationRepository retakeAuthorizationRepository;

    public StudentProgressService(
        StudentRepository studentRepository,
        CompetencyRepository competencyRepository,
        DiscussionRepository discussionRepository,
        StudentStageProgressRepository stageProgressRepository,
        StudentCompetencyProgressRepository competencyProgressRepository,
        AssessmentRepository assessmentRepository,
        AssessmentAttemptRepository attemptRepository,
        RetakeAuthorizationRepository retakeAuthorizationRepository
    ) {
        this.studentRepository = studentRepository;
        this.competencyRepository = competencyRepository;
        this.discussionRepository = discussionRepository;
        this.stageProgressRepository = stageProgressRepository;
        this.competencyProgressRepository = competencyProgressRepository;
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
        this.retakeAuthorizationRepository = retakeAuthorizationRepository;
    }

    @Transactional
    public List<StudentCompetencyProgress> initializeStudentProgress(Long studentId) {
        Student student = findStudentForUpdate(studentId);
        List<Competency> competencies = competencyRepository.findAll(
            Sort.by(Sort.Direction.ASC, "displayOrder"));
        Map<String, StudentCompetencyProgress> progressByCode = new HashMap<>();

        for (Competency competency : competencies) {
            String code = competency.getCode();
            if (!code.equals("COC1") && !code.equals("COC2")) {
                continue;
            }
            StudentCompetencyProgress progress = competencyProgressRepository
                .findByStudent_IdAndCompetency_Id(studentId, competency.getId())
                .orElseGet(() -> createCompetencyProgress(student, competency, code));
            progressByCode.put(code, progress);
        }

        StudentCompetencyProgress coc1 = progressByCode.get("COC1");
        StudentCompetencyProgress coc2 = progressByCode.get("COC2");
        if (coc1 == null || coc2 == null) {
            throw new IllegalStateException("Both COC 1 and COC 2 competencies must exist before progress can be initialized.");
        }
        if (PASSED.equals(coc1.getStatus()) && LOCKED.equals(coc2.getStatus())) {
            coc2.setStatus(AVAILABLE);
            coc2.setUpdatedAt(utcNow());
            competencyProgressRepository.save(coc2);
        }

        for (Competency competency : competencies) {
            StudentCompetencyProgress competencyProgress = progressByCode.get(competency.getCode());
            if (competencyProgress == null) {
                continue;
            }
            List<Discussion> discussions = discussionRepository
                .findByCompetency_IdOrderByDisplayOrderAsc(competency.getId()).stream()
                .filter(Discussion::isActive).toList();
            for (int discussionIndex = 0; discussionIndex < discussions.size(); discussionIndex++) {
                Discussion discussion = discussions.get(discussionIndex);
                Map<String, StudentStageProgress> existing = new HashMap<>();
                for (StudentStageProgress row : stageProgressRepository
                    .findByStudent_IdAndDiscussion_Id(studentId, discussion.getId())) {
                    existing.put(row.getStageCode(), row);
                }

                for (int stageIndex = 0; stageIndex < STAGE_CODES.size(); stageIndex++) {
                    String stageCode = STAGE_CODES.get(stageIndex);
                    if (!existing.containsKey(stageCode)) {
                        boolean firstStageAvailable = discussionIndex == 0 && stageIndex == 0
                            && !LOCKED.equals(competencyProgress.getStatus());
                        stageProgressRepository.save(newStageProgress(
                            student, discussion, stageCode, firstStageAvailable ? AVAILABLE : LOCKED));
                    }
                }
            }
            refreshDiscussionSequence(studentId, competencyProgress, discussions);
        }

        return competencyProgressRepository.findByStudent_IdOrderByCompetency_DisplayOrderAsc(studentId);
    }

    public List<StudentCompetencyProgress> getCompetencyProgress(Long studentId) {
        findStudent(studentId);
        return competencyProgressRepository.findByStudent_IdOrderByCompetency_DisplayOrderAsc(studentId);
    }

    public List<StudentStageProgress> getStageProgress(Long studentId, Long competencyId) {
        findStudent(studentId);
        Competency competency = findCompetency(competencyId);
        List<StudentStageProgress> result = new ArrayList<>();
        for (Discussion discussion : discussionRepository
            .findByCompetency_IdOrderByDisplayOrderAsc(competency.getId()).stream()
                .filter(Discussion::isActive).toList()) {
            List<StudentStageProgress> rows = stageProgressRepository
                .findByStudent_IdAndDiscussion_Id(studentId, discussion.getId());
            rows.sort(Comparator.comparingInt(row -> stageOrder(row.getStageCode())));
            result.addAll(rows);
        }
        return result;
    }

    /** Marks an unlocked stage as in progress when the student opens it. */
    @Transactional
    public StudentStageProgress startStage(Long studentId, Long discussionId, String stageCode) {
        Student student = findStudent(studentId);
        Discussion discussion = findDiscussion(discussionId);
        if (!discussion.isActive() || !discussion.getCompetency().isActive()) {
            throw new EntityNotFoundException("Discussion not found.");
        }
        String normalizedStageCode = normalizeStageCode(stageCode);
        Competency competency = discussion.getCompetency();
        StudentCompetencyProgress competencyProgress = competencyProgressRepository
            .findByStudent_IdAndCompetency_Id(studentId, competency.getId())
            .orElseThrow(() -> new IllegalStateException("Student progress has not been initialized."));
        requireCompetencyUnlocked(studentId, competency);
        List<Discussion> discussions = discussionRepository
            .findByCompetency_IdOrderByDisplayOrderAsc(competency.getId()).stream()
            .filter(Discussion::isActive).toList();
        int discussionIndex = indexOfDiscussion(discussions, discussionId);
        List<StudentStageProgress> rows = stageProgressRepository
            .findByStudent_IdAndDiscussion_Id(studentId, discussionId);
        StudentStageProgress stage = rows.stream()
            .filter(row -> row.getStageCode().equals(normalizedStageCode))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Student progress has not been initialized for this stage."));

        boolean postTestStarted = stageProgressRepository.findByStudent_IdAndDiscussion_Id(studentId, discussionId)
            .stream().anyMatch(row -> "POST_TEST".equals(row.getStageCode())
                && IN_PROGRESS.equals(row.getStatus()));
        if (postTestStarted && !"POST_TEST".equals(normalizedStageCode)) {
            throw new IllegalStateException("Earlier stages are unavailable after the Discussion Post-test has started.");
        }
        if (COMPLETED.equals(stage.getStatus())) return stage;
        if (LOCKED.equals(competencyProgress.getStatus()) || !AVAILABLE.equals(stage.getStatus())
            && !IN_PROGRESS.equals(stage.getStatus())) {
            throw new IllegalStateException("This stage is locked until the previous stage is completed.");
        }
        validatePreviousStageCompleted(rows, discussionIndex, discussions, normalizedStageCode, studentId);
        if (AVAILABLE.equals(stage.getStatus())) {
            stage.setStatus(IN_PROGRESS);
            stage.setUpdatedAt(utcNow());
            stageProgressRepository.save(stage);
        }
        return stage;
    }

    @Transactional
    public StudentStageProgress completeStage(Long studentId, Long discussionId, String stageCode) {
        Student student = findStudent(studentId);
        Discussion discussion = findDiscussion(discussionId);
        if (!discussion.isActive() || !discussion.getCompetency().isActive()) {
            throw new EntityNotFoundException("Discussion not found.");
        }
        String normalizedStageCode = normalizeStageCode(stageCode);
        Competency competency = discussion.getCompetency();
        StudentCompetencyProgress competencyProgress = competencyProgressRepository
            .findByStudent_IdAndCompetency_Id(studentId, competency.getId())
            .orElseThrow(() -> new IllegalStateException("Student progress has not been initialized."));
        requireCompetencyUnlocked(studentId, competency);

        List<Discussion> discussions = discussionRepository
            .findByCompetency_IdOrderByDisplayOrderAsc(competency.getId()).stream()
            .filter(Discussion::isActive).toList();
        int discussionIndex = indexOfDiscussion(discussions, discussionId);
        List<StudentStageProgress> rows = stageProgressRepository
            .findByStudent_IdAndDiscussion_Id(studentId, discussionId);
        StudentStageProgress stage = rows.stream()
            .filter(row -> row.getStageCode().equals(normalizedStageCode))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Student progress has not been initialized for this stage."));

        if (COMPLETED.equals(stage.getStatus())) {
            return stage;
        }
        if (LOCKED.equals(competencyProgress.getStatus())
            || !(AVAILABLE.equals(stage.getStatus()) || IN_PROGRESS.equals(stage.getStatus()))) {
            throw new IllegalStateException("This stage is locked until the previous stage is completed.");
        }
        validatePreviousStageCompleted(rows, discussionIndex, discussions, normalizedStageCode, studentId);
        if (isAssessmentStage(normalizedStageCode)) {
            requireSubmittedDiscussionAssessment(studentId, discussionId, normalizedStageCode);
        }

        LocalDateTime now = utcNow();
        stage.setStatus(COMPLETED);
        stage.setCompletedAt(now);
        stage.setUpdatedAt(now);
        stageProgressRepository.save(stage);

        if (!PASSED.equals(competencyProgress.getStatus()) && !IN_PROGRESS.equals(competencyProgress.getStatus())) {
            competencyProgress.setStatus(IN_PROGRESS);
            competencyProgress.setUpdatedAt(now);
            competencyProgressRepository.save(competencyProgress);
        }
        refreshDiscussionSequence(studentId, competencyProgress, discussions);
        return stage;
    }

    /** Records the pass state from a submitted Generalized Post-test attempt. */
    @Transactional
    public StudentCompetencyProgress recordGeneralizedPostTestResult(Long attemptId) {
        requireId("attemptId", attemptId);
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() -> new EntityNotFoundException("Assessment attempt not found: " + attemptId));
        Assessment assessment = attempt.getAssessment();
        if (!SUBMITTED.equals(attempt.getStatus()) || assessment.getCompetency() == null
            || !"GENERALIZED_POST_TEST".equals(assessment.getAssessmentType())) {
            throw new IllegalArgumentException("Attempt must be a submitted Generalized Post-test.");
        }
        if (attempt.getPassed() == null) {
            throw new IllegalArgumentException("The submitted attempt has no calculated pass result.");
        }

        Student student = attempt.getStudent();
        Competency competency = assessment.getCompetency();
        requireCompetencyUnlocked(student.getId(), competency);
        StudentCompetencyProgress progress = competencyProgressRepository
            .findByStudent_IdAndCompetency_Id(student.getId(), competency.getId())
            .orElseThrow(() -> new IllegalStateException("Student competency progress has not been initialized."));
        if (LOCKED.equals(progress.getStatus())) {
            throw new IllegalStateException("This competency is locked.");
        }
        if (!allDiscussionsCompleted(student.getId(), competency.getId())) {
            throw new IllegalStateException("All Discussions must be completed before the Generalized Post-test can pass.");
        }

        if (PASSED.equals(progress.getStatus())) {
            return progress;
        }
        LocalDateTime now = utcNow();
        if (attempt.getPassed()) {
            progress.setStatus(PASSED);
            progress.setPassedAt(now);
        } else {
            long submittedAttempts = attemptRepository.countByStudent_IdAndAssessment_IdAndStatus(
                student.getId(), assessment.getId(), SUBMITTED);
            long allowedAttempts = 3 + 3 * retakeAuthorizationRepository
                .countByStudent_IdAndCompetency_Id(student.getId(), competency.getId());
            progress.setStatus(submittedAttempts >= allowedAttempts ? "FAILED" : IN_PROGRESS);
        }
        progress.setUpdatedAt(now);
        competencyProgressRepository.save(progress);

        if (PASSED.equals(progress.getStatus()) && "COC1".equals(competency.getCode())) {
            competencyRepository.findAll().stream()
                .filter(candidate -> "COC2".equals(candidate.getCode()))
                .findFirst()
                .flatMap(candidate -> competencyProgressRepository
                    .findByStudent_IdAndCompetency_Id(student.getId(), candidate.getId()))
                .filter(coc2 -> LOCKED.equals(coc2.getStatus()))
                .ifPresent(coc2 -> {
                    coc2.setStatus(AVAILABLE);
                    coc2.setUpdatedAt(now);
                    competencyProgressRepository.save(coc2);
                });
        }
        return progress;
    }

    private StudentCompetencyProgress createCompetencyProgress(
        Student student, Competency competency, String code
    ) {
        LocalDateTime now = utcNow();
        StudentCompetencyProgress progress = new StudentCompetencyProgress();
        progress.setStudent(student);
        progress.setCompetency(competency);
        progress.setStatus("COC1".equals(code) ? AVAILABLE : LOCKED);
        progress.setPassedAt(null);
        progress.setCreatedAt(now);
        progress.setUpdatedAt(now);
        return competencyProgressRepository.save(progress);
    }

    private StudentStageProgress newStageProgress(
        Student student, Discussion discussion, String stageCode, String status
    ) {
        LocalDateTime now = utcNow();
        StudentStageProgress progress = new StudentStageProgress();
        progress.setStudent(student);
        progress.setDiscussion(discussion);
        progress.setStageCode(stageCode);
        progress.setStatus(status);
        progress.setCompletedAt(null);
        progress.setCreatedAt(now);
        progress.setUpdatedAt(now);
        return progress;
    }

    private void refreshDiscussionSequence(
        Long studentId, StudentCompetencyProgress competencyProgress, List<Discussion> discussions
    ) {
        boolean canUnlockNext = !LOCKED.equals(competencyProgress.getStatus());
        LocalDateTime now = utcNow();
        for (Discussion discussion : discussions) {
            List<StudentStageProgress> rows = stageProgressRepository
                .findByStudent_IdAndDiscussion_Id(studentId, discussion.getId());
            rows.sort(Comparator.comparingInt(row -> stageOrder(row.getStageCode())));
            boolean discussionComplete = rows.size() == STAGE_CODES.size()
                && rows.stream().allMatch(row -> COMPLETED.equals(row.getStatus()));
            for (StudentStageProgress row : rows) {
                String nextStatus;
                if (COMPLETED.equals(row.getStatus())) {
                    nextStatus = COMPLETED;
                } else if (IN_PROGRESS.equals(row.getStatus())) {
                    nextStatus = IN_PROGRESS;
                    canUnlockNext = false;
                } else if (canUnlockNext) {
                    nextStatus = AVAILABLE;
                    canUnlockNext = false;
                } else {
                    nextStatus = LOCKED;
                }
                if (!row.getStatus().equals(nextStatus)) {
                    row.setStatus(nextStatus);
                    row.setUpdatedAt(now);
                    stageProgressRepository.save(row);
                }
            }
            canUnlockNext = discussionComplete && !LOCKED.equals(competencyProgress.getStatus());
        }
    }

    private void validatePreviousStageCompleted(
        List<StudentStageProgress> currentRows,
        int discussionIndex,
        List<Discussion> discussions,
        String stageCode,
        Long studentId
    ) {
        int order = stageOrder(stageCode) - 1;
        if (order > 0) {
            boolean previousCompleted = currentRows.stream().anyMatch(row ->
                stageOrder(row.getStageCode()) == order && COMPLETED.equals(row.getStatus()));
            if (!previousCompleted) {
                throw new IllegalStateException("The previous stage must be completed first.");
            }
        } else if (discussionIndex > 0) {
            List<StudentStageProgress> previousDiscussion = stageProgressRepository
                .findByStudent_IdAndDiscussion_Id(studentId, discussions.get(discussionIndex - 1).getId());
            if (previousDiscussion.size() != STAGE_CODES.size()
                || previousDiscussion.stream().anyMatch(row -> !COMPLETED.equals(row.getStatus()))) {
                throw new IllegalStateException("The previous Discussion must be completed first.");
            }
        }
    }

    private void requireSubmittedDiscussionAssessment(Long studentId, Long discussionId, String stageCode) {
        String assessmentType = switch (stageCode) {
            case "PRE_TEST" -> "PRE_TEST";
            case "ACTIVITY" -> "ACTIVITY";
            case "POST_TEST" -> "DISCUSSION_POST_TEST";
            default -> throw new IllegalArgumentException("The stage is not an assessment stage.");
        };
        Assessment assessment = assessmentRepository.findByDiscussion_IdAndAssessmentType(discussionId, assessmentType)
            .orElseThrow(() -> new IllegalStateException("The required Discussion assessment is not configured."));
        if (attemptRepository.countByStudent_IdAndAssessment_IdAndStatus(
            studentId, assessment.getId(), SUBMITTED) == 0) {
            throw new IllegalStateException("The required Discussion assessment must be submitted first.");
        }
    }

    private boolean allDiscussionsCompleted(Long studentId, Long competencyId) {
        List<Discussion> discussions = discussionRepository
            .findByCompetency_IdOrderByDisplayOrderAsc(competencyId).stream()
            .filter(Discussion::isActive).toList();
        if (discussions.isEmpty()) {
            return false;
        }
        for (Discussion discussion : discussions) {
            List<StudentStageProgress> rows = stageProgressRepository
                .findByStudent_IdAndDiscussion_Id(studentId, discussion.getId());
            if (rows.size() != STAGE_CODES.size()
                || rows.stream().anyMatch(row -> !COMPLETED.equals(row.getStatus()))) {
                return false;
            }
        }
        return true;
    }

    private void requireCompetencyUnlocked(Long studentId, Competency competency) {
        if (!"COC2".equals(competency.getCode())) {
            return;
        }
        Competency coc1 = competencyRepository.findAll().stream()
            .filter(candidate -> "COC1".equals(candidate.getCode()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("COC 1 competency is not configured."));
        StudentCompetencyProgress coc1Progress = competencyProgressRepository
            .findByStudent_IdAndCompetency_Id(studentId, coc1.getId())
            .orElseThrow(() -> new IllegalStateException("COC 1 progress has not been initialized."));
        if (!PASSED.equals(coc1Progress.getStatus())) {
            throw new IllegalStateException("COC 2 remains locked until the student passes COC 1.");
        }
    }

    private int indexOfDiscussion(List<Discussion> discussions, Long discussionId) {
        for (int i = 0; i < discussions.size(); i++) {
            if (discussions.get(i).getId().equals(discussionId)) {
                return i;
            }
        }
        throw new EntityNotFoundException("Discussion is not part of its competency: " + discussionId);
    }

    private boolean isAssessmentStage(String stageCode) {
        return "PRE_TEST".equals(stageCode) || "ACTIVITY".equals(stageCode) || "POST_TEST".equals(stageCode);
    }

    private int stageOrder(String stageCode) {
        return STAGE_CODES.indexOf(stageCode) + 1;
    }

    private String normalizeStageCode(String stageCode) {
        if (stageCode == null || !STAGE_CODES.contains(stageCode.trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("stageCode must be one of the seven standard stage codes.");
        }
        return stageCode.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private Student findStudent(Long studentId) {
        requireId("studentId", studentId);
        return studentRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));
    }

    private Student findStudentForUpdate(Long studentId) {
        requireId("studentId", studentId);
        return studentRepository.findByIdForUpdate(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));
    }

    private Competency findCompetency(Long competencyId) {
        requireId("competencyId", competencyId);
        return competencyRepository.findById(competencyId)
            .orElseThrow(() -> new EntityNotFoundException("Competency not found: " + competencyId));
    }

    private Discussion findDiscussion(Long discussionId) {
        requireId("discussionId", discussionId);
        return discussionRepository.findById(discussionId)
            .orElseThrow(() -> new EntityNotFoundException("Discussion not found: " + discussionId));
    }

    private void requireId(String fieldName, Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive ID.");
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
