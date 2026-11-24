package com.cssncii.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.entity.Admin;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Topic;
import com.cssncii.repository.AdminRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.service.CourseManagementService;
import com.cssncii.service.CourseStructureInitializationService;
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

@WebMvcTest({AdminCourseStructureController.class, AdminCourseStructureInitializationController.class})
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class, AdminAuthController.class})
class AdminCourseStructureControllerTest {
    private static final String BASE_URL = "/api/admin/competencies";
    private static final String USER = "course-test-admin";
    private static final String PASSWORD = "course-test-password";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminRepository adminRepository;
    @MockitoBean private CourseManagementService courseService;
    @MockitoBean private CourseStructureInitializationService initializationService;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        Admin admin = mock(Admin.class);
        when(admin.getId()).thenReturn(23L);
        when(admin.getUsername()).thenReturn(USER);
        when(admin.getPasswordHash()).thenReturn(new BCryptPasswordEncoder().encode(PASSWORD));
        when(adminRepository.findByUsername(USER)).thenReturn(Optional.of(admin));
        adminSession = login();
    }

    @Test
    void courseStructureEndpointsRequireAdminAndCsrf() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE_URL).session(adminSession).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void initializationEndpointRequiresAdminAndCsrf() throws Exception {
        mockMvc.perform(post("/api/admin/course-structure/initialize").with(csrf()))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/course-structure/initialize").session(adminSession))
            .andExpect(status().isForbidden());

        when(initializationService.initialize()).thenReturn(
            new CourseStructureInitializationService.InitializationResult(2, 9, 24, 63, 24));
        mockMvc.perform(post("/api/admin/course-structure/initialize").session(adminSession).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.competencyCount").value(2))
            .andExpect(jsonPath("$.discussionCount").value(9))
            .andExpect(jsonPath("$.topicCount").value(24))
            .andExpect(jsonPath("$.learningStageCount").value(63));
    }

    @Test
    void listsCompetenciesAsSafeOrderedDtos() throws Exception {
        when(courseService.getAllCompetencies()).thenReturn(List.of(competency(1L, "COC1")));
        mockMvc.perform(get(BASE_URL).session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].code").value("COC1"))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void createsAndEditsCompetencyWithValidatedFields() throws Exception {
        when(courseService.createCompetency("coc2", "Hardware", "Description", 2L))
            .thenReturn(competency(2L, "COC2"));
        mockMvc.perform(post(BASE_URL).session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"coc2\",\"title\":\"Hardware\",\"description\":\"Description\",\"displayOrder\":2}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("COC2"));

        when(courseService.updateCompetency(2L, "COC2", "Hardware Updated", "", 3L))
            .thenReturn(competency(2L, "COC2"));
        mockMvc.perform(put(BASE_URL + "/2").session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"COC2\",\"title\":\"Hardware Updated\",\"description\":\"\",\"displayOrder\":3}"))
            .andExpect(status().isOk());

        mockMvc.perform(post(BASE_URL).session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"COC3\",\"title\":\"Unsupported\",\"displayOrder\":0}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createsDiscussionUnderSelectedCompetencyAndReturnsParentId() throws Exception {
        Discussion discussion = discussion(41L, 1L);
        when(courseService.createDiscussion(1L, "Networking", "Intro", 0L)).thenReturn(discussion);
        mockMvc.perform(post(BASE_URL + "/1/discussions").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Networking\",\"description\":\"Intro\",\"displayOrder\":0}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(41))
            .andExpect(jsonPath("$.competencyId").value(1));
    }

    @Test
    void rejectsDiscussionAccessThroughWrongCompetency() throws Exception {
        when(courseService.getDiscussionById(41L)).thenReturn(discussion(41L, 2L));
        mockMvc.perform(get(BASE_URL + "/1/discussions/41/topics").session(adminSession))
            .andExpect(status().isNotFound());
        verify(courseService, never()).getTopicsByDiscussion(41L);
    }

    @Test
    void editsDiscussionWithoutChangingItsCompetencyRelationship() throws Exception {
        Discussion existing = discussion(41L, 1L);
        when(courseService.getDiscussionById(41L)).thenReturn(existing);
        when(courseService.updateDiscussion(41L, 1L, "Updated Networking", "", 2L))
            .thenReturn(existing);
        mockMvc.perform(put(BASE_URL + "/1/discussions/41").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated Networking\",\"description\":\"\",\"displayOrder\":2}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.competencyId").value(1));
        verify(courseService).updateDiscussion(41L, 1L, "Updated Networking", "", 2L);
    }

    @Test
    void createsAndEditsTopicOnlyWithinSelectedDiscussion() throws Exception {
        Discussion parent = discussion(41L, 1L);
        Topic topic = topic(81L, parent);
        when(courseService.getDiscussionById(41L)).thenReturn(parent);
        when(courseService.createTopic(41L, "Network Basics", "", 0L)).thenReturn(topic);
        mockMvc.perform(post(BASE_URL + "/1/discussions/41/topics").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Network Basics\",\"description\":\"\",\"displayOrder\":0}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.discussionId").value(41));

        when(courseService.getDiscussionById(41L)).thenReturn(parent);
        when(courseService.getTopicById(81L)).thenReturn(topic);
        when(courseService.updateTopic(81L, 41L, "Network Basics Updated", "", 1L)).thenReturn(topic);
        mockMvc.perform(put(BASE_URL + "/1/discussions/41/topics/81").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Network Basics Updated\",\"description\":\"\",\"displayOrder\":1}"))
            .andExpect(status().isOk());

        when(courseService.setTopicActive(81L, false)).thenReturn(topic);
        mockMvc.perform(patch(BASE_URL + "/1/discussions/41/topics/81/status").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
            .andExpect(status().isOk());

        when(courseService.getTopicById(81L)).thenReturn(topic(81L, discussion(42L, 1L)));
        mockMvc.perform(patch(BASE_URL + "/1/discussions/41/topics/81/status").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
            .andExpect(status().isNotFound());
        verify(courseService, times(1)).setTopicActive(81L, false);
    }

    @Test
    void forwardsActiveAndInactiveUpdatesAndReturnsNotFoundForMissingRecords() throws Exception {
        when(courseService.setCompetencyActive(1L, false)).thenReturn(competency(1L, "COC1", false));
        mockMvc.perform(patch(BASE_URL + "/1/status").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        when(courseService.setDiscussionActive(41L, true)).thenReturn(discussion(41L, 1L));
        when(courseService.getDiscussionById(41L)).thenReturn(discussion(41L, 1L));
        mockMvc.perform(patch(BASE_URL + "/1/discussions/41/status").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
            .andExpect(status().isOk());

        when(courseService.setCompetencyActive(404L, false)).thenThrow(new EntityNotFoundException("missing"));
        mockMvc.perform(patch(BASE_URL + "/404/status").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void reportsConflictingDisplayOrderAsConflict() throws Exception {
        when(courseService.createDiscussion(1L, "Duplicate", null, 1L))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("conflict"));
        mockMvc.perform(post(BASE_URL + "/1/discussions").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Duplicate\",\"displayOrder\":1}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
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

    private Competency competency(Long id, String code) { return competency(id, code, true); }
    private Competency competency(Long id, String code, boolean active) {
        Competency item = new Competency();
        item.setId(id); item.setCode(code); item.setTitle("Course " + code); item.setDisplayOrder(0L);
        item.setActive(active); item.setCreatedAt(LocalDateTime.now()); item.setUpdatedAt(item.getCreatedAt());
        return item;
    }
    private Discussion discussion(Long id, Long competencyId) {
        Discussion item = new Discussion(); item.setId(id); item.setCompetency(competency(competencyId, "COC" + competencyId));
        item.setTitle("Networking"); item.setDisplayOrder(0L); item.setActive(true);
        item.setCreatedAt(LocalDateTime.now()); item.setUpdatedAt(item.getCreatedAt()); return item;
    }
    private Topic topic(Long id, Discussion discussion) {
        Topic item = new Topic(); item.setId(id); item.setDiscussion(discussion); item.setTitle("Topic");
        item.setDisplayOrder(0L); item.setActive(true); item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(item.getCreatedAt()); return item;
    }
}
