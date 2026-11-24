package com.cssncii.controller;

import com.cssncii.dto.AdminStudentCreateRequest;
import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.dto.StudentNotesRequest;
import com.cssncii.dto.StudentPasswordResetRequest;
import com.cssncii.dto.StudentResponse;
import com.cssncii.dto.StudentReviewRequest;
import com.cssncii.dto.StudentStatusRequest;
import com.cssncii.dto.StudentUpdateRequest;
import com.cssncii.entity.Student;
import com.cssncii.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cssncii.security.AdminPrincipal;

@RestController
@RequestMapping("/api/admin/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<StudentResponse> getAllStudents() {
        Set<Long> duplicates = studentService.getPossibleDuplicateStudentIds();
        return studentService.getAllStudents().stream()
            .map(student -> StudentResponse.from(student, duplicates.contains(student.getId())))
            .toList();
    }

    @GetMapping("/pending")
    public List<StudentResponse> getPendingStudents() {
        Set<Long> duplicates = studentService.getPossibleDuplicateStudentIds();
        return studentService.getPendingStudents().stream()
            .map(student -> StudentResponse.from(student, duplicates.contains(student.getId())))
            .toList();
    }

    @GetMapping("/{id}")
    public StudentResponse getStudent(@PathVariable Long id) {
        Student student = studentService.getStudentById(id);
        return StudentResponse.from(student, studentService.hasPossibleDuplicate(student));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody AdminStudentCreateRequest request) {
        StudentService.StudentCreationResult result = studentService.createStudent(
            request.fullName(), request.program(), request.initialPassword());
        Student student = result.student();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(StudentResponse.from(student, result.possibleDuplicate()));
    }

    @PutMapping("/{id}")
    public StudentResponse updateStudent(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        Student student = studentService.updateStudent(id, request.fullName(), request.program());
        return StudentResponse.from(student, studentService.hasPossibleDuplicate(student));
    }

    @PatchMapping("/{id}/review")
    public StudentResponse reviewStudent(
        @PathVariable Long id,
        @Valid @RequestBody StudentReviewRequest request,
        @AuthenticationPrincipal AdminPrincipal admin
    ) {
        Student student = studentService.reviewStudent(admin.getId(), id, request.decision());
        return StudentResponse.from(student, studentService.hasPossibleDuplicate(student));
    }

    @PatchMapping("/{id}/status")
    public StudentResponse setStudentStatus(@PathVariable Long id, @Valid @RequestBody StudentStatusRequest request) {
        Student student = studentService.setStudentStatus(id, request.status());
        return StudentResponse.from(student, studentService.hasPossibleDuplicate(student));
    }

    @PutMapping("/{id}/notes")
    public StudentResponse updateNotes(@PathVariable Long id, @Valid @RequestBody StudentNotesRequest request) {
        Student student = studentService.updateAdminNotes(id, request.adminNotes());
        return StudentResponse.from(student, studentService.hasPossibleDuplicate(student));
    }

    @PostMapping("/{id}/password-reset")
    public ResponseEntity<Void> resetPassword(
        @PathVariable Long id,
        @Valid @RequestBody StudentPasswordResetRequest request
    ) {
        studentService.resetPassword(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "Request contains invalid fields."
            : fieldError.getField() + " is required or invalid.";
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Request body or path contains an invalid value.");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleInvalidArgument(RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage() == null
            ? "Request contains invalid fields." : exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Student record not found.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrityConflict(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "The generated Account ID conflicts with an existing account. Retry the request.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(message));
    }
}
