package com.cssncii.controller;

import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.dto.LearningContentDtos.StageContentRequest;
import com.cssncii.dto.LearningContentDtos.StageContentResponse;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.LearningStageContent;
import com.cssncii.service.CourseManagementService;
import com.cssncii.service.LearningContentService;
import com.cssncii.service.MarkdownLearningContentImportService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/competencies/{competencyId}/discussions/{discussionId}/learning-stages")
public class AdminLearningContentController {
    private static final List<String> STAGE_CODES = List.of(
        "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"
    );

    private final CourseManagementService courseManagementService;
    private final LearningContentService learningContentService;
    private final MarkdownLearningContentImportService markdownImportService;

    public AdminLearningContentController(
        CourseManagementService courseManagementService,
        LearningContentService learningContentService,
        MarkdownLearningContentImportService markdownImportService
    ) {
        this.courseManagementService = courseManagementService;
        this.learningContentService = learningContentService;
        this.markdownImportService = markdownImportService;
    }

    @GetMapping
    public List<StageContentResponse> getDiscussionStages(
        @PathVariable Long competencyId,
        @PathVariable Long discussionId
    ) {
        requireDiscussionBelongsTo(competencyId, discussionId);
        return learningContentService.getStagesForDiscussion(discussionId).stream()
            .map(this::toResponse).toList();
    }

    @GetMapping("/{stageCode}/source-preview")
    public MarkdownLearningContentImportService.SourcePreview previewMarkdownSource(
        @PathVariable Long competencyId,
        @PathVariable Long discussionId,
        @PathVariable String stageCode
    ) {
        CourseManagementService.DiscussionMarkdownContext discussion =
            courseManagementService.getDiscussionMarkdownContext(competencyId, discussionId);
        String normalizedCode = stageCode.trim().toUpperCase(Locale.ROOT);
        LearningStageContent stage = learningContentService.getStagesForDiscussion(discussionId).stream()
            .filter(item -> item.getStageCode().equals(normalizedCode))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Learning stage content not found."));
        return markdownImportService.preview(discussion.competencyCode(), discussion.discussionOrder(),
            discussion.discussionTitle(), normalizedCode, stage.getContentBody());
    }

    @PutMapping("/{stageCode}")
    public StageContentResponse updateStageContent(
        @PathVariable Long competencyId,
        @PathVariable Long discussionId,
        @PathVariable String stageCode,
        @Valid @RequestBody StageContentRequest request
    ) {
        requireDiscussionBelongsTo(competencyId, discussionId);
        String normalizedCode = stageCode.trim().toUpperCase(Locale.ROOT);
        if (!STAGE_CODES.contains(normalizedCode)) {
            throw new IllegalArgumentException("stageCode must be one of the seven standard stage codes.");
        }

        LearningStageContent existing = learningContentService.getStagesForDiscussion(discussionId).stream()
            .filter(stage -> stage.getStageCode().equals(normalizedCode))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Learning stage content not found."));
        LearningStageContent updated = learningContentService.updateStageContent(
            existing.getId(), request.title(), request.contentBody(), existing.getDisplayOrder(),
            request.youtubeVideoUrls());
        return toResponse(updated);
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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> structureConflict(IllegalStateException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Discussion or learning stage was not found.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> dataConflict(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "This learning stage conflicts with an existing record.");
    }

    private Discussion requireDiscussionBelongsTo(Long competencyId, Long discussionId) {
        Discussion discussion = courseManagementService.getDiscussionById(discussionId);
        if (discussion.getCompetency() == null || !discussion.getCompetency().getId().equals(competencyId)) {
            throw new EntityNotFoundException("Discussion not found in competency.");
        }
        return discussion;
    }

    private StageContentResponse toResponse(LearningStageContent stage) {
        String body = stage.getContentBody();
        List<String> youtubeVideoUrls = stage.getYoutubeVideoUrls();
        return new StageContentResponse(stage.getId(), stage.getDiscussion().getId(), stage.getStageCode(),
            stage.getTitle(), body, youtubeVideoUrls, stage.getDisplayOrder(), stage.isActive(),
            body != null && !body.isBlank() || !youtubeVideoUrls.isEmpty());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(message == null
            ? "Request could not be completed." : message));
    }
}
