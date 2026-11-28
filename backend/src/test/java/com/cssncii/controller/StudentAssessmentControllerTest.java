package com.cssncii.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.entity.Assessment;
import com.cssncii.repository.AdminRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.security.StudentPrincipal;
import com.cssncii.service.AssessmentAttemptService;
import com.cssncii.service.GeneralizedPostTestService;
import com.cssncii.service.StudentLearningService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@WebMvcTest(StudentAssessmentController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class})
class StudentAssessmentControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminRepository adminRepository;
    @MockitoBean private AssessmentRepository assessmentRepository;
    @MockitoBean private DiscussionRepository discussionRepository;
    @MockitoBean private StudentLearningService learningService;
    @MockitoBean private AssessmentAttemptService attemptService;
    @MockitoBean private GeneralizedPostTestService generalizedPostTestService;

    @Test
    void pretestAttemptReturnsClearNotConfiguredResponseAfterStageOpenSucceeds() throws Exception {
        StudentPrincipal principal = new StudentPrincipal(73L, "STU-000073", "Jordan Student", "BSCS");
        when(discussionRepository.findActiveCompetencyIdByDiscussionId(41L)).thenReturn(Optional.of(10L));
        when(learningService.openStage(73L, 10L, 41L, "PRE_TEST"))
            .thenReturn(new StudentLearningService.StageContent(41L, "PRE_TEST", "Pre-test", "IN_PROGRESS",
                "Pre-test", "", false));
        when(assessmentRepository.findByDiscussion_IdAndAssessmentType(41L, "PRE_TEST"))
            .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/student/assessments/discussions/41/PRE_TEST/attempts")
                .with(authentication(new UsernamePasswordAuthenticationToken(
                    principal, "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                .with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("This assessment is not configured yet."));

        var order = inOrder(learningService, assessmentRepository);
        order.verify(learningService).openStage(73L, 10L, 41L, "PRE_TEST");
        order.verify(assessmentRepository).findByDiscussion_IdAndAssessmentType(41L, "PRE_TEST");
        verify(attemptService, org.mockito.Mockito.never()).startOrResumeAttempt(any(), any());
    }

    @Test
    void pretestAttemptRejectsDraftAssessment() throws Exception {
        StudentPrincipal principal = new StudentPrincipal(73L, "STU-000073", "Jordan Student", "BSCS");
        when(discussionRepository.findActiveCompetencyIdByDiscussionId(41L)).thenReturn(Optional.of(10L));
        Assessment draft = new Assessment();
        draft.setId(91L);
        draft.setActive(false);
        when(learningService.openStage(73L, 10L, 41L, "PRE_TEST"))
            .thenReturn(new StudentLearningService.StageContent(41L, "PRE_TEST", "Pre-test", "IN_PROGRESS",
                "Pre-test", "", false));
        when(assessmentRepository.findByDiscussion_IdAndAssessmentType(41L, "PRE_TEST"))
            .thenReturn(Optional.of(draft));

        mockMvc.perform(post("/api/student/assessments/discussions/41/PRE_TEST/attempts")
                .with(authentication(new UsernamePasswordAuthenticationToken(
                    principal, "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                .with(csrf()))
            .andExpect(status().isNotFound());
        verify(attemptService, org.mockito.Mockito.never()).startOrResumeAttempt(any(), any());
    }

    @Test
    void startsDiscussionAttemptUsingScalarActiveDiscussionLookup() throws Exception {
        StudentPrincipal principal = new StudentPrincipal(73L, "STU-000073", "Jordan Student", "BSCS");
        when(discussionRepository.findActiveCompetencyIdByDiscussionId(41L)).thenReturn(Optional.of(10L));
        when(learningService.openStage(73L, 10L, 41L, "PRE_TEST"))
            .thenReturn(new StudentLearningService.StageContent(41L, "PRE_TEST", "Pre-test", "IN_PROGRESS",
                "Pre-test", "", false));
        Assessment assessment = new Assessment();
        assessment.setId(91L);
        assessment.setActive(true);
        when(assessmentRepository.findByDiscussion_IdAndAssessmentType(41L, "PRE_TEST"))
            .thenReturn(Optional.of(assessment));
        when(attemptService.startOrResumeAttempt(73L, 91L)).thenReturn(
            new AssessmentAttemptService.AttemptSummary(101L, 91L, "PRE_TEST", "Pre-test", null,
                1, "IN_PROGRESS", java.time.LocalDateTime.now()));

        mockMvc.perform(post("/api/student/assessments/discussions/41/PRE_TEST/attempts")
                .with(authentication(new UsernamePasswordAuthenticationToken(
                    principal, "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attemptId").value(101L));

        verify(discussionRepository).findActiveCompetencyIdByDiscussionId(41L);
        verify(learningService).openStage(73L, 10L, 41L, "PRE_TEST");
        verify(attemptService).startOrResumeAttempt(73L, 91L);
    }

    @Test
    void studentQuestionPromptDoesNotExposeCorrectMatchingMap() throws Exception {
        StudentPrincipal principal = new StudentPrincipal(73L, "STU-000073", "Jordan Student", "BSCS");
        when(attemptService.getQuestionsForAttempt(73L, 101L)).thenReturn(List.of(
            new AssessmentAttemptService.QuestionPrompt(21L, "MATCHING_TYPE", "Match the terms.",
                new java.math.BigDecimal("2.00"), 1,
                List.of(new AssessmentAttemptService.ChoicePrompt(31L, "Processor", 1)),
                List.of("CPU"), "{\"matchingPrompts\":[\"CPU\"]}")));

        mockMvc.perform(get("/api/student/assessments/attempts/101/questions")
                .with(authentication(new UsernamePasswordAuthenticationToken(
                    principal, "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].matchingPrompts[0]").value("CPU"))
            .andExpect(jsonPath("$[0].choices[0].choiceText").value("Processor"))
            .andExpect(jsonPath("$[0].correctAnswerData").doesNotExist())
            .andExpect(jsonPath("$[0].correctChoiceIds").doesNotExist());
    }
}
