package com.cssncii.service;

import com.cssncii.dto.AdminReportingDtos.AssessmentAttemptDetail;
import com.cssncii.dto.AdminReportingDtos.AssessmentProgressDetail;
import com.cssncii.dto.AdminReportingDtos.AssessmentStatistics;
import com.cssncii.dto.AdminReportingDtos.CompetencyProgressDetail;
import com.cssncii.dto.AdminReportingDtos.DashboardSummary;
import com.cssncii.dto.AdminReportingDtos.DiscussionProgressDetail;
import com.cssncii.dto.AdminReportingDtos.LearningContentStatistics;
import com.cssncii.dto.AdminReportingDtos.ProgressCount;
import com.cssncii.dto.AdminReportingDtos.StageProgressDetail;
import com.cssncii.dto.AdminReportingDtos.StudentIdentity;
import com.cssncii.dto.AdminReportingDtos.StudentProgressDetail;
import com.cssncii.dto.AdminReportingDtos.StudentProgressPage;
import com.cssncii.dto.AdminReportingDtos.StudentProgressRow;
import com.cssncii.dto.AdminReportingDtos.StudentStatistics;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.AssessmentAttempt;
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
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminReportingService {
    private static final List<String> COC_CODES = List.of("COC1", "COC2");
    private static final List<String> STAGE_CODES = List.of(
        "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"
    );
    private static final List<String> DISCUSSION_ASSESSMENT_TYPES = List.of(
        "PRE_TEST", "ACTIVITY", "DISCUSSION_POST_TEST"
    );
    private static final String GENERALIZED_POST_TEST = "GENERALIZED_POST_TEST";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String COMPLETED = "COMPLETED";

    private final StudentRepository studentRepository;
    private final AssessmentRepository assessmentRepository;
    private final LearningStageContentRepository learningContentRepository;
    private final CompetencyRepository competencyRepository;
    private final DiscussionRepository discussionRepository;
    private final StudentStageProgressRepository stageProgressRepository;
    private final StudentCompetencyProgressRepository competencyProgressRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final RetakeAuthorizationRepository retakeAuthorizationRepository;

    public AdminReportingService(
        StudentRepository studentRepository,
        AssessmentRepository assessmentRepository,
        LearningStageContentRepository learningContentRepository,
        CompetencyRepository competencyRepository,
        DiscussionRepository discussionRepository,
        StudentStageProgressRepository stageProgressRepository,
        StudentCompetencyProgressRepository competencyProgressRepository,
        AssessmentAttemptRepository attemptRepository,
        RetakeAuthorizationRepository retakeAuthorizationRepository
    ) {
        this.studentRepository = studentRepository;
        this.assessmentRepository = assessmentRepository;
        this.learningContentRepository = learningContentRepository;
        this.competencyRepository = competencyRepository;
        this.discussionRepository = discussionRepository;
        this.stageProgressRepository = stageProgressRepository;
        this.competencyProgressRepository = competencyProgressRepository;
        this.attemptRepository = attemptRepository;
        this.retakeAuthorizationRepository = retakeAuthorizationRepository;
    }

    public DashboardSummary getDashboardSummary() {
        long students = studentRepository.count();
        StudentStatistics studentStats = new StudentStatistics(
            students,
            studentRepository.countByStatus(StudentStatus.ACTIVE),
            studentRepository.countByStatus(StudentStatus.PENDING),
            studentRepository.countByStatus(StudentStatus.REJECTED),
            studentRepository.countByStatus(StudentStatus.DEACTIVATED));

        long totalAssessments = assessmentRepository.count();
        long generalized = assessmentRepository.countByAssessmentType(GENERALIZED_POST_TEST);
        long discussionAssessments = totalAssessments - generalized;
        long coc1Assessments = assessmentRepository.countByCompetency_Code("COC1")
            + assessmentRepository.countByDiscussion_Competency_Code("COC1");
        long coc2Assessments = assessmentRepository.countByCompetency_Code("COC2")
            + assessmentRepository.countByDiscussion_Competency_Code("COC2");
        AssessmentStatistics assessmentStats = new AssessmentStatistics(
            totalAssessments, assessmentRepository.countByActiveTrue(), assessmentRepository.countByActiveFalse(),
            coc1Assessments, coc2Assessments, discussionAssessments, generalized);

        long totalStages = learningContentRepository.count();
        LearningContentStatistics learningStats = new LearningContentStatistics(
            totalStages, learningContentRepository.countByActiveTrue(), learningContentRepository.countByActiveFalse(),
            learningContentRepository.countByDiscussion_Competency_Code("COC1"),
            learningContentRepository.countByDiscussion_Competency_Code("COC2"));

        return new DashboardSummary(studentStats, assessmentStats, learningStats);
    }

    public List<String> getPrograms() {
        return studentRepository.findDistinctPrograms();
    }

    public StudentProgressPage getStudentProgressPage(
        String search, String program, String status, int page, int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be 0 or greater and size must be between 1 and 100.");
        }
        StudentStatus selectedStatus = parseStatus(status);
        Page<Student> result = studentRepository.findForAdminProgress(
            normalizeFilter(search), normalizeFilter(program), selectedStatus, PageRequest.of(page, size));
        List<Student> students = result.getContent();
        if (students.isEmpty()) {
            return new StudentProgressPage(List.of(), page, size, result.getTotalElements(), result.getTotalPages());
        }

        Map<String, Integer> discussionTotals = getDiscussionTotals();
        Set<Long> studentIds = students.stream().map(Student::getId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Map<String, ProgressAccumulator>> completedByStudent = new HashMap<>();
        for (StudentStageProgressRepository.DiscussionStageCountView row
            : stageProgressRepository.findDiscussionCompletionCounts(studentIds)) {
            long completedStages = row.getCompletedStages() == null ? 0 : row.getCompletedStages();
            ProgressAccumulator accumulator = completedByStudent
                .computeIfAbsent(row.getStudentId(), ignored -> new HashMap<>())
                .computeIfAbsent(row.getCompetencyCode(), ignored -> new ProgressAccumulator());
            accumulator.completedStages += Math.toIntExact(completedStages);
            if (completedStages == STAGE_CODES.size()
                && row.getTotalStageRows() != null && row.getTotalStageRows() == STAGE_CODES.size()) {
                accumulator.completedDiscussions++;
            }
        }

        List<StudentProgressRow> rows = students.stream().map(student -> new StudentProgressRow(
            student.getId(), student.getAccountId(), student.getFullName(), student.getProgram(), student.getStatus(),
            toProgressCount(completedByStudent, student.getId(), "COC1", discussionTotals.getOrDefault("COC1", 0)),
            toProgressCount(completedByStudent, student.getId(), "COC2", discussionTotals.getOrDefault("COC2", 0))))
            .toList();
        return new StudentProgressPage(rows, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public StudentProgressDetail getStudentProgress(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student record not found."));
        List<Competency> competencies = competencyRepository.findByCodeInOrderByDisplayOrderAsc(COC_CODES);
        Map<Long, StudentCompetencyProgress> progressByCompetency = new HashMap<>();
        for (StudentCompetencyProgress progress : competencyProgressRepository
            .findByStudent_IdOrderByCompetency_DisplayOrderAsc(studentId)) {
            progressByCompetency.put(progress.getCompetency().getId(), progress);
        }

        Map<Long, List<Discussion>> discussionsByCompetency = new HashMap<>();
        List<Discussion> allDiscussions = new ArrayList<>();
        for (Competency competency : competencies) {
            List<Discussion> discussions = discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(competency.getId());
            discussionsByCompetency.put(competency.getId(), discussions);
            allDiscussions.addAll(discussions);
        }
        List<Long> discussionIds = allDiscussions.stream().map(Discussion::getId).toList();
        Map<Long, Map<String, StudentStageProgress>> stagesByDiscussion = new HashMap<>();
        if (!discussionIds.isEmpty()) {
            for (StudentStageProgress stage : stageProgressRepository
                .findForAdminStudentAndDiscussions(studentId, discussionIds)) {
                stagesByDiscussion.computeIfAbsent(stage.getDiscussion().getId(), ignored -> new HashMap<>())
                    .put(stage.getStageCode(), stage);
            }
        }

        List<AssessmentRepository.AdminProgressAssessmentView> assessments = assessmentRepository
            .findProgressAssessmentsByCompetencyCodes(COC_CODES);
        Map<Long, AssessmentRepository.AdminProgressAssessmentView> assessmentById = new HashMap<>();
        assessments.forEach(assessment -> assessmentById.put(assessment.getId(), assessment));
        List<Long> assessmentIds = assessments.stream().map(AssessmentRepository.AdminProgressAssessmentView::getId).toList();
        Map<Long, List<AssessmentAttemptRepository.AdminProgressAttemptView>> attemptsByAssessment = new HashMap<>();
        if (!assessmentIds.isEmpty()) {
            for (AssessmentAttemptRepository.AdminProgressAttemptView attempt
                : attemptRepository.findForAdminStudentAndAssessments(studentId, assessmentIds)) {
                attemptsByAssessment.computeIfAbsent(attempt.getAssessmentId(), ignored -> new ArrayList<>()).add(attempt);
            }
        }

        List<CompetencyProgressDetail> competencyDetails = new ArrayList<>();
        for (Competency competency : competencies) {
            List<Discussion> discussions = discussionsByCompetency.getOrDefault(competency.getId(), List.of());
            List<DiscussionProgressDetail> discussionDetails = discussions.stream()
                .map(discussion -> toDiscussionDetail(discussion, stagesByDiscussion.getOrDefault(discussion.getId(), Map.of()),
                    assessments, attemptsByAssessment))
                .toList();
            int completedStages = discussionDetails.stream().mapToInt(DiscussionProgressDetail::completedStages).sum();
            int completedDiscussions = (int) discussionDetails.stream().filter(DiscussionProgressDetail::completed).count();
            int totalStages = discussions.size() * STAGE_CODES.size();

            StudentCompetencyProgress savedProgress = progressByCompetency.get(competency.getId());
            String savedStatus = savedProgress == null ? "NOT_INITIALIZED" : savedProgress.getStatus();
            AssessmentProgressDetail generalized = toAssessmentDetail(
                competency.getCode(), GENERALIZED_POST_TEST, assessments, attemptsByAssessment);
            int submittedAttempts = generalized.submittedAttempts();
            boolean passed = "PASSED".equals(savedStatus) || generalized.attempts().stream()
                .anyMatch(attempt -> SUBMITTED.equals(attempt.status()) && Boolean.TRUE.equals(attempt.passed()));
            int allowedAttempts = 3 + 3 * Math.toIntExact(
                retakeAuthorizationRepository.countByStudent_IdAndCompetency_Id(studentId, competency.getId()));
            int remainingAttempts = passed ? 0 : Math.max(0, allowedAttempts - submittedAttempts);
            String competencyStatus = passed ? "PASSED" : savedStatus;

            competencyDetails.add(new CompetencyProgressDetail(
                competency.getId(), competency.getCode(), competency.getTitle(), competencyStatus,
                completedDiscussions, discussions.size(), completedStages, totalStages,
                passed, submittedAttempts, allowedAttempts, remainingAttempts,
                generalized, discussionDetails));
        }
        return new StudentProgressDetail(
            new StudentIdentity(student.getId(), student.getAccountId(), student.getFullName(), student.getProgram(), student.getStatus()),
            competencyDetails);
    }

    private DiscussionProgressDetail toDiscussionDetail(
        Discussion discussion,
        Map<String, StudentStageProgress> savedStages,
        List<AssessmentRepository.AdminProgressAssessmentView> assessments,
        Map<Long, List<AssessmentAttemptRepository.AdminProgressAttemptView>> attemptsByAssessment
    ) {
        List<StageProgressDetail> stages = STAGE_CODES.stream().map(stageCode -> {
            StudentStageProgress saved = savedStages.get(stageCode);
            return new StageProgressDetail(stageCode, saved == null ? "NOT_STARTED" : saved.getStatus(),
                saved == null ? null : saved.getCompletedAt());
        }).toList();
        int completedStages = (int) stages.stream().filter(stage -> COMPLETED.equals(stage.status())).count();
        boolean discussionCompleted = savedStages.size() == STAGE_CODES.size()
            && stages.size() == STAGE_CODES.size() && completedStages == STAGE_CODES.size();
        List<AssessmentProgressDetail> discussionAssessments = DISCUSSION_ASSESSMENT_TYPES.stream()
            .map(type -> toDiscussionAssessmentDetail(discussion.getId(), type, assessments, attemptsByAssessment))
            .toList();
        return new DiscussionProgressDetail(
            discussion.getId(), discussion.getTitle(), Math.toIntExact(discussion.getDisplayOrder()), discussion.isActive(),
            discussionCompleted, completedStages, stages, discussionAssessments);
    }

    private AssessmentProgressDetail toDiscussionAssessmentDetail(
        Long discussionId, String type, List<AssessmentRepository.AdminProgressAssessmentView> assessments,
        Map<Long, List<AssessmentAttemptRepository.AdminProgressAttemptView>> attemptsByAssessment
    ) {
        AssessmentRepository.AdminProgressAssessmentView assessment = assessments.stream()
            .filter(item -> discussionId.equals(item.getDiscussionId()) && type.equals(item.getAssessmentType()))
            .findFirst().orElse(null);
        return toAssessmentDetail(assessment, type, attemptsByAssessment);
    }

    private AssessmentProgressDetail toAssessmentDetail(
        String competencyCode, String type, List<AssessmentRepository.AdminProgressAssessmentView> assessments,
        Map<Long, List<AssessmentAttemptRepository.AdminProgressAttemptView>> attemptsByAssessment
    ) {
        AssessmentRepository.AdminProgressAssessmentView assessment = assessments.stream()
            .filter(item -> competencyCode.equals(item.getCompetencyCode()) && type.equals(item.getAssessmentType())
                && item.getDiscussionId() == null)
            .findFirst().orElse(null);
        return toAssessmentDetail(assessment, type, attemptsByAssessment);
    }

    private AssessmentProgressDetail toAssessmentDetail(
        AssessmentRepository.AdminProgressAssessmentView assessment, String type,
        Map<Long, List<AssessmentAttemptRepository.AdminProgressAttemptView>> attemptsByAssessment
    ) {
        if (assessment == null) return new AssessmentProgressDetail(null, type, null, false, false, 0, List.of());
        List<AssessmentAttemptDetail> attempts = attemptsByAssessment
            .getOrDefault(assessment.getId(), List.of()).stream()
            .map(attempt -> new AssessmentAttemptDetail(attempt.getId(), attempt.getAttemptNumber(), attempt.getStatus(),
                attempt.getScore(), attempt.getTotalPossibleScore(), attempt.getPercentage(), attempt.getPassed(), attempt.getSubmittedAt()))
            .sorted(Comparator.comparingInt(AssessmentAttemptDetail::attemptNumber))
            .toList();
        int submittedAttempts = (int) attempts.stream().filter(attempt -> SUBMITTED.equals(attempt.status())).count();
        return new AssessmentProgressDetail(assessment.getId(), assessment.getAssessmentType(), assessment.getTitle(),
            true, assessment.getActive(), submittedAttempts, attempts);
    }

    private Map<String, Integer> getDiscussionTotals() {
        Map<String, Integer> totals = new HashMap<>();
        for (Competency competency : competencyRepository.findByCodeInOrderByDisplayOrderAsc(COC_CODES)) {
            totals.put(competency.getCode(), discussionRepository
                .findByCompetency_IdOrderByDisplayOrderAsc(competency.getId()).size());
        }
        return totals;
    }

    private ProgressCount toProgressCount(
        Map<Long, Map<String, ProgressAccumulator>> completedByStudent,
        Long studentId, String competencyCode, int totalDiscussions
    ) {
        ProgressAccumulator progress = completedByStudent.getOrDefault(studentId, Map.of())
            .getOrDefault(competencyCode, new ProgressAccumulator());
        return new ProgressCount(progress.completedDiscussions, totalDiscussions,
            progress.completedStages, totalDiscussions * STAGE_CODES.size());
    }

    private StudentStatus parseStatus(String status) {
        String normalized = normalizeFilter(status);
        if (normalized == null) return null;
        try {
            return StudentStatus.valueOf(normalized.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Status must be ACTIVE, PENDING, REJECTED, or DEACTIVATED.");
        }
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static final class ProgressAccumulator {
        private int completedDiscussions;
        private int completedStages;
    }
}
