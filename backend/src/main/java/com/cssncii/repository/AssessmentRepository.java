package com.cssncii.repository;

import com.cssncii.entity.Assessment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Optional<Assessment> findByDiscussion_IdAndAssessmentType(Long discussionId, String assessmentType);

    Optional<Assessment> findByCompetency_IdAndAssessmentType(Long competencyId, String assessmentType);

    List<Assessment> findByDiscussion_IdOrderByAssessmentTypeAsc(Long discussionId);

    List<Assessment> findByCompetency_IdOrderByAssessmentTypeAsc(Long competencyId);

    long countByActiveTrue();

    long countByActiveFalse();

    long countByCompetency_Code(String code);

    long countByDiscussion_Competency_Code(String code);

    long countByAssessmentType(String assessmentType);

    @Query("select assessment.id as id, assessment.assessmentType as assessmentType, "
        + "assessment.title as title, assessment.active as active, discussion.id as discussionId, "
        + "discussion.title as discussionTitle, "
        + "coalesce(assessmentCompetency.id, discussionCompetency.id) as competencyId, "
        + "coalesce(assessmentCompetency.code, discussionCompetency.code) as competencyCode "
        + "from Assessment assessment left join assessment.discussion discussion "
        + "left join discussion.competency discussionCompetency "
        + "left join assessment.competency assessmentCompetency "
        + "where coalesce(assessmentCompetency.code, discussionCompetency.code) in :codes")
    List<AdminProgressAssessmentView> findProgressAssessmentsByCompetencyCodes(@Param("codes") Collection<String> codes);

    interface AdminProgressAssessmentView {
        Long getId();
        String getAssessmentType();
        String getTitle();
        boolean getActive();
        Long getDiscussionId();
        String getDiscussionTitle();
        Long getCompetencyId();
        String getCompetencyCode();
    }
}
