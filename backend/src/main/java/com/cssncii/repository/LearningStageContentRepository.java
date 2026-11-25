package com.cssncii.repository;

import com.cssncii.entity.LearningStageContent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningStageContentRepository extends JpaRepository<LearningStageContent, Long> {
    List<LearningStageContent> findByDiscussion_IdOrderByDisplayOrderAsc(Long discussionId);

    @Query("select content from LearningStageContent content "
        + "join fetch content.discussion discussion join fetch discussion.competency "
        + "where content.active = true and locate(:reference, content.contentBody) > 0")
    List<LearningStageContent> findActiveContentReferencingImage(@Param("reference") String reference);

    long countByActiveTrue();

    long countByActiveFalse();

    long countByDiscussion_Competency_Code(String code);
}
