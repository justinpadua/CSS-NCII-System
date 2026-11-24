package com.cssncii.repository;

import com.cssncii.entity.RetakeAuthorization;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetakeAuthorizationRepository extends JpaRepository<RetakeAuthorization, Long> {
    long countByStudent_IdAndCompetency_Id(Long studentId, Long competencyId);

    List<RetakeAuthorization> findByStudent_IdAndCompetency_IdOrderByAuthorizedAtAsc(
        Long studentId, Long competencyId);
}
