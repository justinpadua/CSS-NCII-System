package com.cssncii.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.dto.AdminReportingDtos.AssessmentStatistics;
import com.cssncii.dto.AdminReportingDtos.DashboardSummary;
import com.cssncii.dto.AdminReportingDtos.LearningContentStatistics;
import com.cssncii.dto.AdminReportingDtos.StudentStatistics;
import com.cssncii.repository.AdminRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.service.AdminReportingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminReportingController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class})
class AdminReportingControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminRepository adminRepository;
    @MockitoBean private AdminReportingService reportingService;

    @Test
    void reportingEndpointsRejectUnauthenticatedAndStudentSessions() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/student-progress").with(user("student").roles("STUDENT")))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/student-progress/filters/programs").with(user("student").roles("STUDENT")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanLoadLiveReportingEndpoints() throws Exception {
        when(reportingService.getDashboardSummary()).thenReturn(new DashboardSummary(
            new StudentStatistics(0, 0, 0, 0, 0), new AssessmentStatistics(0, 0, 0, 0, 0, 0, 0),
            new LearningContentStatistics(0, 0, 0, 0, 0)));
        when(reportingService.getPrograms()).thenReturn(List.of("BSCS"));

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.students.total").value(0))
            .andExpect(jsonPath("$.assessments.draft").value(0));
        mockMvc.perform(get("/api/admin/student-progress/filters/programs").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("BSCS"));
    }
}
