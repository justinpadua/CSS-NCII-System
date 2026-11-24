package com.cssncii.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cssncii.TestController;
import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.entity.Admin;
import com.cssncii.repository.AdminRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.jayway.jsonpath.JsonPath;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AdminAuthController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class, TestController.class})
class AdminAuthControllerTest {
    private static final String LOGIN_URL = "/api/admin/auth/login";
    private static final String CSRF_URL = "/api/admin/auth/csrf";
    private static final String ME_URL = "/api/admin/auth/me";
    private static final String LOGOUT_URL = "/api/admin/auth/logout";
    private static final String PASSWORD = "test-only-admin-password";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRepository adminRepository;

    private Admin admin;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        admin = mock(Admin.class);
        when(admin.getId()).thenReturn(17L);
        when(admin.getUsername()).thenReturn("css-admin");
        when(admin.getPasswordHash()).thenReturn(encoder.encode(PASSWORD));
        when(adminRepository.findByUsername("css-admin")).thenReturn(Optional.of(admin));
    }

    @Test
    void csrfEndpointProvidesSessionTokenAndLoginCreatesSafeAdminSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String csrfToken = getCsrfToken(session);

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                .session(session)
                .header("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"css-admin\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.username").value("css-admin"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn();

        MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get(ME_URL).session(authenticatedSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.username").value("css-admin"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void incorrectPasswordIsRejected() throws Exception {
        mockMvc.perform(post(LOGIN_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"css-admin\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void unknownUsernameIsRejected() throws Exception {
        when(adminRepository.findByUsername("unknown-admin")).thenReturn(Optional.empty());

        mockMvc.perform(post(LOGIN_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"unknown-admin\",\"password\":\"some-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void authenticatedAdminCanReadMeAndResponseNeverContainsHash() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get(ME_URL).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.username").value("css-admin"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void unauthenticatedUserCannotAccessAdminManagementEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/students"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ME_URL))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesAuthenticatedSession() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post(LOGOUT_URL).session(session).with(csrf()))
            .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        mockMvc.perform(get(ME_URL))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void studentIdentityCannotAuthenticateThroughAdminProvider() throws Exception {
        when(adminRepository.findByUsername("STUDENT-1001")).thenReturn(Optional.empty());

        mockMvc.perform(post(LOGIN_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"STUDENT-1001\",\"password\":\"student-name-program\"}"))
            .andExpect(status().isUnauthorized());

        verify(adminRepository).findByUsername("STUDENT-1001");
    }

    @Test
    void loginRequiresCsrfTokenAndHelloEndpointRemainsAvailable() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"css-admin\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/hello"))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello from CSS NCII API!"));
    }

    @Test
    void loginRejectsInvalidCsrfToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        getCsrfToken(session);

        mockMvc.perform(post(LOGIN_URL)
                .session(session)
                .header("X-CSRF-TOKEN", "invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"css-admin\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isForbidden());
    }

    private MockHttpSession login() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = getCsrfToken(session);
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .session(session)
                .header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"css-admin\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String getCsrfToken(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get(CSRF_URL).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
