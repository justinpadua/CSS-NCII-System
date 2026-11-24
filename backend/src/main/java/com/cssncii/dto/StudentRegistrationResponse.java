package com.cssncii.dto;

import com.cssncii.entity.StudentStatus;

public record StudentRegistrationResponse(
    String accountId,
    StudentStatus status,
    boolean possibleDuplicate,
    String message
) {
}
