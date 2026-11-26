package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void savesOptionalYouTubeUrlsOnlyForVideoLessons() {
        LearningStageContentRepository contentRepository = mock(LearningStageContentRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        LearningStageContent videoStage = stage(5L, "VIDEO_LESSONS");
        when(contentRepository.findById(5L)).thenReturn(Optional.of(videoStage));
        when(contentRepository.save(any(LearningStageContent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LearningContentService service = new LearningContentService(contentRepository, discussionRepository);

        LearningStageContent updated = service.updateStageContent(5L, "Video Lessons", "Written notes.", (short) 5,
            List.of("https://www.youtube.com/watch?v=abcdefghijk", "https://youtu.be/ZYXWVUTSRQP"));
        List<String> savedUrls = updated.getYoutubeVideoUrls();
        service.updateStageContent(5L, "Video Lessons", "Written notes.", (short) 5, List.of());

        assertEquals(List.of("https://www.youtube.com/watch?v=abcdefghijk", "https://youtu.be/ZYXWVUTSRQP"), savedUrls);
        assertEquals(List.of(), videoStage.getYoutubeVideoUrls());
    }

    @Test
    void rejectsInvalidUrlsAndVideoUrlsOnOtherStages() {
        LearningStageContentRepository contentRepository = mock(LearningStageContentRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        when(contentRepository.findById(2L)).thenReturn(Optional.of(stage(2L, "DEFINITIONS")));
        when(contentRepository.findById(5L)).thenReturn(Optional.of(stage(5L, "VIDEO_LESSONS")));
        LearningContentService service = new LearningContentService(contentRepository, discussionRepository);

        assertThrows(IllegalArgumentException.class, () -> service.updateStageContent(5L, "Video Lessons", "",
            (short) 5, List.of("https://example.com/video")));
        assertThrows(IllegalArgumentException.class, () -> service.updateStageContent(2L, "Definitions", "",
            (short) 2, List.of("https://youtu.be/abcdefghijk")));
    }

    private LearningStageContent stage(Long id, String code) {
        LearningStageContent content = new LearningStageContent();
        content.setId(id);
        content.setStageCode(code);
        content.setTitle("Video Lessons".equals(code) ? "Video Lessons" : "Definitions");
        content.setContentBody("");
        content.setDisplayOrder((short) ("VIDEO_LESSONS".equals(code) ? 5 : 2));
        return content;
    }
}
