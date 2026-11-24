package com.cssncii.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRegistrationRequest(
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank @Size(max = 150) String program,
    @NotBlank @Size(min = 8, max = 128) String password
) {
}
