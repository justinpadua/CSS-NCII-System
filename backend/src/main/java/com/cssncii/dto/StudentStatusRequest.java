package com.cssncii.dto;

import com.cssncii.entity.StudentStatus;
import jakarta.validation.constraints.NotNull;

public record StudentStatusRequest(@NotNull StudentStatus status) {
}
