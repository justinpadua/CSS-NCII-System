package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.TopicRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.mock;

class CourseManagementServiceTest {
    @Test
    void mapsMarkdownPreviewContextToScalarsAndValidatesCompetency() {
        CompetencyRepository competencyRepository = mock(CompetencyRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        TopicRepository topicRepository = mock(TopicRepository.class);
        LearningContentService learningContentService = mock(LearningContentService.class);
        Competency competency = new Competency();
        competency.setId(3L);
        competency.setCode("COC1");
        Discussion discussion = new Discussion();
        discussion.setId(41L);
        discussion.setCompetency(competency);
        discussion.setTitle("Discuss computer systems servicing");
        discussion.setDisplayOrder(2L);
        when(discussionRepository.findById(41L)).thenReturn(Optional.of(discussion));
        CourseManagementService service = new CourseManagementService(
            competencyRepository, discussionRepository, topicRepository, learningContentService);

        var context = service.getDiscussionMarkdownContext(3L, 41L);

        assertEquals(new CourseManagementService.DiscussionMarkdownContext(
            3L, "COC1", 2L, "Discuss computer systems servicing"), context);
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
            () -> service.getDiscussionMarkdownContext(4L, 41L));
    }

    @Test
    void creatingDiscussionInitializesStagesAfterDiscussionIsSaved() {
        CompetencyRepository competencyRepository = mock(CompetencyRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        TopicRepository topicRepository = mock(TopicRepository.class);
        LearningContentService learningContentService = mock(LearningContentService.class);
        Competency competency = new Competency();
        competency.setId(3L);
        Discussion persistedDiscussion = new Discussion();
        persistedDiscussion.setId(41L);

        when(competencyRepository.findById(3L)).thenReturn(Optional.of(competency));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(3L)).thenReturn(List.of());
        when(discussionRepository.save(any(Discussion.class))).thenReturn(persistedDiscussion);
        when(learningContentService.initializeDiscussionStages(41L)).thenReturn(List.of());
        CourseManagementService service = new CourseManagementService(
            competencyRepository, discussionRepository, topicRepository, learningContentService);

        Discussion result = service.createDiscussion(3L, "Networking", null, 1L);

        assertEquals(41L, result.getId());
        InOrder order = inOrder(discussionRepository, learningContentService);
        order.verify(discussionRepository).save(any(Discussion.class));
        order.verify(learningContentService).initializeDiscussionStages(41L);
    }

    @Test
    void duplicateDiscussionOrderIsRejectedBeforeSavingOrInitializingStages() {
        CompetencyRepository competencyRepository = mock(CompetencyRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        TopicRepository topicRepository = mock(TopicRepository.class);
        LearningContentService learningContentService = mock(LearningContentService.class);
        Competency competency = new Competency();
        competency.setId(3L);
        Discussion existing = new Discussion();
        existing.setId(9L);
        existing.setDisplayOrder(1L);
        when(competencyRepository.findById(3L)).thenReturn(Optional.of(competency));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(3L)).thenReturn(List.of(existing));
        CourseManagementService service = new CourseManagementService(
            competencyRepository, discussionRepository, topicRepository, learningContentService);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createDiscussion(3L, "Second Discussion", null, 1L));

        Assertions.assertTrue(exception.getMessage().contains("display order"));
        verify(discussionRepository, never()).save(any(Discussion.class));
        verify(learningContentService, never()).initializeDiscussionStages(41L);
    }

    @Test
    void validatesCompetencyCodeAndTrimsTitles() {
        CompetencyRepository competencyRepository = mock(CompetencyRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        TopicRepository topicRepository = mock(TopicRepository.class);
        LearningContentService learningContentService = mock(LearningContentService.class);
        when(competencyRepository.findAll()).thenReturn(List.of());
        when(competencyRepository.save(any(Competency.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CourseManagementService service = new CourseManagementService(
            competencyRepository, discussionRepository, topicRepository, learningContentService);

        Competency created = service.createCompetency(" coc1 ", "  Computer Systems  ", "  Course description  ", 0L);

        assertEquals("COC1", created.getCode());
        assertEquals("Computer Systems", created.getTitle());
        assertEquals("Course description", created.getDescription());
        assertThrows(IllegalArgumentException.class,
            () -> service.createCompetency("COC3", "Unsupported", null, 1L));
    }
}
