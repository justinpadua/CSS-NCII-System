package com.cssncii.dto;

public final class LearningImageDtos {
    private LearningImageDtos() {}

    public record UploadResponse(String filename, String url, String contentType) {}
}
