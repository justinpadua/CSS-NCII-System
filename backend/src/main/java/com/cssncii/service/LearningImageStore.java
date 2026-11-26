package com.cssncii.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/** Storage provider for validated learning images. */
public interface LearningImageStore {
    UploadedImage store(MultipartFile file);
    StoredImage get(String filename);

    record UploadedImage(String filename, String contentType) {}
    record StoredImage(String filename, String contentType, Resource resource) {}
}
