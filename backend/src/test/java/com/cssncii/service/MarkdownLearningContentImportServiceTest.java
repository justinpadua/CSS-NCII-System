package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

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
    void missingSourcePlaceholderIsNotMadeImportable() {
        var preview = service.preview("COC2", 1, "Discuss the concepts of computer network", "ACTIVITY", "");

        assertEquals("learning-content/COC-2/Lesson-01/06-Activity.md", preview.sourcePath());
        assertFalse(preview.sourceAvailable());
        assertFalse(preview.canLoadIntoEditor());
        assertTrue(preview.message().contains("marks this stage as missing"));
    }
}
