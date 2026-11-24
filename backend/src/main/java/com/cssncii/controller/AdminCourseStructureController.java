package com.cssncii.controller;

import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.dto.CourseStructureDtos.ActiveStatusRequest;
import com.cssncii.dto.CourseStructureDtos.CompetencyRequest;
import com.cssncii.dto.CourseStructureDtos.CompetencyResponse;
import com.cssncii.dto.CourseStructureDtos.DiscussionRequest;
import com.cssncii.dto.CourseStructureDtos.DiscussionResponse;
import com.cssncii.dto.CourseStructureDtos.TopicRequest;
import com.cssncii.dto.CourseStructureDtos.TopicResponse;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Topic;
import com.cssncii.service.CourseManagementService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/competencies")
public class AdminCourseStructureController {
    private final CourseManagementService courseService;

    public AdminCourseStructureController(CourseManagementService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CompetencyResponse> listCompetencies() {
        return courseService.getAllCompetencies().stream().map(this::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<CompetencyResponse> createCompetency(@Valid @RequestBody CompetencyRequest request) {
        Competency created = courseService.createCompetency(
            request.code(), request.title(), request.description(), request.displayOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{competencyId}")
    public CompetencyResponse updateCompetency(
        @PathVariable Long competencyId,
        @Valid @RequestBody CompetencyRequest request
    ) {
        return toResponse(courseService.updateCompetency(
            competencyId, request.code(), request.title(), request.description(), request.displayOrder()));
    }

    @PatchMapping("/{competencyId}/status")
    public CompetencyResponse setCompetencyStatus(
        @PathVariable Long competencyId,
        @Valid @RequestBody ActiveStatusRequest request
    ) {
        return toResponse(courseService.setCompetencyActive(competencyId, request.active()));
    }

    @GetMapping("/{competencyId}/discussions")
    public List<DiscussionResponse> listDiscussions(@PathVariable Long competencyId) {
        return courseService.getDiscussionsByCompetency(competencyId).stream().map(this::toResponse).toList();
    }

    @PostMapping("/{competencyId}/discussions")
    public ResponseEntity<DiscussionResponse> createDiscussion(
        @PathVariable Long competencyId,
        @Valid @RequestBody DiscussionRequest request
    ) {
        Discussion created = courseService.createDiscussion(
            competencyId, request.title(), request.description(), request.displayOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{competencyId}/discussions/{discussionId}")
    public DiscussionResponse updateDiscussion(
        @PathVariable Long competencyId,
        @PathVariable Long discussionId,
        @Valid @RequestBody DiscussionRequest request
    ) {
        requireDiscussionBelongsTo(competencyId, discussionId);
        return toResponse(courseService.updateDiscussion(
            discussionId, competencyId, request.title(), request.description(), request.displayOrder()));
    }

    @PatchMapping("/{competencyId}/discussions/{discussionId}/status")
    public DiscussionResponse setDiscussionStatus(
        @PathVariable Long competencyId,
        @PathVariable Long discussionId,
        @Valid @RequestBody ActiveStatusRequest request
    ) {
        requireDiscussionBelongsTo(competencyId, discussionId);
        return toResponse(courseService.setDiscussionActive(discussionId, request.active()));
    }

    @GetMapping("/{competencyId}/discussions/{discussionId}/topics")
    public List<TopicResponse> listTopics(
        @PathVariable Long competencyId,
        @PathVariable Long discussionId
    ) {
        requireDiscussionBelongsTo(competencyId, discussionId);
        return courseService.getTopicsByDiscussion(discussionId).stream().map(this::toResponse).toList();
    }

    @PostMapping("/{competencyId}/discussions/{discussionId}/topics")
    public ResponseEntity<TopicResponse> createTopic(
        @PathVariable Long competencyId,
        @PathVariable Long discussionId,
        @Valid @RequestBody TopicRequest request
    ) {
        requireDiscussionBelongsTo(competencyId, discussionId);
        Topic created = courseService.createTopic(
            discussionId, request.title(), request.description(), request.displayOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{competencyId}/discussions/{discussionId}/topics/{topicId}")
    public TopicResponse updateTopic(
        @PathVariable Long competencyId,
        @PathVariable Long discussionId,
        @PathVariable Long topicId,
        @Valid @RequestBody TopicRequest request
    ) {
        requireDiscussionBelongsTo(competencyId, discussionId);
        requireTopicBelongsTo(discussionId, topicId);
        return toResponse(courseService.updateTopic(
            topicId, discussionId, request.title(), request.description(), request.displayOrder()));
    }

    @PatchMapping("/{competencyId}/discussions/{discussionId}/topics/{topicId}/status")
    public TopicResponse setTopicStatus(
        @PathVariable Long competencyId,
        @PathVariable Long discussionId,
        @PathVariable Long topicId,
        @Valid @RequestBody ActiveStatusRequest request
    ) {
        requireDiscussionBelongsTo(competencyId, discussionId);
        requireTopicBelongsTo(discussionId, topicId);
        return toResponse(courseService.setTopicActive(topicId, request.active()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> invalidRequest(MethodArgumentNotValidException exception) {
        FieldError error = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = error == null ? "Request contains invalid fields."
            : error.getField() + " is required or invalid.";
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> invalidArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Course, Discussion, or topic record not found.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> dataConflict(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "This course record conflicts with an existing code or display order.");
    }

    private void requireDiscussionBelongsTo(Long competencyId, Long discussionId) {
        Discussion discussion = courseService.getDiscussionById(discussionId);
        if (!discussion.getCompetency().getId().equals(competencyId)) {
            throw new EntityNotFoundException("Discussion not found in competency.");
        }
    }

    private void requireTopicBelongsTo(Long discussionId, Long topicId) {
        Topic topic = courseService.getTopicById(topicId);
        if (!topic.getDiscussion().getId().equals(discussionId)) {
            throw new EntityNotFoundException("Topic not found in Discussion.");
        }
    }

    private CompetencyResponse toResponse(Competency item) {
        return new CompetencyResponse(item.getId(), item.getCode(), item.getTitle(), item.getDescription(),
            item.getDisplayOrder(), item.isActive());
    }

    private DiscussionResponse toResponse(Discussion item) {
        return new DiscussionResponse(item.getId(), item.getCompetency().getId(), item.getTitle(),
            item.getDescription(), item.getDisplayOrder(), item.isActive());
    }

    private TopicResponse toResponse(Topic item) {
        return new TopicResponse(item.getId(), item.getDiscussion().getId(), item.getTitle(),
            item.getDescription(), item.getDisplayOrder(), item.isActive());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(message == null
            ? "Request could not be completed." : message));
    }
}
