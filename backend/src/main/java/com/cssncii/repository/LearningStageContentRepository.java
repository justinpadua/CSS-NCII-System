package com.cssncii.repository;

import com.cssncii.entity.LearningStageContent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningStageContentRepository extends JpaRepository<LearningStageContent, Long> {
    List<LearningStageContent> findByDiscussion_IdOrderByDisplayOrderAsc(Long discussionId);

    long countByActiveTrue();

    long countByActiveFalse();

    long countByDiscussion_Competency_Code(String code);
}
