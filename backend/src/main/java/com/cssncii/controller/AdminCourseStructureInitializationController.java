package com.cssncii.controller;

import com.cssncii.service.CourseStructureInitializationService;
import com.cssncii.service.CourseStructureInitializationService.InitializationResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/course-structure")
public class AdminCourseStructureInitializationController {
    private final CourseStructureInitializationService initializationService;

    public AdminCourseStructureInitializationController(CourseStructureInitializationService initializationService) {
        this.initializationService = initializationService;
    }

    @PostMapping("/initialize")
    public InitializationResult initialize() {
        return initializationService.initialize();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<InitializationError> structureConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new InitializationError(exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<InitializationError> dataConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new InitializationError("A course structure record conflicts with existing data. Refresh and review the structure before retrying."));
    }

    public record InitializationError(String message) {}
}
