package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cssncii.dto.AdminReportingDtos.AssessmentStatistics;
import com.cssncii.dto.AdminReportingDtos.LearningContentStatistics;
import com.cssncii.dto.AdminReportingDtos.StudentStatistics;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentCompetencyProgress;
import com.cssncii.entity.StudentStageProgress;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.LearningStageContentRepository;
import com.cssncii.repository.RetakeAuthorizationRepository;
import com.cssncii.repository.StudentCompetencyProgressRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class AdminReportingServiceTest {
    private StudentRepository studentRepository;
    private AssessmentRepository assessmentRepository;
    private LearningStageContentRepository learningContentRepository;
    private CompetencyRepository competencyRepository;
    private DiscussionRepository discussionRepository;
    private StudentStageProgressRepository stageProgressRepository;
    private StudentCompetencyProgressRepository competencyProgressRepository;
    private AssessmentAttemptRepository attemptRepository;
    private RetakeAuthorizationRepository retakeAuthorizationRepository;
    private AdminReportingService service;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        learningContentRepository = mock(LearningStageContentRepository.class);
        competencyRepository = mock(CompetencyRepository.class);
        discussionRepository = mock(DiscussionRepository.class);
        stageProgressRepository = mock(StudentStageProgressRepository.class);
        competencyProgressRepository = mock(StudentCompetencyProgressRepository.class);
        attemptRepository = mock(AssessmentAttemptRepository.class);
        retakeAuthorizationRepository = mock(RetakeAuthorizationRepository.class);
        service = new AdminReportingService(studentRepository, assessmentRepository, learningContentRepository,
            competencyRepository, discussionRepository, stageProgressRepository, competencyProgressRepository,
            attemptRepository, retakeAuthorizationRepository);
    }

    @Test
    void dashboardReportsZeroCountsWithoutInventingStatistics() {
        var summary = service.getDashboardSummary();
        assertEquals(0, summary.students().total());
        assertEquals(0, summary.assessments().total());
        assertEquals(0, summary.learningContent().total());
        assertEquals(0, summary.learningContent().draft());
    }

    @Test
    void dashboardStatisticsReflectRepositoryCountsForEachStatusAndCourse() {
        when(studentRepository.count()).thenReturn(5L);
        when(studentRepository.countByStatus(StudentStatus.ACTIVE)).thenReturn(1L);
        when(studentRepository.countByStatus(StudentStatus.PENDING)).thenReturn(2L);
        when(studentRepository.countByStatus(StudentStatus.REJECTED)).thenReturn(1L);
        when(studentRepository.countByStatus(StudentStatus.DEACTIVATED)).thenReturn(1L);
        when(assessmentRepository.count()).thenReturn(8L);
        when(assessmentRepository.countByActiveTrue()).thenReturn(5L);
        when(assessmentRepository.countByActiveFalse()).thenReturn(3L);
        when(assessmentRepository.countByAssessmentType("GENERALIZED_POST_TEST")).thenReturn(2L);
        when(assessmentRepository.countByCompetency_Code("COC1")).thenReturn(1L);
        when(assessmentRepository.countByDiscussion_Competency_Code("COC1")).thenReturn(2L);
        when(assessmentRepository.countByCompetency_Code("COC2")).thenReturn(1L);
        when(assessmentRepository.countByDiscussion_Competency_Code("COC2")).thenReturn(4L);
        when(learningContentRepository.count()).thenReturn(63L);
        when(learningContentRepository.countByActiveTrue()).thenReturn(54L);
        when(learningContentRepository.countByActiveFalse()).thenReturn(9L);
        when(learningContentRepository.countByDiscussion_Competency_Code("COC1")).thenReturn(42L);
        when(learningContentRepository.countByDiscussion_Competency_Code("COC2")).thenReturn(21L);

        var summary = service.getDashboardSummary();

        assertEquals(new StudentStatistics(5, 1, 2, 1, 1), summary.students());
        assertEquals(new AssessmentStatistics(8, 5, 3, 3, 5, 6, 2), summary.assessments());
        assertEquals(new LearningContentStatistics(63, 54, 9, 42, 21), summary.learningContent());
    }

    @Test
    void pageSummaryCountsOnlyCompletedStagesAndDiscussions() {
        Student student = student(7L);
        var pageRequest = PageRequest.of(0, 20);
        when(studentRepository.findForAdminProgress(null, null, null, pageRequest))
            .thenReturn(new PageImpl<>(List.of(student), pageRequest, 1));
        Competency coc1 = competency(1L, "COC1");
        Competency coc2 = competency(2L, "COC2");
        when(competencyRepository.findByCodeInOrderByDisplayOrderAsc(List.of("COC1", "COC2")))
            .thenReturn(List.of(coc1, coc2));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(discussion(11L, coc1)));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(2L)).thenReturn(List.of(discussion(21L, coc2)));
        var coc1Count = mock(StudentStageProgressRepository.DiscussionStageCountView.class);
        when(coc1Count.getStudentId()).thenReturn(7L);
        when(coc1Count.getCompetencyCode()).thenReturn("COC1");
        when(coc1Count.getDiscussionId()).thenReturn(11L);
        when(coc1Count.getCompletedStages()).thenReturn(7L);
        when(coc1Count.getTotalStageRows()).thenReturn(7L);
        var coc2Count = mock(StudentStageProgressRepository.DiscussionStageCountView.class);
        when(coc2Count.getStudentId()).thenReturn(7L);
        when(coc2Count.getCompetencyCode()).thenReturn("COC2");
        when(coc2Count.getDiscussionId()).thenReturn(21L);
        when(coc2Count.getCompletedStages()).thenReturn(2L);
        when(coc2Count.getTotalStageRows()).thenReturn(2L);
        when(stageProgressRepository.findDiscussionCompletionCounts(Set.of(7L))).thenReturn(List.of(coc1Count, coc2Count));

        var result = service.getStudentProgressPage("", "", "", 0, 20);

        assertEquals(1, result.content().size());
        assertEquals(7, result.content().getFirst().coc1().completedStages());
        assertEquals(1, result.content().getFirst().coc1().completedDiscussions());
        assertEquals(2, result.content().getFirst().coc2().completedStages());
        assertEquals(0, result.content().getFirst().coc2().completedDiscussions());
        assertEquals(100, result.content().getFirst().coc1().percentage());
    }

    @Test
    void detailDoesNotCountInProgressAttemptsAsSubmittedAndKeepsPassedCompetency() {
        Student student = student(7L);
        Competency coc1 = competency(1L, "COC1");
        Competency coc2 = competency(2L, "COC2");
        Discussion discussion1 = discussion(11L, coc1);
        Discussion discussion2 = discussion(21L, coc2);
        when(studentRepository.findById(7L)).thenReturn(Optional.of(student));
        when(competencyRepository.findByCodeInOrderByDisplayOrderAsc(List.of("COC1", "COC2")))
            .thenReturn(List.of(coc1, coc2));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(discussion1));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(2L)).thenReturn(List.of(discussion2));
        when(competencyProgressRepository.findByStudent_IdOrderByCompetency_DisplayOrderAsc(7L))
            .thenReturn(List.of(competencyProgress(student, coc1, "IN_PROGRESS"), competencyProgress(student, coc2, "PASSED")));
        when(stageProgressRepository.findForAdminStudentAndDiscussions(7L, List.of(11L, 21L)))
            .thenReturn(List.of(stage(student, discussion1, "PRE_TEST", "COMPLETED"),
                stage(student, discussion1, "DEFINITIONS", "IN_PROGRESS")));

        var preTest = assessmentView(51L, "COC1", 11L, "PRE_TEST", true);
        var generalizedCoc1 = assessmentView(52L, "COC1", null, "GENERALIZED_POST_TEST", true);
        var generalizedCoc2 = assessmentView(53L, "COC2", null, "GENERALIZED_POST_TEST", false);
        when(assessmentRepository.findProgressAssessmentsByCompetencyCodes(List.of("COC1", "COC2")))
            .thenReturn(List.of(preTest, generalizedCoc1, generalizedCoc2));
        var submittedPreTest = attemptView(61L, 51L, 1, "SUBMITTED",
            new BigDecimal("1.00"), new BigDecimal("50.00"), false);
        var inProgressPreTest = attemptView(62L, 51L, 2, "IN_PROGRESS", null, null, null);
        var submittedGeneralized = attemptView(63L, 52L, 1, "SUBMITTED",
            new BigDecimal("60.00"), new BigDecimal("60.00"), false);
        when(attemptRepository.findForAdminStudentAndAssessments(7L, List.of(51L, 52L, 53L)))
            .thenReturn(List.of(submittedPreTest, inProgressPreTest, submittedGeneralized));
        when(retakeAuthorizationRepository.countByStudent_IdAndCompetency_Id(7L, 1L)).thenReturn(0L);
        when(retakeAuthorizationRepository.countByStudent_IdAndCompetency_Id(7L, 2L)).thenReturn(0L);

        var detail = service.getStudentProgress(7L);
        var coc1Detail = detail.competencies().getFirst();
        var preTestDetail = coc1Detail.discussions().getFirst().assessments().getFirst();
        var generalizedCoc2Detail = detail.competencies().get(1);

        assertEquals(1, coc1Detail.completedStages());
        assertFalse(coc1Detail.discussions().getFirst().completed());
        assertEquals(1, preTestDetail.submittedAttempts());
        assertEquals("IN_PROGRESS", preTestDetail.attempts().get(1).status());
        assertEquals(1, coc1Detail.generalizedAttemptsUsed());
        assertEquals(2, coc1Detail.generalizedAttemptsRemaining());
        assertTrue(generalizedCoc2Detail.generalizedPostTestPassed());
        assertEquals("PASSED", generalizedCoc2Detail.status());
    }

    private Student student(Long id) {
        Student student = new Student(); student.setId(id); student.setAccountId("STU-000007");
        student.setFullName("Test Student"); student.setProgram("BSCS"); student.setStatus(StudentStatus.ACTIVE);
        return student;
    }

    private Competency competency(Long id, String code) {
        Competency competency = new Competency(); competency.setId(id); competency.setCode(code);
        competency.setTitle(code.equals("COC1") ? "Computer Systems Servicing NC II COC 1" : "Computer Systems Servicing NC II COC 2");
        competency.setActive(true); return competency;
    }

    private Discussion discussion(Long id, Competency competency) {
        Discussion discussion = new Discussion(); discussion.setId(id); discussion.setCompetency(competency);
        discussion.setTitle("Discussion " + id); discussion.setDisplayOrder(1); discussion.setActive(true);
        return discussion;
    }

    private StudentStageProgress stage(Student student, Discussion discussion, String code, String status) {
        StudentStageProgress stage = new StudentStageProgress(); stage.setStudent(student);
        stage.setDiscussion(discussion); stage.setStageCode(code); stage.setStatus(status);
        if ("COMPLETED".equals(status)) stage.setCompletedAt(LocalDateTime.now());
        return stage;
    }

    private StudentCompetencyProgress competencyProgress(Student student, Competency competency, String status) {
        StudentCompetencyProgress progress = new StudentCompetencyProgress(); progress.setStudent(student);
        progress.setCompetency(competency); progress.setStatus(status); return progress;
    }

    private AssessmentRepository.AdminProgressAssessmentView assessmentView(
        Long id, String code, Long discussionId, String type, boolean active
    ) {
        var assessment = mock(AssessmentRepository.AdminProgressAssessmentView.class);
        when(assessment.getId()).thenReturn(id); when(assessment.getAssessmentType()).thenReturn(type);
        when(assessment.getTitle()).thenReturn(type); when(assessment.getActive()).thenReturn(active);
        when(assessment.getDiscussionId()).thenReturn(discussionId); when(assessment.getCompetencyCode()).thenReturn(code);
        return assessment;
    }

    private AssessmentAttemptRepository.AdminProgressAttemptView attemptView(
        Long id, Long assessmentId, int number, String status, BigDecimal score,
        BigDecimal percentage, Boolean passed
    ) {
        var attempt = mock(AssessmentAttemptRepository.AdminProgressAttemptView.class);
        when(attempt.getId()).thenReturn(id); when(attempt.getAssessmentId()).thenReturn(assessmentId);
        when(attempt.getAttemptNumber()).thenReturn(number); when(attempt.getStatus()).thenReturn(status);
        when(attempt.getPercentage()).thenReturn(percentage); when(attempt.getScore()).thenReturn(score);
        when(attempt.getTotalPossibleScore()).thenReturn(score); when(attempt.getPassed()).thenReturn(passed);
        return attempt;
    }
}
