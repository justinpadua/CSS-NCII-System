package com.cssncii.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class CourseStructureDtos {
    private static final long MAX_DISPLAY_ORDER = 4_294_967_295L;

    private CourseStructureDtos() {}

    public record CompetencyRequest(
        @NotBlank @Pattern(regexp = "(?i)COC[12]", message = "Code must be COC1 or COC2.")
        @Size(max = 20) String code,
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull @PositiveOrZero @Max(MAX_DISPLAY_ORDER) Long displayOrder
    ) {}

    public record DiscussionRequest(
        @NotBlank @Size(max = 250) String title,
        String description,
        @NotNull @PositiveOrZero @Max(MAX_DISPLAY_ORDER) Long displayOrder
    ) {}

    public record TopicRequest(
        @NotBlank @Size(max = 250) String title,
        String description,
        @NotNull @PositiveOrZero @Max(MAX_DISPLAY_ORDER) Long displayOrder
    ) {}

    public record ActiveStatusRequest(@NotNull Boolean active) {}

    public record CompetencyResponse(
        Long id, String code, String title, String description, long displayOrder, boolean active
    ) {}

    public record DiscussionResponse(
        Long id, Long competencyId, String title, String description, long displayOrder, boolean active
    ) {}

    public record TopicResponse(
        Long id, Long discussionId, String title, String description, long displayOrder, boolean active
    ) {}
}
