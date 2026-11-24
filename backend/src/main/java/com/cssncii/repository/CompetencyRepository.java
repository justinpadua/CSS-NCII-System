package com.cssncii.repository;

import com.cssncii.entity.Competency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetencyRepository extends JpaRepository<Competency, Long> {
    Optional<Competency> findByCode(String code);

    List<Competency> findByCodeInOrderByDisplayOrderAsc(List<String> codes);
}
