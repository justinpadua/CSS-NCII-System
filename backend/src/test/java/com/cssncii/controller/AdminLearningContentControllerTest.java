package com.cssncii.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.entity.Admin;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.LearningStageContent;
import com.cssncii.repository.AdminRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.service.CourseManagementService;
import com.cssncii.service.LearningContentService;
import com.cssncii.service.MarkdownLearningContentImportService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityNotFoundException;
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

@WebMvcTest(AdminLearningContentController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class, AdminAuthController.class})
class AdminLearningContentControllerTest {
    private static final String URL = "/api/admin/competencies/1/discussions/41/learning-stages";
    private static final String USER = "content-test-admin";
    private static final String PASSWORD = "content-test-password";
    private static final List<String> STAGE_CODES = List.of(
        "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"
    );

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminRepository adminRepository;
    @MockitoBean private CourseManagementService courseManagementService;
    @MockitoBean private LearningContentService learningContentService;
    @MockitoBean private MarkdownLearningContentImportService markdownImportService;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(29L);
        when(admin.getUsername()).thenReturn(USER);
        when(admin.getPasswordHash()).thenReturn(new BCryptPasswordEncoder().encode(PASSWORD));
        when(adminRepository.findByUsername(USER)).thenReturn(Optional.of(admin));
        when(courseManagementService.getDiscussionById(41L)).thenReturn(discussion(41L, 1L));
        adminSession = login();
    }

    @Test
    void learningContentEndpointsRequireAdminAndCsrf() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(URL + "/PRE_TEST").session(adminSession).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Pre-test\",\"contentBody\":\"\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void previewsMappedMarkdownOnlyForAdminAndDoesNotWriteStageContent() throws Exception {
        when(learningContentService.getStagesForDiscussion(41L)).thenReturn(stages(41L));
        when(courseManagementService.getDiscussionMarkdownContext(1L, 41L))
            .thenReturn(new CourseManagementService.DiscussionMarkdownContext(
                1L, "COC1", 1L, "Discuss fundamental concepts of computer systems servicing"));
        when(markdownImportService.preview("COC1", 1L,
            "Discuss fundamental concepts of computer systems servicing", "PRE_TEST", ""))
            .thenReturn(new MarkdownLearningContentImportService.SourcePreview(
                "learning-content/COC-1/Lesson-01/01-Pretest.md", true, true, false, false, true,
                "# Lesson 1\\n\\nDirections: choose the best answer.",
                "Preview only: assessment questions and answer keys are excluded."));

        mockMvc.perform(get(URL + "/PRE_TEST/source-preview").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourcePath").value("learning-content/COC-1/Lesson-01/01-Pretest.md"))
            .andExpect(jsonPath("$.canLoadIntoEditor").value(true))
            .andExpect(jsonPath("$.assessmentItemsRemoved").value(true))
            .andExpect(jsonPath("$.stageBodyPreview").value("# Lesson 1\\n\\nDirections: choose the best answer."));
        verify(markdownImportService).preview("COC1", 1L,
            "Discuss fundamental concepts of computer systems servicing", "PRE_TEST", "");
        verify(courseManagementService).getDiscussionMarkdownContext(1L, 41L);
        verify(courseManagementService, never()).getDiscussionById(41L);
        verify(learningContentService, never()).updateStageContent(anyLong(), anyString(), anyString(), anyShort());
        mockMvc.perform(get(URL + "/PRE_TEST/source-preview"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void retrievesAllSevenCanonicalStagesInOrderAndShowsContentState() throws Exception {
        when(learningContentService.getStagesForDiscussion(41L)).thenReturn(stages(41L));

        mockMvc.perform(get(URL).session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].stageCode").value("PRE_TEST"))
            .andExpect(jsonPath("$[0].displayOrder").value(1))
            .andExpect(jsonPath("$[0].hasContent").value(false))
            .andExpect(jsonPath("$[1].stageCode").value("DEFINITIONS"))
            .andExpect(jsonPath("$[4].stageCode").value("VIDEO_LESSONS"))
            .andExpect(jsonPath("$[6].stageCode").value("POST_TEST"))
            .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void updatesExistingStageContentAndDoesNotCreateAnotherStageRow() throws Exception {
        LearningStageContent existing = stage(41L, 1, "PRE_TEST", "Pre-test", "");
        LearningStageContent updated = stage(41L, 1, "PRE_TEST", "Pre-test instructions", "Read this first.");
        when(learningContentService.getStagesForDiscussion(41L)).thenReturn(stages(41L));
        when(learningContentService.updateStageContent(1L, "Pre-test instructions", "Read this first.", (short) 1))
            .thenReturn(updated);

        mockMvc.perform(put(URL + "/PRE_TEST").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Pre-test instructions\",\"contentBody\":\"Read this first.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stageCode").value("PRE_TEST"))
            .andExpect(jsonPath("$.contentBody").value("Read this first."))
            .andExpect(jsonPath("$.hasContent").value(true));

        verify(learningContentService).updateStageContent(existing.getId(), "Pre-test instructions", "Read this first.", (short) 1);
        verify(learningContentService, never()).createStageContent(any(), any(), any(), any(), anyShort());
    }

    @Test
    void acceptsBodyWithTextImageAndVideoReferencesWithoutChangingStageIdentity() throws Exception {
        String markdown = "Text\n\n![Board](https://example.test/board.png)\n\n[Watch clip](https://example.test/video)";
        LearningStageContent updated = stage(41L, 5, "VIDEO_LESSONS", "Video Lessons", markdown);
        when(learningContentService.getStagesForDiscussion(41L)).thenReturn(stages(41L));
        when(learningContentService.updateStageContent(5L, "Video Lessons", markdown, (short) 5)).thenReturn(updated);

        mockMvc.perform(put(URL + "/VIDEO_LESSONS").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Video Lessons\",\"contentBody\":\"Text\\n\\n![Board](https://example.test/board.png)\\n\\n[Watch clip](https://example.test/video)\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contentBody").value(markdown));
    }

    @Test
    void rejectsMismatchedCompetencyAndUnknownStageCodes() throws Exception {
        when(courseManagementService.getDiscussionById(41L)).thenReturn(discussion(41L, 2L));
        mockMvc.perform(get(URL).session(adminSession)).andExpect(status().isNotFound());
        verify(learningContentService, never()).getStagesForDiscussion(41L);

        when(courseManagementService.getDiscussionById(41L)).thenReturn(discussion(41L, 1L));
        mockMvc.perform(put(URL + "/NOT_A_STAGE").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Invalid\",\"contentBody\":\"body\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingTitleOrBodyAndReportsMissingDiscussion() throws Exception {
        mockMvc.perform(put(URL + "/PRE_TEST").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \",\"contentBody\":null}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());

        when(courseManagementService.getDiscussionById(404L)).thenThrow(new EntityNotFoundException("missing"));
        mockMvc.perform(get("/api/admin/competencies/1/discussions/404/learning-stages").session(adminSession))
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

    private List<LearningStageContent> stages(Long discussionId) {
        List<String> titles = List.of("Pre-test", "Definitions", "Explanation", "Examples", "Video Lessons", "Activity", "Post-test");
        return java.util.stream.IntStream.range(0, STAGE_CODES.size())
            .mapToObj(index -> stage(discussionId, (short) (index + 1), STAGE_CODES.get(index), titles.get(index), ""))
            .toList();
    }

    private LearningStageContent stage(Long discussionId, int order, String code, String title, String body) {
        LearningStageContent item = new LearningStageContent();
        item.setId((long) order);
        item.setDiscussion(discussion(discussionId, 1L));
        item.setStageCode(code);
        item.setTitle(title);
        item.setContentBody(body);
        item.setDisplayOrder((short) order);
        item.setActive(true);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(item.getCreatedAt());
        return item;
    }

    private Discussion discussion(Long id, Long competencyId) {
        Competency competency = new Competency();
        competency.setId(competencyId);
        competency.setCode(competencyId == 1L ? "COC1" : "COC2");
        Discussion discussion = new Discussion();
        discussion.setId(id);
        discussion.setCompetency(competency);
        discussion.setTitle("Discuss fundamental concepts of computer systems servicing");
        discussion.setDisplayOrder(1L);
        return discussion;
    }
}
