package com.cssncii.dto;

import jakarta.validation.constraints.NotBlank;

public record StudentLoginRequest(@NotBlank String accountId, @NotBlank String password) {
}
