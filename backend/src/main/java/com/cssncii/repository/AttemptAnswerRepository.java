package com.cssncii.repository;

import com.cssncii.entity.AttemptAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {
    List<AttemptAnswer> findByAttempt_Id(Long attemptId);

    java.util.Optional<AttemptAnswer> findByAttempt_IdAndQuestion_Id(Long attemptId, Long questionId);

    boolean existsByQuestion_Id(Long questionId);
}
