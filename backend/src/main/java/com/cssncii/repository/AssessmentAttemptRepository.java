package com.cssncii.repository;

import com.cssncii.entity.AssessmentAttempt;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {
    boolean existsByAssessment_Id(Long assessmentId);

    boolean existsByAssessment_IdAndStatusNot(Long assessmentId, String status);

    Optional<AssessmentAttempt> findByStudent_IdAndAssessment_Id(Long studentId, Long assessmentId);

    Optional<AssessmentAttempt> findByStudent_IdAndAssessment_IdAndStatus(
        Long studentId, Long assessmentId, String status);

    List<AssessmentAttempt> findAllByStudent_IdAndAssessment_IdAndStatusOrderByAttemptNumberAsc(
        Long studentId, Long assessmentId, String status);

    List<AssessmentAttempt> findByStudent_IdAndAssessment_IdOrderByAttemptNumberAsc(
        Long studentId, Long assessmentId);

    long countByStudent_IdAndAssessment_IdAndStatus(Long studentId, Long assessmentId, String status);

    @Query("select attempt.id as id, attempt.assessment.id as assessmentId, "
        + "attempt.attemptNumber as attemptNumber, attempt.status as status, "
        + "attempt.score as score, attempt.totalPossibleScore as totalPossibleScore, "
        + "attempt.percentage as percentage, attempt.passed as passed, attempt.submittedAt as submittedAt "
        + "from AssessmentAttempt attempt where attempt.student.id = :studentId "
        + "and attempt.assessment.id in :assessmentIds order by attempt.assessment.id, attempt.attemptNumber")
    List<AdminProgressAttemptView> findForAdminStudentAndAssessments(
        @Param("studentId") Long studentId, @Param("assessmentIds") Collection<Long> assessmentIds);

    interface AdminProgressAttemptView {
        Long getId();
        Long getAssessmentId();
        int getAttemptNumber();
        String getStatus();
        java.math.BigDecimal getScore();
        java.math.BigDecimal getTotalPossibleScore();
        java.math.BigDecimal getPercentage();
        Boolean getPassed();
        java.time.LocalDateTime getSubmittedAt();
    }
}
