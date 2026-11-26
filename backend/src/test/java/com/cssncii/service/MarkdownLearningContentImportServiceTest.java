package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

class MarkdownLearningContentImportServiceTest {
    private final MarkdownLearningContentImportService service =
        new MarkdownLearningContentImportService(new DefaultResourceLoader());

    @Test
    void mapsDiscussionAndStageToThePackagedSourceAndRemovesAssessmentItemsAndAnswerKey() {
        var preview = service.preview("COC1", 1, "Discuss fundamental concepts of computer systems servicing", "PRE_TEST", "");

        assertEquals("learning-content/COC-1/Lesson-01/01-Pretest.md", preview.sourcePath());
        assertTrue(preview.sourceAvailable());
        assertTrue(preview.canLoadIntoEditor());
        assertTrue(preview.assessmentItemsRemoved());
        assertTrue(preview.stageBodyPreview().contains("Directions:"));
        assertTrue(preview.stageBodyPreview().contains("COC 1 — Lesson 1"));
        assertTrue(preview.stageBodyPreview().contains("LESSON 1 – PRE-TEST"));
        assertFalse(preview.stageBodyPreview().contains("â€”"));
        assertFalse(preview.stageBodyPreview().contains("â€“"));
        assertFalse(preview.stageBodyPreview().contains("Which of the following is an input device?"));
        assertFalse(preview.stageBodyPreview().contains("Answer Key"));
    }

    @Test
    void neverOffersToReplaceExistingAdminContentAndRecognizesAnAlreadyPresentSource() {
        var title = "Discuss elements of computer systems";
        var first = service.preview("COC1", 2, title, "DEFINITIONS", "");
        assertTrue(first.canLoadIntoEditor());

        var duplicate = service.preview("COC1", 2, title, "DEFINITIONS", first.stageBodyPreview());
        assertTrue(duplicate.alreadyPresent());
        assertFalse(duplicate.canLoadIntoEditor());

        var adminEdited = service.preview("COC1", 2, title, "DEFINITIONS", "Admin-edited material");
        assertTrue(adminEdited.existingContentPreserved());
        assertFalse(adminEdited.canLoadIntoEditor());
    }

    @Test
    void completedActivitySourceIsAvailableLoadableAndContainsActivityContent() {
        var preview = service.preview("COC2", 1, "Discuss the concepts of computer network", "ACTIVITY", "");

        assertEquals("learning-content/COC-2/Lesson-01/06-Activity.md", preview.sourcePath());
        assertTrue(preview.sourceAvailable());
        assertTrue(preview.canLoadIntoEditor());
        assertTrue(preview.stageBodyPreview().contains("Activity: Cable It Right"));
        assertTrue(preview.stageBodyPreview().contains("Part E – Troubleshoot It"));
        assertTrue(preview.stageBodyPreview().contains("Expected output:"));
    }

    @Test
    void explicitMissingContentMarkerIsNotMadeImportable() {
        String sourcePath = "learning-content/COC-2/Lesson-01/06-Activity.md";
        String placeholder = "# Activity\n\nMISSING / NOT SEPARATELY IDENTIFIED IN THE SOURCE\n";
        ResourceLoader loader = mock(ResourceLoader.class);
        when(loader.getResource("classpath:" + sourcePath)).thenReturn(
            new ByteArrayResource(placeholder.getBytes(StandardCharsets.UTF_8)));
        var markerService = new MarkdownLearningContentImportService(loader);

        var preview = markerService.preview("COC2", 1, "Discuss the concepts of computer network", "ACTIVITY", "");

        assertFalse(preview.sourceAvailable());
        assertFalse(preview.canLoadIntoEditor());
        assertTrue(preview.message().contains("marks this stage as missing"));
    }
}
