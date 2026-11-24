package com.cssncii.controller;

import com.cssncii.dto.AdminReportingDtos.DashboardSummary;
import com.cssncii.dto.AdminReportingDtos.StudentProgressDetail;
import com.cssncii.dto.AdminReportingDtos.StudentProgressPage;
import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.service.AdminReportingService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/admin")
public class AdminReportingController {
    private final AdminReportingService reportingService;

    public AdminReportingController(AdminReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    public DashboardSummary getDashboardSummary() {
        return reportingService.getDashboardSummary();
    }

    @GetMapping("/student-progress")
    public StudentProgressPage getStudentProgressPage(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String program,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return reportingService.getStudentProgressPage(search, program, status, page, size);
    }

    @GetMapping("/student-progress/filters/programs")
    public List<String> getStudentProgressPrograms() {
        return reportingService.getPrograms();
    }

    @GetMapping("/student-progress/{studentId}")
    public StudentProgressDetail getStudentProgress(@PathVariable Long studentId) {
        return reportingService.getStudentProgress(studentId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST, "A query parameter or path value is invalid.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(message));
    }
}
