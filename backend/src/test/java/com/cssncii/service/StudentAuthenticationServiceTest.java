package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cssncii.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentAuthenticationServiceTest {
    private StudentService studentService;
    private StudentAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        studentService = mock(StudentService.class);
        authenticationService = new StudentAuthenticationService(studentService);
    }

    @Test
    void delegatesAccountIdAndPasswordAuthentication() {
        Student student = new Student();
        when(studentService.authenticate("STU-000027", "password8")).thenReturn(student);
        assertSame(student, authenticationService.authenticate("STU-000027", "password8"));
    }
}
