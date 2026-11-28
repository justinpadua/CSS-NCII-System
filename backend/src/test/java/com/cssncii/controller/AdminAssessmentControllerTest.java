package com.cssncii.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.entity.Admin;
import com.cssncii.entity.AnswerChoice;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Question;
import com.cssncii.repository.AdminRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.service.AssessmentManagementService;
import com.cssncii.service.CourseManagementService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

@WebMvcTest(AdminAssessmentController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class, AdminAuthController.class})
class AdminAssessmentControllerTest {
    private static final String USER = "assessment-admin";
    private static final String PASSWORD = "assessment-admin-password";
    private static final String DISCUSSION_URL = "/api/admin/competencies/2/discussions/41/assessments";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminRepository adminRepository;
    @MockitoBean private AssessmentManagementService assessmentService;
    @MockitoBean private CourseManagementService courseService;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        Admin admin = mock(Admin.class);
        when(admin.getUsername()).thenReturn(USER);
        when(admin.getPasswordHash()).thenReturn(new BCryptPasswordEncoder().encode(PASSWORD));
        when(adminRepository.findByUsername(USER)).thenReturn(Optional.of(admin));
        when(courseService.getDiscussionById(41L)).thenReturn(discussion(41L, 2L));
        adminSession = login();
    }

    @Test
    void allAssessmentManagementRoutesRequireAdminAndCsrfForWrites() throws Exception {
        mockMvc.perform(get(DISCUSSION_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(DISCUSSION_URL).session(adminSession).contentType(MediaType.APPLICATION_JSON)
                .content("{\"assessmentType\":\"PRE_TEST\",\"title\":\"Pre-test\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listsAndCreatesDiscussionAssessmentWithoutChangingItsScope() throws Exception {
        Assessment preTest = assessment(8L, 41L, null, "PRE_TEST");
        when(assessmentService.getAssessmentsByDiscussion(41L)).thenReturn(List.of(preTest));
        when(assessmentService.createAssessment("ACTIVITY", 41L, null, "Activity", "Do this", false))
            .thenReturn(assessment(9L, 41L, null, "ACTIVITY"));

        mockMvc.perform(get(DISCUSSION_URL).session(adminSession))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].assessmentType").value("PRE_TEST"))
            .andExpect(jsonPath("$[0].passPercentage").doesNotExist())
            .andExpect(jsonPath("$[0].correctAnswerData").doesNotExist());
        mockMvc.perform(post(DISCUSSION_URL).session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"assessmentType\":\"ACTIVITY\",\"title\":\"Activity\",\"instructions\":\"Do this\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.assessmentType").value("ACTIVITY"));
        verify(assessmentService).createAssessment("ACTIVITY", 41L, null, "Activity", "Do this", false);
    }

    @Test
    void createsGeneralizedPostTestOnlyUnderCompetencyAndKeepsFixedPassingRule() throws Exception {
        when(assessmentService.createAssessment("GENERALIZED_POST_TEST", null, 2L, "COC 1 Generalized Post-test", null, false))
            .thenReturn(assessment(10L, null, 2L, "GENERALIZED_POST_TEST"));

        mockMvc.perform(post("/api/admin/competencies/2/assessments").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assessmentType\":\"GENERALIZED_POST_TEST\",\"title\":\"COC 1 Generalized Post-test\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.passPercentage").value(70.00));
        verify(assessmentService).createAssessment("GENERALIZED_POST_TEST", null, 2L,
            "COC 1 Generalized Post-test", null, false);
        mockMvc.perform(post("/api/admin/competencies/2/assessments").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assessmentType\":\"PRE_TEST\",\"title\":\"Wrong scope\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createRequestsCannotPublishDiscussionOrGeneralizedAssessments() throws Exception {
        Assessment discussionDraft = assessment(11L, 41L, null, "ACTIVITY");
        discussionDraft.setActive(false);
        when(assessmentService.createAssessment("ACTIVITY", 41L, null, "Activity", null, false))
            .thenReturn(discussionDraft);
        Assessment generalizedDraft = assessment(12L, null, 2L, "GENERALIZED_POST_TEST");
        generalizedDraft.setActive(false);
        when(assessmentService.createAssessment("GENERALIZED_POST_TEST", null, 2L,
            "COC 1 Generalized Post-test", null, false)).thenReturn(generalizedDraft);

        mockMvc.perform(post(DISCUSSION_URL).session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"assessmentType\":\"ACTIVITY\",\"title\":\"Activity\",\"active\":true}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/admin/competencies/2/assessments").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assessmentType\":\"GENERALIZED_POST_TEST\",\"title\":\"COC 1 Generalized Post-test\",\"active\":true}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(false));
        verify(assessmentService).createAssessment("ACTIVITY", 41L, null, "Activity", null, false);
        verify(assessmentService).createAssessment("GENERALIZED_POST_TEST", null, 2L,
            "COC 1 Generalized Post-test", null, false);
    }

    @Test
    void adminCanActivateAndDeactivateAnAssessment() throws Exception {
        when(assessmentService.updateAssessment(8L, "Pre-test", "Instructions", true))
            .thenReturn(assessment(8L, 41L, null, "PRE_TEST"));
        when(assessmentService.updateAssessment(8L, "Pre-test", "Instructions", false))
            .thenAnswer(invocation -> {
                Assessment draft = assessment(8L, 41L, null, "PRE_TEST");
                draft.setActive(false);
                return draft;
            });

        mockMvc.perform(put("/api/admin/assessments/8").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Pre-test\",\"instructions\":\"Instructions\",\"active\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(put("/api/admin/assessments/8").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Pre-test\",\"instructions\":\"Instructions\",\"active\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        verify(assessmentService).updateAssessment(8L, "Pre-test", "Instructions", true);
        verify(assessmentService).updateAssessment(8L, "Pre-test", "Instructions", false);
    }

    @Test
    void createsConfiguredChoiceQuestionAtomicallyAndReturnsAdminAnswerSelection() throws Exception {
        Assessment assessment = assessment(8L, 41L, null, "PRE_TEST");
        Question question = question(21L, assessment, "MULTIPLE_CHOICE", "Which item?", "{\"choiceId\":31}");
        AnswerChoice first = choice(31L, question, "Keyboard", 1);
        AnswerChoice second = choice(32L, question, "Monitor", 2);
        when(assessmentService.createQuestionWithChoices(eq(8L), eq("MULTIPLE_CHOICE"), eq("Which item?"),
            eq(new BigDecimal("2.00")), eq(1L), any(), eq(List.of(0)), eq(null)))
            .thenReturn(new AssessmentManagementService.ConfiguredQuestion(question, List.of(first, second)));

        mockMvc.perform(post("/api/admin/assessments/8/questions").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionType\":\"MULTIPLE_CHOICE\",\"questionText\":\"Which item?\",\"points\":2.00,\"displayOrder\":1,\"choices\":[{\"choiceText\":\"Keyboard\",\"displayOrder\":1},{\"choiceText\":\"Monitor\",\"displayOrder\":2}],\"correctChoiceIndexes\":[0]}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.choices[0].correct").value(true))
            .andExpect(jsonPath("$.choices[1].correct").value(false))
            .andExpect(jsonPath("$.correctAnswerData").doesNotExist());
    }

    @Test
    void updateQuestionAcceptsOnlyCorrectChoicesOwnedByQuestion() throws Exception {
        Assessment assessment = assessment(8L, 41L, null, "PRE_TEST");
        Question question = question(21L, assessment, "MULTIPLE_SELECT", "Choose two", "{\"choiceIds\":[31,32]}");
        List<AnswerChoice> choices = List.of(choice(31L, question, "A", 1), choice(32L, question, "B", 2));
        when(assessmentService.getQuestionById(21L)).thenReturn(question);
        when(assessmentService.getAnswerChoicesByQuestion(21L)).thenReturn(choices);
        when(assessmentService.updateQuestion(21L, "MULTIPLE_SELECT", "Choose two", "{\"choiceIds\":[31,32]}",
            new BigDecimal("1.50"), 3L)).thenReturn(question);

        mockMvc.perform(put("/api/admin/assessments/8/questions/21").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionType\":\"MULTIPLE_SELECT\",\"questionText\":\"Choose two\",\"points\":1.50,\"displayOrder\":3,\"correctChoiceIds\":[31,32]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.correctChoiceIds[0]").value(31));

        mockMvc.perform(put("/api/admin/assessments/8/questions/21").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionType\":\"MULTIPLE_SELECT\",\"questionText\":\"Choose two\",\"points\":1.50,\"displayOrder\":3,\"correctChoiceIds\":[999]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void matchingUpdateAcceptsExpandedSharedOptionsAndPersistsPromptMappingConfiguration() throws Exception {
        Assessment assessment = assessment(8L, 41L, null, "PRE_TEST");
        Question question = question(21L, assessment, "MATCHING_TYPE", "Match components.",
            "{\"matches\":[{\"promptIndex\":0,\"choiceId\":31}]}");
        question.setQuestionConfig("{\"matchingPrompts\":[\"CPU\"]}");
        List<AnswerChoice> choices = List.of(choice(31L, question, "Core", 1), choice(32L, question, "Spare", 2));
        when(assessmentService.getQuestionById(21L)).thenReturn(question);
        when(assessmentService.updateMatchingQuestionConfiguration(eq(21L), eq("Match components."),
            eq(new BigDecimal("2.00")), eq(1L), any())).thenReturn(question);
        when(assessmentService.getAnswerChoicesByQuestion(21L)).thenReturn(choices);

        mockMvc.perform(put("/api/admin/assessments/8/questions/21").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionType\":\"MATCHING_TYPE\",\"questionText\":\"Match components.\",\"points\":2.00,\"displayOrder\":1,\"questionConfig\":{\"matchingPairs\":[{\"prompt\":\"CPU\",\"answerText\":\"Core\"}],\"distractors\":[\"Spare\"],\"sharedOptions\":[{\"id\":31,\"optionText\":\"Core\"},{\"id\":32,\"optionText\":\"Spare\"}]}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.questionConfiguration").value(
                "{\"matchingPairs\":[{\"answerText\":\"Core\",\"prompt\":\"CPU\"}],\"distractors\":[\"Spare\"]}"));
        verify(assessmentService).updateMatchingQuestionConfiguration(eq(21L), eq("Match components."),
            eq(new BigDecimal("2.00")), eq(1L), any());
    }

    @Test
    void rejectsMismatchedDiscussionAndPreventsDeletingQuestionUsedInStudentHistory() throws Exception {
        when(courseService.getDiscussionById(41L)).thenReturn(discussion(41L, 99L));
        mockMvc.perform(get(DISCUSSION_URL).session(adminSession)).andExpect(status().isNotFound());

        Question question = question(21L, assessment(8L, 41L, null, "PRE_TEST"), "TRUE_FALSE", "Prompt", "{\"value\":true}");
        when(assessmentService.getQuestionById(21L)).thenReturn(question);
        doThrow(new IllegalStateException("A question with saved student answers cannot be deleted."))
            .when(assessmentService).deleteQuestion(21L);
        mockMvc.perform(delete("/api/admin/assessments/8/questions/21").session(adminSession).with(csrf()))
            .andExpect(status().isConflict());
    }

    @Test
    void supportsAnswerChoiceCreationEditingAndDeletionWithQuestionOwnershipChecks() throws Exception {
        Assessment assessment = assessment(8L, 41L, null, "PRE_TEST");
        Question question = question(21L, assessment, "MULTIPLE_CHOICE", "Prompt", "{\"choiceId\":31}");
        AnswerChoice existing = choice(32L, question, "Old option", 2);
        AnswerChoice updated = choice(32L, question, "Revised option", 3);
        when(assessmentService.createAnswerChoice(21L, "Another option", 3)).thenReturn(updated);
        when(assessmentService.getAnswerChoiceById(32L)).thenReturn(existing);
        when(assessmentService.updateAnswerChoice(32L, "Revised option", 3)).thenReturn(updated);
        when(assessmentService.getAnswerChoicesByQuestion(21L)).thenReturn(List.of(
            choice(31L, question, "Correct option", 1), updated));

        mockMvc.perform(post("/api/admin/questions/21/choices").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"choiceText\":\"Another option\",\"displayOrder\":3}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.displayOrder").value(3));
        mockMvc.perform(put("/api/admin/questions/21/choices/32").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"choiceText\":\"Revised option\",\"displayOrder\":3}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(false));
        mockMvc.perform(delete("/api/admin/questions/21/choices/32").session(adminSession).with(csrf()))
            .andExpect(status().isNoContent());
        verify(assessmentService).deleteAnswerChoice(32L);

        AnswerChoice unrelated = choice(90L, question(90L, assessment, "TRUE_FALSE", "Other", "{\"value\":true}"), "Other", 1);
        when(assessmentService.getAnswerChoiceById(90L)).thenReturn(unrelated);
        mockMvc.perform(delete("/api/admin/questions/21/choices/90").session(adminSession).with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    void reportsMissingAssessmentAsNotFound() throws Exception {
        when(assessmentService.getQuestionsByAssessment(999L)).thenThrow(new EntityNotFoundException("missing"));
        mockMvc.perform(get("/api/admin/assessments/999/questions").session(adminSession))
            .andExpect(status().isNotFound());
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

    private Assessment assessment(Long id, Long discussionId, Long competencyId, String type) {
        Assessment value = new Assessment();
        value.setId(id);
        value.setAssessmentType(type);
        value.setTitle(type);
        value.setInstructions(null);
        value.setActive(true);
        if (discussionId != null) value.setDiscussion(discussion(discussionId, 2L));
        if (competencyId != null) {
            Competency competency = new Competency(); competency.setId(competencyId); value.setCompetency(competency);
            value.setPassPercentage(new BigDecimal("70.00"));
        }
        return value;
    }

    private Discussion discussion(Long id, Long competencyId) {
        Competency competency = new Competency(); competency.setId(competencyId);
        Discussion discussion = new Discussion(); discussion.setId(id); discussion.setCompetency(competency);
        return discussion;
    }

    private Question question(Long id, Assessment assessment, String type, String text, String answer) {
        Question question = new Question(); question.setId(id); question.setAssessment(assessment);
        question.setQuestionType(type); question.setQuestionText(text); question.setCorrectAnswerData(answer);
        question.setPoints(new BigDecimal("2.00")); question.setDisplayOrder(1); question.setCreatedAt(LocalDateTime.now());
        return question;
    }

    private AnswerChoice choice(Long id, Question question, String text, int order) {
        AnswerChoice choice = new AnswerChoice(); choice.setId(id); choice.setQuestion(question);
        choice.setChoiceText(text); choice.setDisplayOrder(order); return choice;
    }
}
