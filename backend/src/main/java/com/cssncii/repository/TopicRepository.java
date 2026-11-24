package com.cssncii.repository;

import com.cssncii.entity.Topic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByDiscussion_IdOrderByDisplayOrderAsc(Long discussionId);
}
