package com.cssncii.repository;

import com.cssncii.entity.Discussion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {
    List<Discussion> findByCompetency_IdOrderByDisplayOrderAsc(Long competencyId);

    @Query("select discussion.competency.id from Discussion discussion "
        + "where discussion.id = :discussionId and discussion.active = true "
        + "and discussion.competency.active = true")
    Optional<Long> findActiveCompetencyIdByDiscussionId(@Param("discussionId") Long discussionId);
}
