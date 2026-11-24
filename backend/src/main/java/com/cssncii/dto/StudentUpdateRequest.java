package com.cssncii.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentUpdateRequest(
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank @Size(max = 150) String program
) {
}
