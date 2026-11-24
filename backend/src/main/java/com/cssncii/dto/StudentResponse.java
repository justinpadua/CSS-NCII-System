package com.cssncii.dto;

import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStatus;
import java.time.LocalDateTime;

public record StudentResponse(
    Long id,
    String accountId,
    String fullName,
    String program,
    StudentStatus status,
    String adminNotes,
    Long reviewedByAdminId,
    LocalDateTime reviewedAt,
    boolean possibleDuplicate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static StudentResponse from(Student student, boolean possibleDuplicate) {
        return new StudentResponse(
            student.getId(), student.getAccountId(), student.getFullName(), student.getProgram(),
            student.getStatus(), student.getAdminNotes(),
            student.getReviewedByAdmin() == null ? null : student.getReviewedByAdmin().getId(),
            student.getReviewedAt(), possibleDuplicate, student.getCreatedAt(), student.getUpdatedAt());
    }
}
