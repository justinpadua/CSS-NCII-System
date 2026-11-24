package com.cssncii.dto;

import com.cssncii.entity.StudentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminReportingDtos {
    private AdminReportingDtos() {}

    public record DashboardSummary(
        StudentStatistics students,
        AssessmentStatistics assessments,
        LearningContentStatistics learningContent
    ) {}

    public record StudentStatistics(long total, long active, long pending, long rejected, long deactivated) {}

    public record AssessmentStatistics(
        long total, long active, long draft, long coc1, long coc2, long discussion, long generalizedPostTests
    ) {}

    public record LearningContentStatistics(
        long total, long active, long draft, long coc1, long coc2
    ) {}

    public record ProgressCount(int completedDiscussions, int totalDiscussions, int completedStages, int totalStages) {
        public int percentage() {
            return totalStages == 0 ? 0 : Math.round(completedStages * 100f / totalStages);
        }
    }

    public record StudentProgressRow(
        Long id, String accountId, String fullName, String program, StudentStatus status,
        ProgressCount coc1, ProgressCount coc2
    ) {}

    public record StudentProgressPage(
        List<StudentProgressRow> content, int page, int size, long totalElements, int totalPages
    ) {}

    public record StudentIdentity(Long id, String accountId, String fullName, String program, StudentStatus status) {}

    public record StudentProgressDetail(StudentIdentity student, List<CompetencyProgressDetail> competencies) {}

    public record CompetencyProgressDetail(
        Long id, String code, String title, String status,
        int completedDiscussions, int totalDiscussions, int completedStages, int totalStages,
        boolean generalizedPostTestPassed, int generalizedAttemptsUsed,
        int generalizedAttemptsAllowed, int generalizedAttemptsRemaining,
        AssessmentProgressDetail generalizedPostTest, List<DiscussionProgressDetail> discussions
    ) {}

    public record DiscussionProgressDetail(
        Long id, String title, int displayOrder, boolean active, boolean completed,
        int completedStages, List<StageProgressDetail> stages, List<AssessmentProgressDetail> assessments
    ) {}

    public record StageProgressDetail(String stageCode, String status, LocalDateTime completedAt) {}

    public record AssessmentProgressDetail(
        Long id, String assessmentType, String title, boolean configured, boolean active,
        int submittedAttempts, List<AssessmentAttemptDetail> attempts
    ) {}

    public record AssessmentAttemptDetail(
        Long id, int attemptNumber, String status, BigDecimal score, BigDecimal totalPossibleScore,
        BigDecimal percentage, Boolean passed, LocalDateTime submittedAt
    ) {}
}
