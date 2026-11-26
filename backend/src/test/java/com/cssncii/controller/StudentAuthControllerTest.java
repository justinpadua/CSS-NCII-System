package com.cssncii.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.entity.Admin;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.AdminRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.security.StudentPrincipal;
import com.cssncii.service.StudentAuthenticationService;
import com.cssncii.service.StudentService;
import com.jayway.jsonpath.JsonPath;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(StudentAuthController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class, AdminAuthController.class})
class StudentAuthControllerTest {
    private static final String LOGIN = "/api/student/auth/login";
    private static final String ME = "/api/student/auth/me";
    private static final String LOGOUT = "/api/student/auth/logout";
    @Autowired private MockMvc mockMvc;
    @Autowired private SessionRegistry sessionRegistry;
    @MockitoBean private AdminRepository adminRepository;
    @MockitoBean private StudentAuthenticationService authenticationService;
    @MockitoBean private StudentService studentService;

    @BeforeEach
    void setup() {
        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(3L);
        when(admin.getUsername()).thenReturn("test-admin");
        when(admin.getPasswordHash()).thenReturn(new BCryptPasswordEncoder().encode("test-admin-password"));
        when(adminRepository.findByUsername("test-admin")).thenReturn(Optional.of(admin));
    }

    @Test
    void registrationReturnsGeneratedIdPendingStatusAndDuplicateFlag() throws Exception {
        Student pending = student(27L, StudentStatus.PENDING);
        when(studentService.registerStudent("Jordan Student", "BSCS", "student-pass-8"))
            .thenReturn(new StudentService.StudentRegistrationResult(
                pending.getAccountId(), pending.getStatus(), true));
        MockHttpSession session = new MockHttpSession();
        String token = csrfToken(session);
        mockMvc.perform(post("/api/student/auth/register").session(session).header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Jordan Student\",\"program\":\"BSCS\",\"password\":\"student-pass-8\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.accountId").value("STU-000027"))
            .andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.possibleDuplicate").value(true))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void activeLoginCreatesStudentRoleSessionAndSafeMeResponse() throws Exception {
        when(authenticationService.authenticate("STU-000027", "student-pass-8"))
            .thenReturn(student(27L, StudentStatus.ACTIVE));
        MockHttpSession session = login("STU-000027", "student-pass-8");
        mockMvc.perform(get(ME).session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(27)).andExpect(jsonPath("$.accountId").value("STU-000027"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.adminNotes").doesNotExist());
        org.junit.jupiter.api.Assertions.assertFalse(sessionRegistry.getAllSessions(
            new StudentPrincipal(27L, "STU-000027", "Jordan Student", "BSCS"), false).isEmpty());
    }

    @Test
    void invalidAccountOrPasswordReturnsUnauthorized() throws Exception {
        when(authenticationService.authenticate("STU-000001", "wrong-pass"))
            .thenThrow(new jakarta.persistence.EntityNotFoundException("not found"));
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post(LOGIN).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"STU-000001\",\"password\":\"wrong-pass\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequiresAccountIdPasswordAndCsrf() throws Exception {
        mockMvc.perform(post(LOGIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\" \",\"password\":\"\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"STU-000001\",\"password\":\"password8\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedSessionIsRejectedAndAdminCannotUseStudentEndpoint() throws Exception {
        mockMvc.perform(get(ME)).andExpect(status().isUnauthorized());
        MockHttpSession adminSession = adminLoginSession();
        mockMvc.perform(get(ME).session(adminSession)).andExpect(status().isForbidden());
    }

    @Test
    void corsAllowsProductionFrontendCredentialsOnStudentMeAndPreflight() throws Exception {
        String productionOrigin = "https://css-ncii-frontend.onrender.com";

        mockMvc.perform(options(ME)
                .header(HttpHeaders.ORIGIN, productionOrigin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-csrf-token"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, productionOrigin))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mockMvc.perform(get(ME).header(HttpHeaders.ORIGIN, productionOrigin))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, productionOrigin))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void corsPreservesConfiguredLocalhostOriginAndDoesNotUseWildcard() throws Exception {
        String localOrigin = "http://localhost:5173";

        mockMvc.perform(options(ME)
                .header(HttpHeaders.ORIGIN, localOrigin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, localOrigin))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void studentCannotUseAdminEndpointAndLogoutInvalidatesSession() throws Exception {
        when(authenticationService.authenticate("STU-000028", "student-pass-8"))
            .thenReturn(student(28L, StudentStatus.ACTIVE));
        MockHttpSession session = login("STU-000028", "student-pass-8");
        mockMvc.perform(get("/api/admin/auth/me").session(session)).andExpect(status().isForbidden());
        String token = csrfToken(session);
        mockMvc.perform(post(LOGOUT).session(session).header("X-CSRF-TOKEN", token)).andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        mockMvc.perform(get(ME)).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredStudentSessionIsInvalidatedAndRejected() throws Exception {
        when(authenticationService.authenticate("STU-000029", "student-pass-8"))
            .thenReturn(student(29L, StudentStatus.ACTIVE));
        MockHttpSession session = login("STU-000029", "student-pass-8");
        StudentPrincipal principal = new StudentPrincipal(29L, "STU-000029", "Jordan Student", "BSCS");
        sessionRegistry.getAllSessions(principal, false).getFirst().expireNow();
        mockMvc.perform(get(ME).session(session)).andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }

    private MockHttpSession login(String accountId, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = csrfToken(session);
        MvcResult result = mockMvc.perform(post(LOGIN).session(session).header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(true)).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpSession adminLoginSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = csrfToken(session);
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login").session(session).header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\",\"password\":\"test-admin-password\"}"))
            .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String csrfToken(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/student/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private Student student(Long id, StudentStatus status) {
        Student student = new Student();
        student.setId(id);
        student.setAccountId(String.format("STU-%06d", id));
        student.setFullName("Jordan Student");
        student.setProgram("BSCS");
        student.setStatus(status);
        return student;
    }
}
