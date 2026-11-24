package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cssncii.entity.Admin;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.AssessmentAttempt;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.RetakeAuthorization;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStatus;
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
import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GeneralizedPostTestServiceTest {
    private StudentRepository studentRepository;
    private AdminRepository adminRepository;
    private CompetencyRepository competencyRepository;
    private DiscussionRepository discussionRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentAttemptRepository attemptRepository;
    private AttemptAnswerRepository answerRepository;
    private QuestionRepository questionRepository;
    private AnswerChoiceRepository choiceRepository;
    private RetakeAuthorizationRepository authorizationRepository;
    private StudentStageProgressRepository stageProgressRepository;
    private StudentCompetencyProgressRepository competencyProgressRepository;
    private StudentProgressService studentProgressService;
    private AssessmentAttemptService assessmentAttemptService;
    private GeneralizedPostTestService service;
    private Student student;
    private Competency competency;
    private Assessment assessment;
    private StudentCompetencyProgress progress;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        adminRepository = mock(AdminRepository.class);
        competencyRepository = mock(CompetencyRepository.class);
        discussionRepository = mock(DiscussionRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        attemptRepository = mock(AssessmentAttemptRepository.class);
        answerRepository = mock(AttemptAnswerRepository.class);
        questionRepository = mock(QuestionRepository.class);
        choiceRepository = mock(AnswerChoiceRepository.class);
        authorizationRepository = mock(RetakeAuthorizationRepository.class);
        stageProgressRepository = mock(StudentStageProgressRepository.class);
        competencyProgressRepository = mock(StudentCompetencyProgressRepository.class);
        studentProgressService = mock(StudentProgressService.class);
        assessmentAttemptService = mock(AssessmentAttemptService.class);
        service = new GeneralizedPostTestService(
            studentRepository, adminRepository, competencyRepository, discussionRepository,
            assessmentRepository, attemptRepository, answerRepository, questionRepository,
            choiceRepository, authorizationRepository, stageProgressRepository,
            competencyProgressRepository, studentProgressService, assessmentAttemptService);

        student = new Student();
        student.setId(8L);
        student.setStatus(StudentStatus.ACTIVE);
        competency = new Competency();
        competency.setId(2L);
        competency.setCode("COC1");
        progress = new StudentCompetencyProgress();
        progress.setStatus("IN_PROGRESS");

        assessment = new Assessment();
        assessment.setId(22L);
        assessment.setAssessmentType("GENERALIZED_POST_TEST");
        assessment.setCompetency(competency);
        assessment.setActive(true);

        when(studentRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(student));
        when(competencyRepository.findById(2L)).thenReturn(Optional.of(competency));
        when(competencyProgressRepository.findByStudent_IdAndCompetency_Id(8L, 2L))
            .thenReturn(Optional.of(progress));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(2L))
            .thenReturn(List.of(completedDiscussion(4L)));
        when(stageProgressRepository.findByStudent_IdAndDiscussion_Id(8L, 4L))
            .thenReturn(completedStages());
        when(assessmentRepository.findByCompetency_IdAndAssessmentType(2L, "GENERALIZED_POST_TEST"))
            .thenReturn(Optional.of(assessment));
        when(authorizationRepository.countByStudent_IdAndCompetency_Id(8L, 2L)).thenReturn(0L);
    }

    @Test
    void inactiveGeneralizedPostTestIsHiddenFromStatusAndCannotStart() {
        assessment.setActive(false);

        assertThrows(EntityNotFoundException.class, () -> service.getStatus(8L, 2L));
        assertThrows(IllegalStateException.class, () -> service.startOrResumeAttempt(8L, 2L));
        verify(attemptRepository, never()).save(any(AssessmentAttempt.class));
    }

    @Test
    void resumesExistingAttemptWithoutChangingItsNumberOrCreatingAnother() {
        AssessmentAttempt interrupted = attempt(3, "IN_PROGRESS");
        when(attemptRepository.findAllByStudent_IdAndAssessment_IdAndStatusOrderByAttemptNumberAsc(
            8L, 22L, "IN_PROGRESS")).thenReturn(List.of(interrupted));
        when(attemptRepository.countByStudent_IdAndAssessment_IdAndStatus(8L, 22L, "SUBMITTED"))
            .thenReturn(2L);

        GeneralizedPostTestService.AttemptSummary result = service.startOrResumeAttempt(8L, 2L);

        assertEquals(3, result.attemptNumber());
        assertEquals(interrupted.getId(), result.attemptId());
        assertEquals(2, result.submittedAttempts());
        verify(attemptRepository, never()).save(any(AssessmentAttempt.class));
        verify(answerRepository, never()).save(any());
    }

    @Test
    void newAttemptNumberCountsSubmittedAttemptsOnly() {
        when(attemptRepository.findAllByStudent_IdAndAssessment_IdAndStatusOrderByAttemptNumberAsc(
            8L, 22L, "IN_PROGRESS")).thenReturn(List.of());
        when(questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(22L)).thenReturn(List.of(question()));
        when(attemptRepository.countByStudent_IdAndAssessment_IdAndStatus(8L, 22L, "SUBMITTED"))
            .thenReturn(1L);
        when(attemptRepository.save(any(AssessmentAttempt.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        GeneralizedPostTestService.AttemptSummary result = service.startOrResumeAttempt(8L, 2L);

        assertEquals(2, result.attemptNumber());
        assertEquals(1, result.submittedAttempts());
        ArgumentCaptor<AssessmentAttempt> captor = ArgumentCaptor.forClass(AssessmentAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertEquals("IN_PROGRESS", captor.getValue().getStatus());
        assertNotNull(captor.getValue().getStartedAt());
    }

    @Test
    void exhaustedAllowanceGrantsExactlyThreeAdditionalAttempts() {
        progress.setStatus("FAILED");
        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(5L);
        when(adminRepository.findById(5L)).thenReturn(Optional.of(admin));
        when(attemptRepository.findAllByStudent_IdAndAssessment_IdAndStatusOrderByAttemptNumberAsc(
            8L, 22L, "IN_PROGRESS")).thenReturn(List.of());
        when(attemptRepository.findByStudent_IdAndAssessment_IdOrderByAttemptNumberAsc(8L, 22L))
            .thenReturn(List.of());
        when(attemptRepository.countByStudent_IdAndAssessment_IdAndStatus(8L, 22L, "SUBMITTED"))
            .thenReturn(3L);
        when(authorizationRepository.save(any(RetakeAuthorization.class)))
            .thenAnswer(invocation -> {
                RetakeAuthorization authorization = invocation.getArgument(0);
                authorization.setId(14L);
                return authorization;
            });

        GeneralizedPostTestService.RetakeAuthorizationResult result =
            service.authorizeAdditionalAttempts(5L, 8L, 2L);

        assertEquals(3, result.attemptsGranted());
        assertEquals(5L, result.adminId());
        assertNotNull(result.authorizedAt());
        assertEquals("AVAILABLE", progress.getStatus());
    }

    @Test
    void comparesPassingThresholdBeforePercentageRounding() {
        assertTrue(GeneralizedPostTestService.meetsPassingThreshold(
            new BigDecimal("70.00"), new BigDecimal("100.00")));
        assertFalse(GeneralizedPostTestService.meetsPassingThreshold(
            new BigDecimal("699.99"), new BigDecimal("1000.00")));
    }

    private AssessmentAttempt attempt(int number, String status) {
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setId(31L);
        attempt.setStudent(student);
        attempt.setAssessment(assessment);
        attempt.setAttemptNumber(number);
        attempt.setStatus(status);
        return attempt;
    }

    private Discussion completedDiscussion(Long id) {
        Discussion discussion = new Discussion();
        discussion.setId(id);
        discussion.setCompetency(competency);
        return discussion;
    }

    private List<StudentStageProgress> completedStages() {
        List<StudentStageProgress> rows = new java.util.ArrayList<>();
        for (String code : List.of(
            "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST")) {
            StudentStageProgress row = new StudentStageProgress();
            row.setStageCode(code);
            row.setStatus("COMPLETED");
            rows.add(row);
        }
        return rows;
    }

    private com.cssncii.entity.Question question() {
        return new com.cssncii.entity.Question();
    }
}
