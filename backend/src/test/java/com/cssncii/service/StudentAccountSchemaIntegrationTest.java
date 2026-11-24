package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.DiscussionRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.datasource.url=jdbc:mysql://localhost:3306/css_ncii_java_db_migration_test")
class StudentAccountSchemaIntegrationTest {
    @Autowired private StudentRepository studentRepository;
    @Autowired private DiscussionRepository discussionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void writesAccountToTheExplicitMigrationTestDatabaseAndRollsBackWithTestTransaction() {
        assertEquals("css_ncii_java_db_migration_test", jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));
        String accountId = "STU-TEST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Student student = new Student();
        student.setAccountId(accountId);
        student.setFullName("Migration Test Student");
        student.setProgram("TEST");
        student.setPasswordHash(null);
        student.setStatus(StudentStatus.DEACTIVATED);
        student.setCreatedAt(now);
        student.setUpdatedAt(now);

        Student saved = studentRepository.saveAndFlush(student);
        assertNotNull(saved.getId());
        assertEquals(StudentStatus.DEACTIVATED, studentRepository.findByAccountId(accountId).orElseThrow().getStatus());
    }

    @Test
    void activeDiscussionCompetencyLookupExecutesAgainstTheExplicitMigrationTestDatabase() {
        assertEquals("css_ncii_java_db_migration_test", jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));
        assertEquals(java.util.Optional.empty(), discussionRepository.findActiveCompetencyIdByDiscussionId(-1L));
    }
}
