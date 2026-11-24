package com.cssncii.repository;

import com.cssncii.entity.StudentCompetencyProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCompetencyProgressRepository extends JpaRepository<StudentCompetencyProgress, Long> {
    Optional<StudentCompetencyProgress> findByStudent_IdAndCompetency_Id(Long studentId, Long competencyId);

    List<StudentCompetencyProgress> findByStudent_IdOrderByCompetency_DisplayOrderAsc(Long studentId);
}
