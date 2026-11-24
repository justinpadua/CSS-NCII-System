package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cssncii.dto.StudentReviewRequest;
import com.cssncii.entity.Admin;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.AdminRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.security.StudentPrincipal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.persistence.EntityNotFoundException;

class StudentServiceTest {
    private StudentRepository students;
    private AdminRepository admins;
    private PasswordEncoder encoder;
    private SessionRegistry sessions;
    private StudentService service;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setup() {
        students = mock(StudentRepository.class);
        admins = mock(AdminRepository.class);
        encoder = mock(PasswordEncoder.class);
        sessions = mock(SessionRegistry.class);
        service = new StudentService(students, admins, encoder, sessions);
        when(encoder.encode(any())).thenAnswer(call -> "bcrypt:" + call.getArgument(0));
        when(students.findAll()).thenReturn(List.of());
        when(students.saveAndFlush(any(Student.class))).thenAnswer(call -> {
            Student student = call.getArgument(0);
            if (student.getId() == null) student.setId(ids.getAndIncrement());
            return student;
        });
        when(students.save(any(Student.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void createsPendingAccountWithGeneratedIdAndHash() {
        StudentService.StudentRegistrationResult result = service.registerStudent(" Alex Student ", " BSCS ", "secret123");
        assertEquals("STU-000001", result.accountId());
        assertEquals(StudentStatus.PENDING, result.status());
        assertFalse(result.possibleDuplicate());
        var saved = org.mockito.ArgumentCaptor.forClass(Student.class);
        verify(students, times(2)).saveAndFlush(saved.capture());
        Student persisted = saved.getAllValues().getLast();
        assertEquals("Alex Student", persisted.getFullName());
        assertEquals("BSCS", persisted.getProgram());
        assertEquals("bcrypt:secret123", persisted.getPasswordHash());
        assertNotNull(persisted.getId());
    }

    @Test
    void generatedAccountIdExpandsPastSixDigits() {
        ids.set(1_000_000L);
        Student created = service.createStudent("Name", "Program", "password8").student();
        assertEquals("STU-1000000", created.getAccountId());
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        assertThrows(IllegalArgumentException.class, () -> service.registerStudent("Name", "Program", "1234567"));
        verify(students, never()).saveAndFlush(any());
    }

    @Test
    void flagsNormalizedDuplicateWithoutRejectingRegistration() {
        Student existing = student(9L, "STU-000009", "Jose\u0301   Alvarez", "bscs", StudentStatus.REJECTED);
        when(students.findAll()).thenReturn(List.of(existing));
        StudentService.StudentRegistrationResult result = service.registerStudent(" José Alvarez ", " BSCS ", "password8");
        assertTrue(result.possibleDuplicate());
        assertEquals(StudentStatus.PENDING, result.status());
    }

    @Test
    void activeAccountAuthenticatesWithAccountIdAndPassword() {
        Student active = student(3L, "STU-000003", "Name", "Program", StudentStatus.ACTIVE);
        active.setPasswordHash("stored-hash");
        when(students.findByAccountId("STU-000003")).thenReturn(java.util.Optional.of(active));
        when(encoder.matches("password8", "stored-hash")).thenReturn(true);
        assertSame(active, service.authenticate(" STU-000003 ", "password8"));
    }

    @Test
    void pendingRejectedAndDeactivatedAccountsCannotAuthenticate() {
        for (StudentStatus status : List.of(StudentStatus.PENDING, StudentStatus.REJECTED, StudentStatus.DEACTIVATED)) {
            Student student = student(1L, "STU-000001", "Name", "Program", status);
            student.setPasswordHash("stored-hash");
            when(students.findByAccountId("STU-000001")).thenReturn(java.util.Optional.of(student));
            assertThrows(EntityNotFoundException.class, () -> service.authenticate("STU-000001", "password8"));
        }
        verify(encoder, never()).matches(any(), any());
    }

    @Test
    void approvalAndRejectedReconsiderationUpdateLatestReviewer() {
        Student rejected = student(4L, "STU-000004", "Name", "Program", StudentStatus.REJECTED);
        Admin admin = new Admin();
        admin.setId(7L);
        when(students.findByIdForUpdate(4L)).thenReturn(java.util.Optional.of(rejected));
        when(admins.findById(7L)).thenReturn(java.util.Optional.of(admin));
        Student approved = service.reviewStudent(7L, 4L, StudentReviewRequest.Decision.APPROVE);
        assertEquals(StudentStatus.ACTIVE, approved.getStatus());
        assertSame(admin, approved.getReviewedByAdmin());
        assertNotNull(approved.getReviewedAt());
    }

    @Test
    void deactivationAndPasswordResetExpireRegisteredStudentSessions() {
        Student active = student(5L, "STU-000005", "Name", "Program", StudentStatus.ACTIVE);
        when(students.findByIdForUpdate(5L)).thenReturn(java.util.Optional.of(active));
        StudentPrincipal principal = new StudentPrincipal(5L, active.getAccountId(), active.getFullName(), active.getProgram());
        SessionInformation information = mock(SessionInformation.class);
        when(sessions.getAllPrincipals()).thenReturn(List.of(principal));
        when(sessions.getAllSessions(principal, false)).thenReturn(List.of(information));
        service.setStudentStatus(5L, StudentStatus.DEACTIVATED);
        verify(information).expireNow();

        active.setStatus(StudentStatus.ACTIVE);
        service.resetPassword(5L, "newpass8");
        verify(information, times(2)).expireNow();
    }

    private Student student(Long id, String accountId, String name, String program, StudentStatus status) {
        Student student = new Student();
        student.setId(id);
        student.setAccountId(accountId);
        student.setFullName(name);
        student.setProgram(program);
        student.setStatus(status);
        return student;
    }
}
