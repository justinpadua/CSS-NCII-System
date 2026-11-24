package com.cssncii.controller;

import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.security.StudentPrincipal;
import com.cssncii.service.StudentLearningService;
import jakarta.persistence.EntityNotFoundException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/learning")
public class StudentLearningController {
    private final StudentLearningService learningService;

    public StudentLearningController(StudentLearningService learningService) {
        this.learningService = learningService;
    }

    @GetMapping("/dashboard")
    public StudentLearningService.Dashboard getDashboard(@AuthenticationPrincipal StudentPrincipal principal) {
        return learningService.getDashboard(principal.id());
    }

    @PostMapping("/competencies/{competencyId}/discussions/{discussionId}/stages/{stageCode}/open")
    public StudentLearningService.StageContent openStage(
        @AuthenticationPrincipal StudentPrincipal principal,
        @PathVariable Long competencyId,
        @PathVariable Long discussionId,
        @PathVariable String stageCode
    ) {
        return learningService.openStage(
            principal.id(), competencyId, discussionId, stageCode.trim().toUpperCase(Locale.ROOT));
    }

    @PostMapping("/competencies/{competencyId}/discussions/{discussionId}/stages/{stageCode}/complete")
    public StudentLearningService.StageStatus completeStage(
        @AuthenticationPrincipal StudentPrincipal principal,
        @PathVariable Long competencyId,
        @PathVariable Long discussionId,
        @PathVariable String stageCode
    ) {
        return learningService.completeNonAssessmentStage(
            principal.id(), competencyId, discussionId, stageCode.trim().toUpperCase(Locale.ROOT));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> invalid(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> unavailable(IllegalStateException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "The requested course item was not found.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            message == null ? "The request could not be completed." : message));
    }
}
