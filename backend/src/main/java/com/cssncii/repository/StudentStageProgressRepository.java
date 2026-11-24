package com.cssncii.repository;

import com.cssncii.entity.StudentStageProgress;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentStageProgressRepository extends JpaRepository<StudentStageProgress, Long> {
    List<StudentStageProgress> findByStudent_IdAndDiscussion_Id(Long studentId, Long discussionId);

    @Query("select progress.student.id as studentId, competency.code as competencyCode, "
        + "discussion.id as discussionId, "
        + "sum(case when progress.status = 'COMPLETED' then 1 else 0 end) as completedStages, "
        + "count(progress.id) as totalStageRows "
        + "from StudentStageProgress progress join progress.discussion discussion "
        + "join discussion.competency competency "
        + "where progress.student.id in :studentIds and competency.code in ('COC1', 'COC2') "
        + "and progress.stageCode in ('PRE_TEST', 'DEFINITIONS', 'EXPLANATION', 'EXAMPLES', 'VIDEO_LESSONS', 'ACTIVITY', 'POST_TEST') "
        + "group by progress.student.id, competency.code, discussion.id")
    List<DiscussionStageCountView> findDiscussionCompletionCounts(@Param("studentIds") Collection<Long> studentIds);

    @Query("select progress from StudentStageProgress progress "
        + "join fetch progress.discussion discussion join fetch discussion.competency "
        + "where progress.student.id = :studentId and discussion.id in :discussionIds")
    List<StudentStageProgress> findForAdminStudentAndDiscussions(
        @Param("studentId") Long studentId, @Param("discussionIds") Collection<Long> discussionIds);

    interface DiscussionStageCountView {
        Long getStudentId();
        String getCompetencyCode();
        Long getDiscussionId();
        Long getCompletedStages();
        Long getTotalStageRows();
    }
}
