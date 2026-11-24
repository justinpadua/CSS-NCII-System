package com.cssncii.service;

import com.cssncii.dto.StudentReviewRequest;
import com.cssncii.entity.Admin;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.AdminRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.security.StudentPrincipal;
import jakarta.persistence.EntityNotFoundException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    public StudentService(
        StudentRepository studentRepository,
        AdminRepository adminRepository,
        PasswordEncoder passwordEncoder,
        SessionRegistry sessionRegistry
    ) {
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional
    public StudentRegistrationResult registerStudent(String fullName, String program, String password) {
        String name = requiredText("fullName", fullName, 200);
        String normalizedProgram = requiredText("program", program, 150);
        String rawPassword = requiredPassword(password);
        boolean possibleDuplicate = hasDuplicate(name, normalizedProgram, null);
        Student student = createAccount(name, normalizedProgram, rawPassword);
        // Return only scalar response data while the transaction is still open. Do not let a
        // persistence entity (and any lazy relationships added later) cross the registration boundary.
        return new StudentRegistrationResult(student.getAccountId(), student.getStatus(), possibleDuplicate);
    }

    @Transactional
    public StudentCreationResult createStudent(String fullName, String program, String initialPassword) {
        String name = requiredText("fullName", fullName, 200);
        String normalizedProgram = requiredText("program", program, 150);
        String rawPassword = requiredPassword(initialPassword);
        boolean possibleDuplicate = hasDuplicate(name, normalizedProgram, null);
        return new StudentCreationResult(createAccount(name, normalizedProgram, rawPassword), possibleDuplicate);
    }

    private Student createAccount(String fullName, String program, String rawPassword) {
        LocalDateTime now = utcNow();
        Student student = new Student();
        // A private unique placeholder is replaced with the final ID before this transaction commits.
        student.setAccountId("TMP-" + UUID.randomUUID());
        student.setFullName(fullName);
        student.setProgram(program);
        student.setPasswordHash(passwordEncoder.encode(rawPassword));
        student.setStatus(StudentStatus.PENDING);
        student.setCreatedAt(now);
        student.setUpdatedAt(now);
        studentRepository.saveAndFlush(student);
        student.setAccountId(formatAccountId(student.getId()));
        return studentRepository.saveAndFlush(student);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName", "id"));
    }

    public List<Student> getPendingStudents() {
        return studentRepository.findByStatusOrderByFullNameAscIdAsc(StudentStatus.PENDING);
    }

    public Set<Long> getPossibleDuplicateStudentIds() {
        Map<String, List<Student>> groups = studentRepository.findAll().stream()
            .collect(Collectors.groupingBy(student -> comparisonKey(student.getFullName())
                + "\u0000" + comparisonKey(student.getProgram())));
        Set<Long> ids = new HashSet<>();
        groups.values().stream().filter(group -> group.size() > 1)
            .forEach(group -> group.forEach(student -> ids.add(student.getId())));
        return ids;
    }

    public Student getStudentById(Long studentRecordId) {
        return findStudent(studentRecordId);
    }

    @Transactional
    public Student updateStudent(Long studentRecordId, String fullName, String program) {
        Student student = findStudentForUpdate(studentRecordId);
        student.setFullName(requiredText("fullName", fullName, 200));
        student.setProgram(requiredText("program", program, 150));
        student.setUpdatedAt(utcNow());
        return studentRepository.save(student);
    }

    @Transactional
    public Student reviewStudent(Long adminId, Long studentRecordId, StudentReviewRequest.Decision decision) {
        Student student = findStudentForUpdate(studentRecordId);
        if (student.getStatus() != StudentStatus.PENDING && student.getStatus() != StudentStatus.REJECTED) {
            throw new IllegalStateException("Only pending or rejected registrations can be reviewed.");
        }
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new EntityNotFoundException("Admin not found."));
        student.setStatus(decision == StudentReviewRequest.Decision.APPROVE
            ? StudentStatus.ACTIVE : StudentStatus.REJECTED);
        student.setReviewedByAdmin(admin);
        student.setReviewedAt(utcNow());
        student.setUpdatedAt(student.getReviewedAt());
        return studentRepository.save(student);
    }

    @Transactional
    public Student setStudentStatus(Long studentRecordId, StudentStatus status) {
        Student student = findStudentForUpdate(studentRecordId);
        if (status != StudentStatus.ACTIVE && status != StudentStatus.DEACTIVATED) {
            throw new IllegalArgumentException("Status can only be ACTIVE or DEACTIVATED here.");
        }
        if (student.getStatus() != StudentStatus.ACTIVE && student.getStatus() != StudentStatus.DEACTIVATED) {
            throw new IllegalStateException("Only approved student accounts can be activated or deactivated.");
        }
        StudentStatus oldStatus = student.getStatus();
        student.setStatus(status);
        student.setUpdatedAt(utcNow());
        Student saved = studentRepository.save(student);
        if (status == StudentStatus.DEACTIVATED && oldStatus != StudentStatus.DEACTIVATED) {
            invalidateStudentSessions(studentRecordId);
        }
        return saved;
    }

    @Transactional
    public Student updateAdminNotes(Long studentRecordId, String notes) {
        Student student = findStudentForUpdate(studentRecordId);
        if (notes != null && notes.length() > 65535) {
            throw new IllegalArgumentException("Admin notes are too long.");
        }
        student.setAdminNotes(notes == null || notes.isBlank() ? null : notes.trim());
        student.setUpdatedAt(utcNow());
        return studentRepository.save(student);
    }

    @Transactional
    public void resetPassword(Long studentRecordId, String newPassword) {
        Student student = findStudentForUpdate(studentRecordId);
        student.setPasswordHash(passwordEncoder.encode(requiredPassword(newPassword)));
        student.setUpdatedAt(utcNow());
        studentRepository.save(student);
        invalidateStudentSessions(studentRecordId);
    }

    public Student authenticate(String accountId, String password) {
        String normalizedAccountId = requiredText("accountId", accountId, 50);
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        Student student = studentRepository.findByAccountId(normalizedAccountId)
            .orElseThrow(() -> new EntityNotFoundException("Invalid Account ID or password."));
        if (student.getStatus() != StudentStatus.ACTIVE || student.getPasswordHash() == null
            || !passwordEncoder.matches(password, student.getPasswordHash())) {
            throw new EntityNotFoundException("Invalid Account ID or password.");
        }
        return student;
    }

    public boolean hasPossibleDuplicate(Student student) {
        return hasDuplicate(student.getFullName(), student.getProgram(), student.getId());
    }

    private boolean hasDuplicate(String fullName, String program, Long excludedId) {
        String normalizedName = comparisonKey(fullName);
        String normalizedProgram = comparisonKey(program);
        return studentRepository.findAll().stream()
            .anyMatch(other -> !other.getId().equals(excludedId)
                && comparisonKey(other.getFullName()).equals(normalizedName)
                && comparisonKey(other.getProgram()).equals(normalizedProgram));
    }

    private String comparisonKey(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        normalized = normalized.replaceAll("(?U)\\s+", " ");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private void invalidateStudentSessions(Long studentId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof StudentPrincipal student && student.id().equals(studentId)) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    session.expireNow();
                }
            }
        }
    }

    private Student findStudent(Long studentRecordId) {
        validateId(studentRecordId);
        return studentRepository.findById(studentRecordId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found."));
    }

    private Student findStudentForUpdate(Long studentRecordId) {
        validateId(studentRecordId);
        return studentRepository.findByIdForUpdate(studentRecordId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found."));
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("Student ID must be positive.");
    }

    private String requiredText(String fieldName, String value, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " is required.");
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return normalized;
    }

    private String requiredPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
        if (password.length() > 128) throw new IllegalArgumentException("Password must be at most 128 characters.");
        return password;
    }

    private String formatAccountId(Long id) {
        return "STU-" + String.format(Locale.ROOT, "%06d", id);
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public record StudentRegistrationResult(String accountId, StudentStatus status, boolean possibleDuplicate) {}
    public record StudentCreationResult(Student student, boolean possibleDuplicate) {}
}
