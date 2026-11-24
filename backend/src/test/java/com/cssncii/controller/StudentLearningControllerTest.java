package com.cssncii.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.repository.AdminRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.security.StudentPrincipal;
import com.cssncii.service.StudentLearningService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentLearningController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class})
class StudentLearningControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private StudentLearningService learningService;
    @MockitoBean private AdminRepository adminRepository;

    @Test
    void dashboardRequiresStudentRoleAndUsesTheSessionPrincipalId() throws Exception {
        StudentPrincipal principal = new StudentPrincipal(73L, "STU-000073", "Jordan Student", "BSCS");
        var dashboard = new StudentLearningService.Dashboard("Jordan Student", "STU-000073", "BSCS", List.of());
        when(learningService.getDashboard(73L)).thenReturn(dashboard);

        mockMvc.perform(get("/api/student/learning/dashboard"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/student/learning/dashboard")
                .with(authentication(new UsernamePasswordAuthenticationToken(
                    "admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))))))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/student/learning/dashboard")
                .with(authentication(new UsernamePasswordAuthenticationToken(
                    principal, "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value("STU-000073"))
            .andExpect(jsonPath("$.competencies").isArray())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(learningService).getDashboard(73L);
    }

    @Test
    void learningStageJsonPreservesUnicodePunctuation() throws Exception {
        StudentPrincipal principal = new StudentPrincipal(73L, "STU-000073", "Jordan Student", "BSCS");
        String body = "# COC 1 — Lesson 1\nLESSON 1 – PRE-TEST";
        when(learningService.openStage(73L, 1L, 41L, "PRE_TEST"))
            .thenReturn(new StudentLearningService.StageContent(
                41L, "PRE_TEST", "Pre-test", "IN_PROGRESS", "Pre-test", body, true));

        mockMvc.perform(post("/api/student/learning/competencies/1/discussions/41/stages/PRE_TEST/open")
                .with(authentication(new UsernamePasswordAuthenticationToken(
                    principal, "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contentBody").value(body))
            .andExpect(jsonPath("$.contentBody").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("â€”"))))
            .andExpect(jsonPath("$.contentBody").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("â€“"))));
    }
}
