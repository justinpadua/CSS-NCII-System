package com.cssncii.controller;

import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.dto.LearningImageDtos.UploadResponse;
import com.cssncii.service.LearningImageStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/learning-images")
public class AdminLearningImageController {
    private final LearningImageStorageService imageStorage;

    public AdminLearningImageController(LearningImageStorageService imageStorage) {
        this.imageStorage = imageStorage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        LearningImageStorageService.UploadedImage uploaded = imageStorage.store(file);
        return new UploadResponse(uploaded.filename(),
            "/api/student/learning/images/" + uploaded.filename(), uploaded.contentType());
    }

    @GetMapping("/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> preview(@PathVariable String filename) {
        return LearningImageHttpResponse.from(imageStorage.get(filename));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> invalidImage(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> oversizedImage(MaxUploadSizeExceededException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Image files must be 5 MB or smaller.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Learning image not found.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> storageUnavailable(IllegalStateException exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "The learning image could not be stored.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(message));
    }
}
