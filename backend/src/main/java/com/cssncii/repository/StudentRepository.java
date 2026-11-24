package com.cssncii.repository;

import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByAccountId(String accountId);

    List<Student> findByStatusOrderByFullNameAscIdAsc(StudentStatus status);

    boolean existsByAccountId(String accountId);

    long countByStatus(StudentStatus status);

    @Query(value = "select student from Student student "
        + "where (:search is null or lower(student.fullName) like lower(concat('%', concat(:search, '%'))) "
        + "or lower(student.accountId) like lower(concat('%', concat(:search, '%')))) "
        + "and (:program is null or lower(student.program) = lower(:program)) "
        + "and (:status is null or student.status = :status) "
        + "order by student.fullName asc, student.id asc",
        countQuery = "select count(student) from Student student "
            + "where (:search is null or lower(student.fullName) like lower(concat('%', concat(:search, '%'))) "
            + "or lower(student.accountId) like lower(concat('%', concat(:search, '%')))) "
            + "and (:program is null or lower(student.program) = lower(:program)) "
            + "and (:status is null or student.status = :status)")
    Page<Student> findForAdminProgress(
        @Param("search") String search,
        @Param("program") String program,
        @Param("status") StudentStatus status,
        Pageable pageable
    );

    @Query("select distinct student.program from Student student order by student.program asc")
    List<String> findDistinctPrograms();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select student from Student student where student.id = :studentId")
    Optional<Student> findByIdForUpdate(@Param("studentId") Long studentId);
}
