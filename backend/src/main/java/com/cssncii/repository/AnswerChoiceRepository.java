package com.cssncii.repository;

import com.cssncii.entity.AnswerChoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerChoiceRepository extends JpaRepository<AnswerChoice, Long> {
    List<AnswerChoice> findByQuestion_IdOrderByDisplayOrderAsc(Long questionId);
}
