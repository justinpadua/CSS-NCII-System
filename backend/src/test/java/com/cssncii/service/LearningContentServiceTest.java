package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cssncii.entity.Discussion;
import com.cssncii.entity.LearningStageContent;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.LearningStageContentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

class LearningContentServiceTest {
    private static final List<String> STAGE_CODES = List.of(
        "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"
    );
    private static final List<String> STAGE_TITLES = List.of(
        "Pre-test", "Definitions", "Explanation", "Examples", "Video Lessons", "Activity", "Post-test"
    );

    @Test
    void initializesAllSevenStagesOnceAndPreservesThemOnRepeatedInitialization() {
        LearningStageContentRepository contentRepository = mock(LearningStageContentRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        Discussion discussion = new Discussion();
        discussion.setId(41L);
        List<LearningStageContent> savedRecords = new ArrayList<>();

        when(discussionRepository.findById(41L)).thenReturn(Optional.of(discussion));
        when(contentRepository.findByDiscussion_IdOrderByDisplayOrderAsc(41L))
            .thenAnswer(invocation -> new ArrayList<>(savedRecords));
        when(contentRepository.save(any(LearningStageContent.class))).thenAnswer(invocation -> {
            LearningStageContent content = invocation.getArgument(0);
            content.setId((long) savedRecords.size() + 1);
            savedRecords.add(content);
            return content;
        });
        LearningContentService service = new LearningContentService(contentRepository, discussionRepository);

        List<LearningStageContent> firstInitialization = service.initializeDiscussionStages(41L);
        List<LearningStageContent> repeatedInitialization = service.initializeDiscussionStages(41L);

        assertEquals(7, firstInitialization.size());
        assertEquals(7, repeatedInitialization.size());
        assertEquals(STAGE_CODES, repeatedInitialization.stream()
            .map(LearningStageContent::getStageCode).toList());
        assertEquals(STAGE_TITLES, repeatedInitialization.stream()
            .map(LearningStageContent::getTitle).toList());
        assertTrue(repeatedInitialization.stream().allMatch(content -> content.getContentBody().isEmpty()));
        verify(contentRepository, times(7)).save(any(LearningStageContent.class));
    }
}
