package com.cssncii.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.dto.StudentReviewRequest;
import com.cssncii.entity.Admin;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.AdminRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.service.StudentService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(StudentController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class, AdminAuthController.class})
class StudentControllerTest {
    private static final String URL = "/api/admin/students";
    private static final String USER = "students-test-admin";
    private static final String PASSWORD = "controller-test-password";
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminRepository adminRepository;
    @MockitoBean private StudentService studentService;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(11L);
        when(admin.getUsername()).thenReturn(USER);
        when(admin.getPasswordHash()).thenReturn(new BCryptPasswordEncoder().encode(PASSWORD));
        when(adminRepository.findByUsername(USER)).thenReturn(Optional.of(admin));
        when(studentService.getPossibleDuplicateStudentIds()).thenReturn(Set.of());
        adminSession = login();
    }

    @Test
    void listsStudentRecordsWithoutSensitiveFields() throws Exception {
        when(studentService.getAllStudents()).thenReturn(List.of(student(1L, StudentStatus.ACTIVE)));
        mockMvc.perform(get(URL).session(adminSession))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].accountId").value("STU-000001"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void createsPendingAccountAndDoesNotReturnPasswordHash() throws Exception {
        Student created = student(2L, StudentStatus.PENDING);
        when(studentService.createStudent("Jamie Learner", "BSIT", "initialpass8"))
            .thenReturn(new StudentService.StudentCreationResult(created, false));
        mockMvc.perform(post(URL).session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Jamie Learner\",\"program\":\"BSIT\",\"initialPassword\":\"initialpass8\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.accountId").value("STU-000002"))
            .andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void updatesStudentProfileWithoutChangingGeneratedAccountId() throws Exception {
        Student updated = student(1L, StudentStatus.ACTIVE);
        updated.setFullName("Alex Updated");
        when(studentService.updateStudent(1L, "Alex Updated", "BSIT")).thenReturn(updated);
        mockMvc.perform(put(URL + "/1").session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Alex Updated\",\"program\":\"BSIT\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accountId").value("STU-000001"));
    }

    @Test
    void approvesAndRejectsStudentRegistrations() throws Exception {
        Student active = student(1L, StudentStatus.ACTIVE);
        when(studentService.reviewStudent(11L, 1L, StudentReviewRequest.Decision.APPROVE)).thenReturn(active);
        mockMvc.perform(patch(URL + "/1/review").session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVE\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deactivatesAndReactivatesAccountsByStatus() throws Exception {
        Student inactive = student(1L, StudentStatus.DEACTIVATED);
        when(studentService.setStudentStatus(1L, StudentStatus.DEACTIVATED)).thenReturn(inactive);
        mockMvc.perform(patch(URL + "/1/status").session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DEACTIVATED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DEACTIVATED"));
    }

    @Test
    void validatesRequiredFieldsAndPasswordLength() throws Exception {
        mockMvc.perform(post(URL).session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\" \",\"program\":\"BSIT\",\"initialPassword\":\"short\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch(URL + "/1/status").session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void handlesDatabaseUniquenessAndMissingRecords() throws Exception {
        when(studentService.createStudent("Name", "Program", "initialpass8"))
            .thenThrow(new DataIntegrityViolationException("duplicate"));
        mockMvc.perform(post(URL).session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Name\",\"program\":\"Program\",\"initialPassword\":\"initialpass8\"}"))
            .andExpect(status().isConflict());
        when(studentService.getStudentById(404L)).thenThrow(new EntityNotFoundException("missing"));
        mockMvc.perform(get(URL + "/404").session(adminSession)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnauthenticatedManagementAndMissingCsrf() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL).session(adminSession).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Name\",\"program\":\"Program\",\"initialPassword\":\"initialpass8\"}"))
            .andExpect(status().isForbidden());
    }

    private MockHttpSession login() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = JsonPath.read(mockMvc.perform(get("/api/admin/auth/csrf").session(session)).andReturn()
            .getResponse().getContentAsString(), "$.token");
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login").session(session)
                .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + USER + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Student student(Long id, StudentStatus status) {
        Student student = new Student();
        student.setId(id);
        student.setAccountId(String.format("STU-%06d", id));
        student.setFullName("Alex Student");
        student.setProgram("BSCS");
        student.setStatus(status);
        student.setCreatedAt(LocalDateTime.parse("2026-01-01T00:00:00"));
        student.setUpdatedAt(student.getCreatedAt());
        return student;
    }
}
