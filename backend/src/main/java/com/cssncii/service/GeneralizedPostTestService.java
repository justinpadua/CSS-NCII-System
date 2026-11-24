package com.cssncii.service;

import com.cssncii.entity.Admin;
import com.cssncii.entity.AnswerChoice;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.AssessmentAttempt;
import com.cssncii.entity.AttemptAnswer;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.RetakeAuthorization;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentCompetencyProgress;
import com.cssncii.entity.StudentStageProgress;
import com.cssncii.repository.AdminRepository;
import com.cssncii.repository.AnswerChoiceRepository;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.AttemptAnswerRepository;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.QuestionRepository;
import com.cssncii.repository.RetakeAuthorizationRepository;
import com.cssncii.repository.StudentCompetencyProgressRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GeneralizedPostTestService {
    private static final String GENERALIZED_POST_TEST = "GENERALIZED_POST_TEST";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String LOCKED = "LOCKED";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String PASSED = "PASSED";
    private static final String FAILED = "FAILED";
    private static final String COMPETENCY_IN_PROGRESS = "IN_PROGRESS";
    private static final BigDecimal PASS_PERCENTAGE = new BigDecimal("70.00");
    private static final int INITIAL_ATTEMPT_ALLOWANCE = 3;
    private static final int ATTEMPTS_PER_AUTHORIZATION = 3;
    private static final int MAX_ATTEMPT_NUMBER = 65_535;
    private static final List<String> STAGE_CODES = List.of(
        "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"
    );

    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final CompetencyRepository competencyRepository;
    private final DiscussionRepository discussionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AnswerChoiceRepository choiceRepository;
    private final RetakeAuthorizationRepository authorizationRepository;
    private final StudentStageProgressRepository stageProgressRepository;
    private final StudentCompetencyProgressRepository competencyProgressRepository;
    private final StudentProgressService studentProgressService;
    private final AssessmentAttemptService assessmentAttemptService;

    public GeneralizedPostTestService(
        StudentRepository studentRepository,
        AdminRepository adminRepository,
        CompetencyRepository competencyRepository,
        DiscussionRepository discussionRepository,
        AssessmentRepository assessmentRepository,
        AssessmentAttemptRepository attemptRepository,
        AttemptAnswerRepository answerRepository,
        QuestionRepository questionRepository,
        AnswerChoiceRepository choiceRepository,
        RetakeAuthorizationRepository authorizationRepository,
        StudentStageProgressRepository stageProgressRepository,
        StudentCompetencyProgressRepository competencyProgressRepository,
        StudentProgressService studentProgressService,
        AssessmentAttemptService assessmentAttemptService
    ) {
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
        this.competencyRepository = competencyRepository;
        this.discussionRepository = discussionRepository;
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.choiceRepository = choiceRepository;
        this.authorizationRepository = authorizationRepository;
        this.stageProgressRepository = stageProgressRepository;
        this.competencyProgressRepository = competencyProgressRepository;
        this.studentProgressService = studentProgressService;
        this.assessmentAttemptService = assessmentAttemptService;
    }

    @Transactional
    public AttemptSummary startOrResumeAttempt(Long studentId, Long competencyId) {
        Student student = findActiveStudentForUpdate(studentId);
        Competency competency = findCompetency(competencyId);
        StudentCompetencyProgress progress = findCompetencyProgress(studentId, competencyId);
        requireNotPassed(progress);
        requireCompetencyUnlocked(studentId, competency, progress);
        requireAllDiscussionsCompleted(studentId, competencyId);

        Assessment assessment = findGeneralizedPostTest(competencyId);
        if (!assessment.isActive()) {
            throw new IllegalStateException("This Generalized Post-test is inactive.");
        }
        List<AssessmentAttempt> inProgress = attemptRepository
            .findAllByStudent_IdAndAssessment_IdAndStatusOrderByAttemptNumberAsc(
                studentId, assessment.getId(), IN_PROGRESS);
        if (inProgress.size() > 1) {
            throw new IllegalStateException("Multiple in-progress attempts exist for this Generalized Post-test.");
        }
        if (!inProgress.isEmpty()) {
            return toAttemptSummary(inProgress.getFirst(), studentId, competencyId, assessment.getId());
        }
        if (questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(assessment.getId()).isEmpty()) {
            throw new IllegalStateException("The Generalized Post-test has no configured questions.");
        }

        long submittedAttempts = countSubmittedAttempts(studentId, assessment.getId());
        long allowance = availableAttemptAllowance(studentId, competencyId);
        if (submittedAttempts >= allowance) {
            throw new IllegalStateException("All available Generalized Post-test attempts have been used.");
        }
        if (submittedAttempts + 1 > MAX_ATTEMPT_NUMBER) {
            throw new IllegalStateException("The attempt number exceeds the database limit.");
        }

        LocalDateTime now = utcNow();
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setStudent(student);
        attempt.setAssessment(assessment);
        attempt.setAttemptNumber((int) submittedAttempts + 1);
        attempt.setStatus(IN_PROGRESS);
        attempt.setStartedAt(now);
        attempt.setCreatedAt(now);
        attempt.setUpdatedAt(now);
        attempt.setScore(null);
        attempt.setTotalPossibleScore(null);
        attempt.setPercentage(null);
        attempt.setPassed(null);
        attempt.setSubmittedAt(null);
        AssessmentAttempt saved = attemptRepository.save(attempt);

        if (!PASSED.equals(progress.getStatus()) && !COMPETENCY_IN_PROGRESS.equals(progress.getStatus())) {
            progress.setStatus(COMPETENCY_IN_PROGRESS);
            progress.setUpdatedAt(now);
            competencyProgressRepository.save(progress);
        }
        return toAttemptSummary(saved, studentId, competencyId, assessment.getId());
    }

    public List<AssessmentAttemptService.QuestionPrompt> getQuestionsForAttempt(Long studentId, Long attemptId) {
        findActiveStudent(studentId);
        AssessmentAttempt attempt = findAttempt(attemptId);
        requireAttemptOwner(studentId, attempt);
        requireInProgress(attempt);

        List<AssessmentAttemptService.QuestionPrompt> prompts = new ArrayList<>();
        for (var question : questionRepository
            .findByAssessment_IdOrderByDisplayOrderAsc(attempt.getAssessment().getId())) {
            List<AssessmentAttemptService.ChoicePrompt> choices = choiceRepository
                .findByQuestion_IdOrderByDisplayOrderAsc(question.getId())
                .stream()
                .map(choice -> new AssessmentAttemptService.ChoicePrompt(
                    choice.getId(), choice.getChoiceText(), choice.getDisplayOrder()))
                .toList();
            prompts.add(new AssessmentAttemptService.QuestionPrompt(
                question.getId(), question.getQuestionType(), question.getQuestionText(),
                question.getPoints(), question.getDisplayOrder(), choices));
        }
        return prompts;
    }

    /** Returns only this student's saved responses, never the assessment answer key. */
    public List<AssessmentAttemptService.SavedAnswer> getSavedAnswers(Long studentId, Long attemptId) {
        findActiveStudent(studentId);
        AssessmentAttempt attempt = findAttempt(attemptId);
        requireAttemptOwner(studentId, attempt);
        requireInProgress(attempt);
        return answerRepository.findByAttempt_Id(attemptId).stream()
            .map(answer -> new AssessmentAttemptService.SavedAnswer(
                answer.getQuestion().getId(), answer.getResponseData()))
            .toList();
    }

    @Transactional
    public AttemptAnswer saveAnswer(Long studentId, Long attemptId, Long questionId, String responseData) {
        findActiveStudentForUpdate(studentId);
        AssessmentAttempt attempt = findAttempt(attemptId);
        requireAttemptOwner(studentId, attempt);
        requireInProgress(attempt);
        requirePositiveId("questionId", questionId);
        if (responseData == null || responseData.isBlank()) {
            throw new IllegalArgumentException("responseData is required.");
        }

        var question = questionRepository.findById(questionId)
            .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));
        if (!question.getAssessment().getId().equals(attempt.getAssessment().getId())) {
            throw new IllegalArgumentException("Question does not belong to this Generalized Post-test.");
        }
        List<AnswerChoice> choices = choiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(questionId);
        assessmentAttemptService.scoreResponse(question, responseData, choices);

        LocalDateTime now = utcNow();
        AttemptAnswer answer = answerRepository.findByAttempt_IdAndQuestion_Id(attemptId, questionId)
            .orElseGet(() -> {
                AttemptAnswer created = new AttemptAnswer();
                created.setAttempt(attempt);
                created.setQuestion(question);
                created.setCreatedAt(now);
                return created;
            });
        answer.setResponseData(responseData);
        answer.setCorrect(null);
        answer.setAwardedPoints(null);
        answer.setUpdatedAt(now);
        attempt.setUpdatedAt(now);
        attemptRepository.save(attempt);
        return answerRepository.save(answer);
    }

    @Transactional
    public SubmissionResult submitAttempt(Long studentId, Long attemptId) {
        findActiveStudentForUpdate(studentId);
        AssessmentAttempt attempt = findAttempt(attemptId);
        requireAttemptOwner(studentId, attempt);
        requireInProgress(attempt);
        Competency competency = attempt.getAssessment().getCompetency();
        StudentCompetencyProgress progress = findCompetencyProgress(studentId, competency.getId());
        requireNotPassed(progress);
        requireCompetencyUnlocked(studentId, competency, progress);
        requireAllDiscussionsCompleted(studentId, competency.getId());

        long submittedBefore = countSubmittedAttempts(studentId, attempt.getAssessment().getId());
        long allowance = availableAttemptAllowance(studentId, competency.getId());
        if (submittedBefore >= allowance) {
            throw new IllegalStateException("No Generalized Post-test attempts remain for submission.");
        }

        List<com.cssncii.entity.Question> questions = questionRepository
            .findByAssessment_IdOrderByDisplayOrderAsc(attempt.getAssessment().getId());
        if (questions.isEmpty()) {
            throw new IllegalStateException("The Generalized Post-test has no configured questions.");
        }
        Map<Long, AttemptAnswer> answersByQuestionId = new HashMap<>();
        for (AttemptAnswer answer : answerRepository.findByAttempt_Id(attemptId)) {
            answersByQuestionId.put(answer.getQuestion().getId(), answer);
        }
        for (var question : questions) {
            if (!answersByQuestionId.containsKey(question.getId())) {
                throw new IllegalArgumentException("Every question must be answered before submission.");
            }
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal totalPossibleScore = BigDecimal.ZERO;
        LocalDateTime now = utcNow();
        for (var question : questions) {
            AttemptAnswer answer = answersByQuestionId.get(question.getId());
            List<AnswerChoice> choices = choiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(question.getId());
            AssessmentAttemptService.ScoredAnswer scored =
                assessmentAttemptService.scoreResponse(question, answer.getResponseData(), choices);
            answer.setCorrect(scored.correct());
            answer.setAwardedPoints(scored.awardedPoints());
            answer.setUpdatedAt(now);
            answerRepository.save(answer);
            totalScore = totalScore.add(scored.awardedPoints());
            totalPossibleScore = totalPossibleScore.add(question.getPoints());
        }
        if (totalPossibleScore.signum() <= 0) {
            throw new IllegalStateException("Assessment total possible points must be greater than zero.");
        }

        boolean passed = meetsPassingThreshold(totalScore, totalPossibleScore);
        BigDecimal percentage = totalScore.multiply(BigDecimal.valueOf(100))
            .divide(totalPossibleScore, 2, RoundingMode.HALF_UP);
        attempt.setScore(totalScore);
        attempt.setTotalPossibleScore(totalPossibleScore);
        attempt.setPercentage(percentage);
        attempt.setPassed(passed);
        attempt.setStatus(SUBMITTED);
        attempt.setSubmittedAt(now);
        attempt.setUpdatedAt(now);
        attemptRepository.save(attempt);

        StudentCompetencyProgress updatedProgress = studentProgressService
            .recordGeneralizedPostTestResult(attempt.getId());
        long submittedAfter = countSubmittedAttempts(studentId, attempt.getAssessment().getId());
        long remaining = PASSED.equals(updatedProgress.getStatus())
            ? 0 : Math.max(0, allowance - submittedAfter);
        return new SubmissionResult(
            attempt.getId(), attempt.getAttemptNumber(), competency.getCode(), totalScore,
            totalPossibleScore, percentage, passed, updatedProgress.getStatus(), submittedAfter, remaining,
            attempt.getSubmittedAt());
    }

    public PostTestStatus getStatus(Long studentId, Long competencyId) {
        findActiveStudent(studentId);
        Competency competency = findCompetency(competencyId);
        StudentCompetencyProgress progress = findCompetencyProgress(studentId, competencyId);
        Assessment assessment = findGeneralizedPostTest(competencyId);
        if (!assessment.isActive()) {
            throw new EntityNotFoundException("Generalized Post-test is not published.");
        }
        long submitted = countSubmittedAttempts(studentId, assessment.getId());
        long allowance = availableAttemptAllowance(studentId, competencyId);
        long remaining = PASSED.equals(progress.getStatus()) ? 0 : Math.max(0, allowance - submitted);
        boolean inProgress = !attemptRepository.findAllByStudent_IdAndAssessment_IdAndStatusOrderByAttemptNumberAsc(
            studentId, assessment.getId(), IN_PROGRESS).isEmpty();
        return new PostTestStatus(
            competency.getId(), competency.getCode(), progress.getStatus(), submitted, allowance,
            remaining, inProgress, PASSED.equals(progress.getStatus()));
    }

    public List<AttemptHistory> getAttemptHistory(Long studentId, Long competencyId) {
        findActiveStudent(studentId);
        findCompetency(competencyId);
        Assessment assessment = findGeneralizedPostTest(competencyId);
        return attemptRepository.findByStudent_IdAndAssessment_IdOrderByAttemptNumberAsc(studentId, assessment.getId())
            .stream()
            .map(attempt -> new AttemptHistory(
                attempt.getId(), attempt.getAttemptNumber(), attempt.getStatus(), attempt.getScore(),
                attempt.getTotalPossibleScore(), attempt.getPercentage(), attempt.getPassed(),
                attempt.getStartedAt(), attempt.getSubmittedAt()))
            .toList();
    }

    @Transactional
    public RetakeAuthorizationResult authorizeAdditionalAttempts(Long adminId, Long studentId, Long competencyId) {
        requirePositiveId("adminId", adminId);
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new EntityNotFoundException("Admin not found: " + adminId));
        Student student = findActiveStudentForUpdate(studentId);
        Competency competency = findCompetency(competencyId);
        StudentCompetencyProgress progress = findCompetencyProgress(studentId, competencyId);
        requireCompetencyUnlocked(studentId, competency, progress);
        requireAllDiscussionsCompleted(studentId, competencyId);
        Assessment assessment = findGeneralizedPostTest(competencyId);
        requireNotPassed(progress);
        ensureNoPassedAttempt(studentId, assessment.getId());

        List<AssessmentAttempt> inProgress = attemptRepository
            .findAllByStudent_IdAndAssessment_IdAndStatusOrderByAttemptNumberAsc(
                studentId, assessment.getId(), IN_PROGRESS);
        if (!inProgress.isEmpty()) {
            throw new IllegalStateException("Retake authorization is available only after all attempts are submitted.");
        }
        long submitted = countSubmittedAttempts(studentId, assessment.getId());
        long currentAllowance = availableAttemptAllowance(studentId, competencyId);
        if (submitted < currentAllowance) {
            throw new IllegalStateException("Additional attempts can be authorized only after the current allowance is exhausted.");
        }

        LocalDateTime now = utcNow();
        RetakeAuthorization authorization = new RetakeAuthorization();
        authorization.setStudent(student);
        authorization.setCompetency(competency);
        authorization.setAdmin(admin);
        authorization.setAuthorizedAt(now);
        RetakeAuthorization saved = authorizationRepository.save(authorization);

        if (!PASSED.equals(progress.getStatus())) {
            progress.setStatus(AVAILABLE);
            progress.setUpdatedAt(now);
            competencyProgressRepository.save(progress);
        }
        return new RetakeAuthorizationResult(
            saved.getId(), studentId, competencyId, adminId, saved.getAuthorizedAt(), ATTEMPTS_PER_AUTHORIZATION);
    }

    public List<RetakeAuthorizationResult> getAuthorizationHistory(Long studentId, Long competencyId) {
        findActiveStudent(studentId);
        findCompetency(competencyId);
        return authorizationRepository.findByStudent_IdAndCompetency_IdOrderByAuthorizedAtAsc(studentId, competencyId)
            .stream()
            .map(authorization -> new RetakeAuthorizationResult(
                authorization.getId(), authorization.getStudent().getId(),
                authorization.getCompetency().getId(), authorization.getAdmin().getId(),
                authorization.getAuthorizedAt(), ATTEMPTS_PER_AUTHORIZATION))
            .toList();
    }

    private AttemptSummary toAttemptSummary(
        AssessmentAttempt attempt, Long studentId, Long competencyId, Long assessmentId
    ) {
        long submitted = countSubmittedAttempts(studentId, assessmentId);
        long allowance = availableAttemptAllowance(studentId, competencyId);
        return new AttemptSummary(
            attempt.getId(), attempt.getAssessment().getId(), competencyId,
            attempt.getAssessment().getCompetency().getCode(), attempt.getAttemptNumber(), attempt.getStatus(),
            attempt.getStartedAt(), submitted, Math.max(0, allowance - submitted));
    }

    private void requireCompetencyUnlocked(
        Long studentId, Competency competency, StudentCompetencyProgress progress
    ) {
        if (LOCKED.equals(progress.getStatus())) {
            throw new IllegalStateException("This competency is locked.");
        }
        if ("COC2".equals(competency.getCode())) {
            Competency coc1 = competencyRepository.findByCode("COC1")
                .orElseThrow(() -> new IllegalStateException("COC 1 competency is not configured."));
            StudentCompetencyProgress coc1Progress = competencyProgressRepository
                .findByStudent_IdAndCompetency_Id(studentId, coc1.getId())
                .orElseThrow(() -> new IllegalStateException("COC 1 progress has not been initialized."));
            if (!PASSED.equals(coc1Progress.getStatus())) {
                throw new IllegalStateException("COC 2 remains locked until the student passes COC 1.");
            }
        }
    }

    private void requireAllDiscussionsCompleted(Long studentId, Long competencyId) {
        List<Discussion> discussions = discussionRepository
            .findByCompetency_IdOrderByDisplayOrderAsc(competencyId).stream()
            .filter(Discussion::isActive).toList();
        if (discussions.isEmpty()) {
            throw new IllegalStateException("The competency has no Discussions to complete.");
        }
        for (Discussion discussion : discussions) {
            List<StudentStageProgress> rows = stageProgressRepository
                .findByStudent_IdAndDiscussion_Id(studentId, discussion.getId());
            if (rows.size() != STAGE_CODES.size()) {
                throw new IllegalStateException("Student progress is incomplete for a required Discussion.");
            }
            for (String stageCode : STAGE_CODES) {
                boolean completed = rows.stream().anyMatch(row ->
                    stageCode.equals(row.getStageCode()) && "COMPLETED".equals(row.getStatus()));
                if (!completed) {
                    throw new IllegalStateException("All required Discussions must be completed first.");
                }
            }
        }
    }

    private void ensureNoPassedAttempt(Long studentId, Long assessmentId) {
        boolean passedAttempt = attemptRepository
            .findByStudent_IdAndAssessment_IdOrderByAttemptNumberAsc(studentId, assessmentId).stream()
            .anyMatch(attempt -> Boolean.TRUE.equals(attempt.getPassed()));
        if (passedAttempt) {
            throw new IllegalStateException("No additional attempts can be authorized after the student has passed.");
        }
    }

    private long availableAttemptAllowance(Long studentId, Long competencyId) {
        return INITIAL_ATTEMPT_ALLOWANCE
            + ATTEMPTS_PER_AUTHORIZATION * authorizationRepository
                .countByStudent_IdAndCompetency_Id(studentId, competencyId);
    }

    private long countSubmittedAttempts(Long studentId, Long assessmentId) {
        return attemptRepository.countByStudent_IdAndAssessment_IdAndStatus(studentId, assessmentId, SUBMITTED);
    }

    static boolean meetsPassingThreshold(BigDecimal score, BigDecimal totalPossibleScore) {
        return score.multiply(BigDecimal.valueOf(100))
            .compareTo(totalPossibleScore.multiply(PASS_PERCENTAGE)) >= 0;
    }

    private Assessment findGeneralizedPostTest(Long competencyId) {
        return assessmentRepository.findByCompetency_IdAndAssessmentType(competencyId, GENERALIZED_POST_TEST)
            .orElseThrow(() -> new EntityNotFoundException(
                "Generalized Post-test not found for competency: " + competencyId));
    }

    private AssessmentAttempt findAttempt(Long attemptId) {
        requirePositiveId("attemptId", attemptId);
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() -> new EntityNotFoundException("Assessment attempt not found: " + attemptId));
        validateGeneralizedAssessment(attempt.getAssessment());
        return attempt;
    }

    private void validateGeneralizedAssessment(Assessment assessment) {
        if (!GENERALIZED_POST_TEST.equals(assessment.getAssessmentType())
            || assessment.getCompetency() == null || assessment.getDiscussion() != null) {
            throw new IllegalArgumentException("Only Generalized Post-test attempts are handled by this service.");
        }
    }

    private void requireAttemptOwner(Long studentId, AssessmentAttempt attempt) {
        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new EntityNotFoundException("Assessment attempt not found: " + attempt.getId());
        }
    }

    private void requireInProgress(AssessmentAttempt attempt) {
        if (!IN_PROGRESS.equals(attempt.getStatus())) {
            throw new IllegalStateException("This Generalized Post-test attempt is no longer in progress.");
        }
    }

    private Student findActiveStudent(Long studentId) {
        requirePositiveId("studentId", studentId);
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));
        if (!student.isActive()) {
            throw new IllegalStateException("Inactive students cannot access Generalized Post-tests.");
        }
        return student;
    }

    private Student findActiveStudentForUpdate(Long studentId) {
        requirePositiveId("studentId", studentId);
        Student student = studentRepository.findByIdForUpdate(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));
        if (!student.isActive()) {
            throw new IllegalStateException("Inactive students cannot access Generalized Post-tests.");
        }
        return student;
    }

    private Competency findCompetency(Long competencyId) {
        requirePositiveId("competencyId", competencyId);
        Competency competency = competencyRepository.findById(competencyId)
            .orElseThrow(() -> new EntityNotFoundException("Competency not found: " + competencyId));
        if (!"COC1".equals(competency.getCode()) && !"COC2".equals(competency.getCode())) {
            throw new IllegalArgumentException("Generalized Post-tests are supported only for COC 1 and COC 2.");
        }
        return competency;
    }

    private StudentCompetencyProgress findCompetencyProgress(Long studentId, Long competencyId) {
        return competencyProgressRepository.findByStudent_IdAndCompetency_Id(studentId, competencyId)
            .orElseThrow(() -> new IllegalStateException("Student competency progress has not been initialized."));
    }

    private void requireNotPassed(StudentCompetencyProgress progress) {
        if (PASSED.equals(progress.getStatus())) {
            throw new IllegalStateException("The student has already passed this competency.");
        }
    }

    private void requirePositiveId(String field, Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(field + " must be a positive ID.");
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public record AttemptSummary(
        Long attemptId,
        Long assessmentId,
        Long competencyId,
        String competencyCode,
        int attemptNumber,
        String status,
        LocalDateTime startedAt,
        long submittedAttempts,
        long attemptsRemaining
    ) {}

    public record PostTestStatus(
        Long competencyId,
        String competencyCode,
        String competencyStatus,
        long submittedAttempts,
        long allowedAttempts,
        long attemptsRemaining,
        boolean hasInProgressAttempt,
        boolean passed
    ) {}

    public record AttemptHistory(
        Long attemptId,
        int attemptNumber,
        String status,
        BigDecimal score,
        BigDecimal totalPossibleScore,
        BigDecimal percentage,
        Boolean passed,
        LocalDateTime startedAt,
        LocalDateTime submittedAt
    ) {}

    public record SubmissionResult(
        Long attemptId,
        int attemptNumber,
        String competencyCode,
        BigDecimal score,
        BigDecimal totalPossibleScore,
        BigDecimal percentage,
        boolean passed,
        String competencyStatus,
        long submittedAttempts,
        long attemptsRemaining,
        LocalDateTime submittedAt
    ) {}

    public record RetakeAuthorizationResult(
        Long authorizationId,
        Long studentId,
        Long competencyId,
        Long adminId,
        LocalDateTime authorizedAt,
        int attemptsGranted
    ) {}
}
