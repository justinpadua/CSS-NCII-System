package com.cssncii.exception;

public class StudentIdRequiredException extends RuntimeException {
    public StudentIdRequiredException() {
        super("Student ID is required when multiple active records match.");
    }
}
