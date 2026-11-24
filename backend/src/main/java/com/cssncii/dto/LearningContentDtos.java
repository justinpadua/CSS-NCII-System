package com.cssncii.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class LearningContentDtos {
    private LearningContentDtos() {}

    public record StageContentRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull String contentBody
    ) {}

    public record StageContentResponse(
        Long id,
        Long discussionId,
        String stageCode,
        String title,
        String contentBody,
        short displayOrder,
        boolean active,
        boolean hasContent
    ) {}
}
