package com.cssncii.service;

import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Topic;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.TopicRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseManagementService {
    private static final long MAX_INT_UNSIGNED = 4_294_967_295L;

    private final CompetencyRepository competencyRepository;
    private final DiscussionRepository discussionRepository;
    private final TopicRepository topicRepository;
    private final LearningContentService learningContentService;

    public CourseManagementService(
        CompetencyRepository competencyRepository,
        DiscussionRepository discussionRepository,
        TopicRepository topicRepository,
        LearningContentService learningContentService
    ) {
        this.competencyRepository = competencyRepository;
        this.discussionRepository = discussionRepository;
        this.topicRepository = topicRepository;
        this.learningContentService = learningContentService;
    }

    @Transactional
    public Competency createCompetency(String code, String title, String description, long displayOrder) {
        String normalizedCode = normalizeCompetencyCode(code);
        String normalizedTitle = requiredText("title", title, 200);
        validateDisplayOrder(displayOrder);
        ensureCompetencyCodeAvailable(normalizedCode, null);

        LocalDateTime now = utcNow();
        Competency competency = new Competency();
        competency.setCode(normalizedCode);
        competency.setTitle(normalizedTitle);
        competency.setDescription(optionalText(description));
        competency.setDisplayOrder(displayOrder);
        competency.setActive(true);
        competency.setCreatedAt(now);
        competency.setUpdatedAt(now);
        return competencyRepository.save(competency);
    }

    public List<Competency> getAllCompetencies() {
        return competencyRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder"));
    }

    public Competency getCompetencyById(Long competencyId) {
        return findCompetency(competencyId);
    }

    @Transactional
    public Competency updateCompetency(
        Long competencyId, String code, String title, String description, long displayOrder
    ) {
        Competency competency = findCompetency(competencyId);
        String normalizedCode = normalizeCompetencyCode(code);
        String normalizedTitle = requiredText("title", title, 200);
        validateDisplayOrder(displayOrder);
        ensureCompetencyCodeAvailable(normalizedCode, competencyId);

        competency.setCode(normalizedCode);
        competency.setTitle(normalizedTitle);
        competency.setDescription(optionalText(description));
        competency.setDisplayOrder(displayOrder);
        competency.setUpdatedAt(utcNow());
        return competencyRepository.save(competency);
    }

    @Transactional
    public Competency setCompetencyActive(Long competencyId, boolean active) {
        Competency competency = findCompetency(competencyId);
        competency.setActive(active);
        competency.setUpdatedAt(utcNow());
        return competencyRepository.save(competency);
    }

    @Transactional
    public Discussion createDiscussion(
        Long competencyId, String title, String description, long displayOrder
    ) {
        Competency competency = findCompetency(competencyId);
        String normalizedTitle = requiredText("title", title, 250);
        validateDisplayOrder(displayOrder);
        ensureDiscussionOrderAvailable(competencyId, displayOrder, null);

        LocalDateTime now = utcNow();
        Discussion discussion = new Discussion();
        discussion.setCompetency(competency);
        discussion.setTitle(normalizedTitle);
        discussion.setDescription(optionalText(description));
        discussion.setDisplayOrder(displayOrder);
        discussion.setActive(true);
        discussion.setCreatedAt(now);
        discussion.setUpdatedAt(now);
        Discussion savedDiscussion = discussionRepository.save(discussion);
        learningContentService.initializeDiscussionStages(savedDiscussion.getId());
        return savedDiscussion;
    }

    public List<Discussion> getDiscussionsByCompetency(Long competencyId) {
        findCompetency(competencyId);
        return discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(competencyId);
    }

    public Discussion getDiscussionById(Long discussionId) {
        return findDiscussion(discussionId);
    }

    /** Loads and maps the fields needed by the Markdown source preview before the read transaction closes. */
    public DiscussionMarkdownContext getDiscussionMarkdownContext(Long competencyId, Long discussionId) {
        Discussion discussion = findDiscussion(discussionId);
        Competency competency = discussion.getCompetency();
        if (competency == null || !competency.getId().equals(competencyId)) {
            throw new EntityNotFoundException("Discussion not found in competency.");
        }
        return new DiscussionMarkdownContext(
            competency.getId(), competency.getCode(), discussion.getDisplayOrder(), discussion.getTitle());
    }

    @Transactional
    public Discussion updateDiscussion(
        Long discussionId, Long competencyId, String title, String description, long displayOrder
    ) {
        Discussion discussion = findDiscussion(discussionId);
        Competency competency = findCompetency(competencyId);
        String normalizedTitle = requiredText("title", title, 250);
        validateDisplayOrder(displayOrder);
        ensureDiscussionOrderAvailable(competencyId, displayOrder, discussionId);

        discussion.setCompetency(competency);
        discussion.setTitle(normalizedTitle);
        discussion.setDescription(optionalText(description));
        discussion.setDisplayOrder(displayOrder);
        discussion.setUpdatedAt(utcNow());
        return discussionRepository.save(discussion);
    }

    @Transactional
    public Discussion setDiscussionActive(Long discussionId, boolean active) {
        Discussion discussion = findDiscussion(discussionId);
        discussion.setActive(active);
        discussion.setUpdatedAt(utcNow());
        return discussionRepository.save(discussion);
    }

    @Transactional
    public Topic createTopic(Long discussionId, String title, String description, long displayOrder) {
        Discussion discussion = findDiscussion(discussionId);
        String normalizedTitle = requiredText("title", title, 250);
        validateDisplayOrder(displayOrder);
        ensureTopicOrderAvailable(discussionId, displayOrder, null);

        LocalDateTime now = utcNow();
        Topic topic = new Topic();
        topic.setDiscussion(discussion);
        topic.setTitle(normalizedTitle);
        topic.setDescription(optionalText(description));
        topic.setDisplayOrder(displayOrder);
        topic.setActive(true);
        topic.setCreatedAt(now);
        topic.setUpdatedAt(now);
        return topicRepository.save(topic);
    }

    public List<Topic> getTopicsByDiscussion(Long discussionId) {
        findDiscussion(discussionId);
        return topicRepository.findByDiscussion_IdOrderByDisplayOrderAsc(discussionId);
    }

    public Topic getTopicById(Long topicId) {
        return findTopic(topicId);
    }

    @Transactional
    public Topic updateTopic(
        Long topicId, Long discussionId, String title, String description, long displayOrder
    ) {
        Topic topic = findTopic(topicId);
        Discussion discussion = findDiscussion(discussionId);
        String normalizedTitle = requiredText("title", title, 250);
        validateDisplayOrder(displayOrder);
        ensureTopicOrderAvailable(discussionId, displayOrder, topicId);

        topic.setDiscussion(discussion);
        topic.setTitle(normalizedTitle);
        topic.setDescription(optionalText(description));
        topic.setDisplayOrder(displayOrder);
        topic.setUpdatedAt(utcNow());
        return topicRepository.save(topic);
    }

    @Transactional
    public Topic setTopicActive(Long topicId, boolean active) {
        Topic topic = findTopic(topicId);
        topic.setActive(active);
        topic.setUpdatedAt(utcNow());
        return topicRepository.save(topic);
    }

    private Competency findCompetency(Long competencyId) {
        requireId("competencyId", competencyId);
        return competencyRepository.findById(competencyId)
            .orElseThrow(() -> new EntityNotFoundException("Competency not found: " + competencyId));
    }

    private Discussion findDiscussion(Long discussionId) {
        requireId("discussionId", discussionId);
        return discussionRepository.findById(discussionId)
            .orElseThrow(() -> new EntityNotFoundException("Discussion not found: " + discussionId));
    }

    private Topic findTopic(Long topicId) {
        requireId("topicId", topicId);
        return topicRepository.findById(topicId)
            .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + topicId));
    }

    private void ensureCompetencyCodeAvailable(String code, Long excludedId) {
        boolean duplicate = competencyRepository.findAll().stream()
            .anyMatch(existing -> existing.getCode().equalsIgnoreCase(code)
                && (excludedId == null || !excludedId.equals(existing.getId())));
        if (duplicate) {
            throw new IllegalArgumentException("Competency code already exists: " + code);
        }
    }

    private void ensureDiscussionOrderAvailable(Long competencyId, long displayOrder, Long excludedId) {
        boolean duplicate = discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(competencyId).stream()
            .anyMatch(existing -> existing.getDisplayOrder() == displayOrder
                && (excludedId == null || !excludedId.equals(existing.getId())));
        if (duplicate) {
            throw new IllegalArgumentException("A Discussion already uses this display order in the competency.");
        }
    }

    private void ensureTopicOrderAvailable(Long discussionId, long displayOrder, Long excludedId) {
        boolean duplicate = topicRepository.findByDiscussion_IdOrderByDisplayOrderAsc(discussionId).stream()
            .anyMatch(existing -> existing.getDisplayOrder() == displayOrder
                && (excludedId == null || !excludedId.equals(existing.getId())));
        if (duplicate) {
            throw new IllegalArgumentException("A topic already uses this display order in the Discussion.");
        }
    }

    private String normalizeCompetencyCode(String code) {
        String normalized = requiredText("code", code, 20).toUpperCase(Locale.ROOT);
        if (!normalized.equals("COC1") && !normalized.equals("COC2")) {
            throw new IllegalArgumentException("Competency code must be COC1 or COC2.");
        }
        return normalized;
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

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateDisplayOrder(long displayOrder) {
        if (displayOrder < 0 || displayOrder > MAX_INT_UNSIGNED) {
            throw new IllegalArgumentException("Display order must fit an unsigned MySQL INT.");
        }
    }

    private void requireId(String fieldName, Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive ID.");
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public record DiscussionMarkdownContext(
        Long competencyId, String competencyCode, long discussionOrder, String discussionTitle
    ) {}
}
