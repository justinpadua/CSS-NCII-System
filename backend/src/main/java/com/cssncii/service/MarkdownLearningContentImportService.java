package com.cssncii.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/** Reads packaged source Markdown for Admin review; it never writes or publishes stage content. */
@Service
public class MarkdownLearningContentImportService {
    private static final List<String> STAGE_FILES = List.of(
        "01-Pretest.md", "02-Definition-of-Terms.md", "03-Discussion.md", "04-Examples.md",
        "05-Video-Lessons.md", "06-Activity.md", "07-Posttest.md"
    );
    private static final List<String> COC1_DISCUSSIONS = List.of(
        "Discuss fundamental concepts of computer systems servicing",
        "Discuss elements of computer systems",
        "Perform disassembly and assembly of computer hardware",
        "Create portable bootable device",
        "Perform installation of system software and application software",
        "Perform computer testing and documentation"
    );
    private static final List<String> COC2_DISCUSSIONS = List.of(
        "Discuss the concepts of computer network",
        "Perform setting up of computer network",
        "Perform network testing and documentation"
    );
    private static final Pattern QUESTION_LINE = Pattern.compile("^\\s*(?:\\d{1,2}[.)]\\s+|Q\\d+[.:]\\s*)", Pattern.CASE_INSENSITIVE);
    private static final String MISSING_MARKER = "MISSING / NOT SEPARATELY IDENTIFIED IN THE SOURCE";

    private final ResourceLoader resourceLoader;

    public MarkdownLearningContentImportService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public SourcePreview preview(
        String competencyCode, long discussionOrder, String discussionTitle, String stageCode, String existingContent
    ) {
        String code = competencyCode == null ? "" : competencyCode.trim().toUpperCase(Locale.ROOT);
        if (!code.equals("COC1") && !code.equals("COC2")) {
            throw new IllegalArgumentException("Only COC1 and COC2 Markdown sources are available.");
        }
        if (discussionOrder < 1 || discussionOrder > (code.equals("COC1") ? 6 : 3)) {
            throw new IllegalArgumentException("The Discussion order is outside the documented course structure.");
        }
        String expectedDiscussionTitle = (code.equals("COC1") ? COC1_DISCUSSIONS : COC2_DISCUSSIONS)
            .get((int) discussionOrder - 1);
        if (!expectedDiscussionTitle.equals(discussionTitle)) {
            throw new IllegalStateException("The Discussion title/order does not match the documented Markdown mapping. No source content was loaded.");
        }
        String normalizedStage = stageCode == null ? "" : stageCode.trim().toUpperCase(Locale.ROOT);
        int stageIndex = StudentLearningStage.indexOf(normalizedStage);
        if (stageIndex < 0) {
            throw new IllegalArgumentException("stageCode must be one of the seven canonical stages.");
        }

        String sourcePath = "learning-content/" + code.replace("COC", "COC-")
            + "/Lesson-%02d/".formatted(discussionOrder) + STAGE_FILES.get(stageIndex);
        Resource resource = resourceLoader.getResource("classpath:" + sourcePath);
        if (!resource.exists()) {
            return unavailable(sourcePath, "The source Markdown file is missing. No content was created.");
        }

        String source;
        try (InputStream stream = resource.getInputStream()) {
            source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("The Markdown source could not be read for Admin review.", exception);
        }

        if (source.isBlank() || source.toUpperCase(Locale.ROOT).contains(MISSING_MARKER)) {
            return unavailable(sourcePath, source.isBlank()
                ? "The source Markdown file is empty. No content was created."
                : "The source explicitly marks this stage as missing. No content was created.");
        }

        boolean assessmentStage = normalizedStage.equals("PRE_TEST") || normalizedStage.equals("POST_TEST");
        SafeStageBody candidate = assessmentStage ? removeAssessmentItems(source) : new SafeStageBody(source, false);
        boolean hasExisting = existingContent != null && !existingContent.isBlank();
        boolean alreadyPresent = hasExisting && existingContent.equals(candidate.body());
        String message;
        if (alreadyPresent) {
            message = "This source is already present in the stage. No duplicate import is needed.";
        } else if (hasExisting) {
            message = "This stage already has content. The source is preview-only to protect Admin-edited content.";
        } else if (!candidate.body().isBlank()) {
            message = candidate.assessmentItemsRemoved()
                ? "Preview only: assessment questions and answer keys are excluded. Enter questions through Assessment Management."
                : "Review this source, then choose Load into editor. It is not saved or published by this preview.";
        } else {
            message = "No safe learning-stage text remains after excluding assessment material.";
        }
        return new SourcePreview(sourcePath, true, !hasExisting && !candidate.body().isBlank(), alreadyPresent,
            hasExisting, candidate.assessmentItemsRemoved(), candidate.body(), message);
    }

    private SafeStageBody removeAssessmentItems(String source) {
        String[] lines = source.split("\\R", -1);
        List<String> safe = new ArrayList<>();
        boolean removed = false;
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (QUESTION_LINE.matcher(line).find() || lower.contains("answer key") || lower.contains("correct answer:")) {
                removed = true;
                break;
            }
            safe.add(line);
        }
        return new SafeStageBody(String.join("\n", safe).stripTrailing(), removed);
    }

    private SourcePreview unavailable(String sourcePath, String message) {
        return new SourcePreview(sourcePath, false, false, false, false, false, "", message);
    }

    private static final class StudentLearningStage {
        private static final List<String> CODES = List.of(
            "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"
        );

        private static int indexOf(String code) {
            return CODES.indexOf(code);
        }
    }

    private record SafeStageBody(String body, boolean assessmentItemsRemoved) {}

    public record SourcePreview(
        String sourcePath,
        boolean sourceAvailable,
        boolean canLoadIntoEditor,
        boolean alreadyPresent,
        boolean existingContentPreserved,
        boolean assessmentItemsRemoved,
        String stageBodyPreview,
        String message
    ) {}
}
