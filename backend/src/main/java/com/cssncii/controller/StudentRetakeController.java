package com.cssncii.controller;

import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.service.GeneralizedPostTestService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cssncii.security.AdminPrincipal;

@RestController
@RequestMapping("/api/admin/students/{studentId}/competencies/{competencyId}/retake-authorizations")
public class StudentRetakeController {
    private final GeneralizedPostTestService postTestService;

    public StudentRetakeController(GeneralizedPostTestService postTestService) {
        this.postTestService = postTestService;
    }

    @PostMapping
    public GeneralizedPostTestService.RetakeAuthorizationResult authorize(
        @PathVariable Long studentId,
        @PathVariable Long competencyId,
        @AuthenticationPrincipal AdminPrincipal admin
    ) {
        return postTestService.authorizeAdditionalAttempts(admin.getId(), studentId, competencyId);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse("Student or course record not found."));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> invalid(RuntimeException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage()));
    }
}
