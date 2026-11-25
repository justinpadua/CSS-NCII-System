package com.cssncii.controller;

import com.cssncii.service.LearningImageStorageService.StoredImage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class LearningImageHttpResponse {
    private LearningImageHttpResponse() {}

    static ResponseEntity<Resource> from(StoredImage image) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.filename() + "\"")
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header("X-Content-Type-Options", "nosniff")
            .body(new FileSystemResource(image.path()));
    }
}
