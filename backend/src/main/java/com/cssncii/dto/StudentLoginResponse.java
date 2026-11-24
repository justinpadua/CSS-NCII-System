package com.cssncii.dto;

public record StudentLoginResponse(boolean authenticated, String message, StudentAccountResponse student) {
    public static StudentLoginResponse authenticated(StudentAccountResponse student) {
        return new StudentLoginResponse(true, "Student login successful.", student);
    }
}
