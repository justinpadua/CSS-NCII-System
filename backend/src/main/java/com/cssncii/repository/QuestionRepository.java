package com.cssncii.repository;

import com.cssncii.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByAssessment_IdOrderByDisplayOrderAsc(Long assessmentId);
}
