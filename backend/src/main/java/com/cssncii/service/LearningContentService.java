package com.cssncii.service;

import com.cssncii.entity.Discussion;
import com.cssncii.entity.LearningStageContent;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.LearningStageContentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LearningContentService {
    private static final List<String> STAGE_CODES = List.of(
        "PRE_TEST",
        "DEFINITIONS",
        "EXPLANATION",
        "EXAMPLES",
        "VIDEO_LESSONS",
        "ACTIVITY",
        "POST_TEST"
    );
    private static final Map<String, String> STAGE_TITLES = Map.of(
        "PRE_TEST", "Pre-test",
        "DEFINITIONS", "Definitions",
        "EXPLANATION", "Explanation",
        "EXAMPLES", "Examples",
        "VIDEO_LESSONS", "Video Lessons",
        "ACTIVITY", "Activity",
        "POST_TEST", "Post-test"
    );

    private final LearningStageContentRepository contentRepository;
    private final DiscussionRepository discussionRepository;

    public LearningContentService(
        LearningStageContentRepository contentRepository,
        DiscussionRepository discussionRepository
    ) {
        this.contentRepository = contentRepository;
        this.discussionRepository = discussionRepository;
    }

    /** Creates any missing empty stage slots, preserving existing content. */
    @Transactional
    public List<LearningStageContent> initializeDiscussionStages(Long discussionId) {
        Discussion discussion = findDiscussion(discussionId);
        List<LearningStageContent> existing =
            contentRepository.findByDiscussion_IdOrderByDisplayOrderAsc(discussionId);
        Map<String, LearningStageContent> byCode = existing.stream()
            .collect(java.util.stream.Collectors.toMap(
                LearningStageContent::getStageCode,
                content -> content,
                (first, duplicate) -> first
            ));

        for (String stageCode : STAGE_CODES) {
            if (!byCode.containsKey(stageCode)) {
                short displayOrder = stageOrder(stageCode);
                ensureOrderAvailable(existing, displayOrder, null);
                LearningStageContent content = new LearningStageContent();
                content.setDiscussion(discussion);
                content.setStageCode(stageCode);
                content.setTitle(STAGE_TITLES.get(stageCode));
                content.setContentBody("");
                content.setDisplayOrder(displayOrder);
                content.setActive(true);
                LocalDateTime now = utcNow();
                content.setCreatedAt(now);
                content.setUpdatedAt(now);
                existing.add(contentRepository.save(content));
            }
        }
        return contentRepository.findByDiscussion_IdOrderByDisplayOrderAsc(discussionId);
    }

    @Transactional
    public LearningStageContent createStageContent(
        Long discussionId, String stageCode, String title, String contentBody, short displayOrder
    ) {
        Discussion discussion = findDiscussion(discussionId);
        String normalizedCode = normalizeStageCode(stageCode);
        short expectedOrder = stageOrder(normalizedCode);
        if (displayOrder != expectedOrder) {
            throw new IllegalArgumentException("Display order must match the standard stage order.");
        }

        List<LearningStageContent> existing =
            contentRepository.findByDiscussion_IdOrderByDisplayOrderAsc(discussionId);
        if (existing.stream().anyMatch(content -> content.getStageCode().equals(normalizedCode))) {
            throw new IllegalArgumentException("This Discussion already has content for stage " + normalizedCode + ".");
        }
        ensureOrderAvailable(existing, displayOrder, null);

        LocalDateTime now = utcNow();
        LearningStageContent content = new LearningStageContent();
        content.setDiscussion(discussion);
        content.setStageCode(normalizedCode);
        content.setTitle(requiredText("title", title, 200));
        content.setContentBody(requiredBody(contentBody));
        content.setDisplayOrder(displayOrder);
        content.setActive(true);
        content.setCreatedAt(now);
        content.setUpdatedAt(now);
        return contentRepository.save(content);
    }

    public List<LearningStageContent> getStagesForDiscussion(Long discussionId) {
        findDiscussion(discussionId);
        List<LearningStageContent> stages =
            contentRepository.findByDiscussion_IdOrderByDisplayOrderAsc(discussionId);
        validateCompleteStageSet(stages);
        return stages;
    }

    public LearningStageContent getStageContentById(Long contentId) {
        return findContent(contentId);
    }

    @Transactional
    public LearningStageContent updateStageContent(
        Long contentId, String title, String contentBody, short displayOrder
    ) {
        LearningStageContent content = findContent(contentId);
        short expectedOrder = stageOrder(content.getStageCode());
        if (displayOrder != expectedOrder) {
            throw new IllegalArgumentException("Display order must match the standard stage order.");
        }

        content.setTitle(requiredText("title", title, 200));
        content.setContentBody(requiredBody(contentBody));
        content.setDisplayOrder(displayOrder);
        content.setUpdatedAt(utcNow());
        return contentRepository.save(content);
    }

    private Discussion findDiscussion(Long discussionId) {
        requireId("discussionId", discussionId);
        return discussionRepository.findById(discussionId)
            .orElseThrow(() -> new EntityNotFoundException("Discussion not found: " + discussionId));
    }

    private LearningStageContent findContent(Long contentId) {
        requireId("contentId", contentId);
        return contentRepository.findById(contentId)
            .orElseThrow(() -> new EntityNotFoundException("Learning stage content not found: " + contentId));
    }

    private void validateCompleteStageSet(List<LearningStageContent> stages) {
        if (stages.size() != STAGE_TITLES.size()) {
            throw new IllegalStateException("The Discussion must have exactly one content record for each of its seven stages.");
        }
        for (int i = 0; i < stages.size(); i++) {
            LearningStageContent stage = stages.get(i);
            if (!STAGE_TITLES.containsKey(stage.getStageCode()) || stage.getDisplayOrder() != i + 1) {
                throw new IllegalStateException("The Discussion stage content is incomplete or has an invalid order.");
            }
        }
    }

    private void ensureOrderAvailable(List<LearningStageContent> existing, short displayOrder, Long excludedId) {
        boolean duplicate = existing.stream().anyMatch(content -> content.getDisplayOrder() == displayOrder
            && (excludedId == null || !excludedId.equals(content.getId())));
        if (duplicate) {
            throw new IllegalArgumentException("A stage already uses this display order in the Discussion.");
        }
    }

    private String normalizeStageCode(String stageCode) {
        if (stageCode == null || !STAGE_TITLES.containsKey(stageCode.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("stageCode must be one of the seven standard stage codes.");
        }
        return stageCode.trim().toUpperCase(Locale.ROOT);
    }

    private short stageOrder(String stageCode) {
        return (short) (STAGE_CODES.indexOf(stageCode) + 1);
    }

    private String requiredText(String fieldName, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return normalized;
    }

    private String requiredBody(String contentBody) {
        if (contentBody == null) {
            throw new IllegalArgumentException("contentBody is required.");
        }
        return contentBody;
    }

    private void requireId(String fieldName, Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive ID.");
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
