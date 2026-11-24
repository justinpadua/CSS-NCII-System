package com.cssncii.service;

import com.cssncii.dto.StudentAccountResponse;
import com.cssncii.entity.Student;
import org.springframework.stereotype.Service;

@Service
public class StudentAuthenticationService {
    private final StudentService studentService;

    public StudentAuthenticationService(StudentService studentService) {
        this.studentService = studentService;
    }

    public Student authenticate(String accountId, String password) {
        return studentService.authenticate(accountId, password);
    }

    public StudentAccountResponse toAccountResponse(Student student) {
        return new StudentAccountResponse(
            student.getId(), student.getAccountId(), student.getFullName(), student.getProgram());
    }
}
