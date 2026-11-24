package com.cssncii.dto;

import jakarta.validation.constraints.NotNull;

public record StudentReviewRequest(@NotNull Decision decision) {
    public enum Decision { APPROVE, REJECT }
}
