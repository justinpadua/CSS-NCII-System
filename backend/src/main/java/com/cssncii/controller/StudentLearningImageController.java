package com.cssncii.controller;

import com.cssncii.security.StudentPrincipal;
import com.cssncii.service.StudentLearningImageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/learning/images")
public class StudentLearningImageController {
    private final StudentLearningImageService imageService;

    public StudentLearningImageController(StudentLearningImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(
        @AuthenticationPrincipal StudentPrincipal principal,
        @PathVariable String filename
    ) {
        return LearningImageHttpResponse.from(imageService.getImageForStudent(principal.id(), filename));
    }
}
