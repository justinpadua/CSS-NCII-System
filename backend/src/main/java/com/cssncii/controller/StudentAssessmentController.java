package com.cssncii.controller;

import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.entity.Assessment;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.security.StudentPrincipal;
import com.cssncii.service.AssessmentAttemptService;
import com.cssncii.service.GeneralizedPostTestService;
import com.cssncii.service.StudentLearningService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/assessments")
public class StudentAssessmentController {
    private final AssessmentRepository assessmentRepository;
    private final DiscussionRepository discussionRepository;
    private final StudentLearningService learningService;
    private final AssessmentAttemptService attemptService;
    private final GeneralizedPostTestService generalizedPostTestService;

    public StudentAssessmentController(
        AssessmentRepository assessmentRepository,
        DiscussionRepository discussionRepository,
        StudentLearningService learningService,
        AssessmentAttemptService attemptService,
        GeneralizedPostTestService generalizedPostTestService
    ) {
        this.assessmentRepository = assessmentRepository;
        this.discussionRepository = discussionRepository;
        this.learningService = learningService;
        this.attemptService = attemptService;
        this.generalizedPostTestService = generalizedPostTestService;
    }

    @PostMapping("/discussions/{discussionId}/{assessmentType}/attempts")
    public AssessmentAttemptService.AttemptSummary startDiscussionAttempt(
        @AuthenticationPrincipal StudentPrincipal principal,
        @PathVariable Long discussionId,
        @PathVariable String assessmentType
    ) {
        String type = assessmentType.trim().toUpperCase(Locale.ROOT);
        String stageCode = switch (type) {
            case "PRE_TEST" -> "PRE_TEST";
            case "ACTIVITY" -> "ACTIVITY";
            case "DISCUSSION_POST_TEST" -> "POST_TEST";
            default -> throw new IllegalArgumentException("Unsupported Discussion assessment type.");
        };
        Long competencyId = discussionRepository.findActiveCompetencyIdByDiscussionId(discussionId)
            .orElseThrow(() -> new EntityNotFoundException("Discussion not found."));
        learningService.openStage(principal.id(), competencyId, discussionId, stageCode);
        Assessment assessment = assessmentRepository.findByDiscussion_IdAndAssessmentType(discussionId, type)
            .filter(Assessment::isActive)
            .orElseThrow(() -> new EntityNotFoundException("This assessment is not configured yet."));
        return attemptService.startOrResumeAttempt(principal.id(), assessment.getId());
    }

    @GetMapping("/attempts/{attemptId}/questions")
    public java.util.List<AssessmentAttemptService.QuestionPrompt> discussionQuestions(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long attemptId
    ) {
        return attemptService.getQuestionsForAttempt(principal.id(), attemptId);
    }

    @GetMapping("/attempts/{attemptId}/answers")
    public java.util.List<AssessmentAttemptService.SavedAnswer> discussionAnswers(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long attemptId
    ) {
        return attemptService.getSavedAnswers(principal.id(), attemptId);
    }

    @PutMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<Void> saveDiscussionAnswer(
        @AuthenticationPrincipal StudentPrincipal principal,
        @PathVariable Long attemptId,
        @Valid @RequestBody AnswerRequest request
    ) {
        attemptService.saveAnswer(principal.id(), attemptId, request.questionId(), request.responseData());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public AssessmentAttemptService.SubmissionResult submitDiscussionAttempt(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long attemptId
    ) {
        return attemptService.submitAttempt(principal.id(), attemptId);
    }

    @GetMapping("/attempts/{attemptId}/result")
    public AssessmentAttemptService.SubmissionResult discussionResult(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long attemptId
    ) {
        return attemptService.getSubmissionResult(principal.id(), attemptId);
    }

    @GetMapping("/competencies/{competencyId}/generalized-post-test/status")
    public GeneralizedPostTestService.PostTestStatus generalizedStatus(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long competencyId
    ) {
        return generalizedPostTestService.getStatus(principal.id(), competencyId);
    }

    @GetMapping("/competencies/{competencyId}/generalized-post-test/history")
    public java.util.List<GeneralizedPostTestService.AttemptHistory> generalizedHistory(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long competencyId
    ) {
        return generalizedPostTestService.getAttemptHistory(principal.id(), competencyId);
    }

    @PostMapping("/competencies/{competencyId}/generalized-post-test/attempts")
    public GeneralizedPostTestService.AttemptSummary startGeneralizedAttempt(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long competencyId
    ) {
        return generalizedPostTestService.startOrResumeAttempt(principal.id(), competencyId);
    }

    @GetMapping("/generalized-attempts/{attemptId}/questions")
    public java.util.List<AssessmentAttemptService.QuestionPrompt> generalizedQuestions(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long attemptId
    ) {
        return generalizedPostTestService.getQuestionsForAttempt(principal.id(), attemptId);
    }

    @GetMapping("/generalized-attempts/{attemptId}/answers")
    public java.util.List<AssessmentAttemptService.SavedAnswer> generalizedAnswers(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long attemptId
    ) {
        return generalizedPostTestService.getSavedAnswers(principal.id(), attemptId);
    }

    @PutMapping("/generalized-attempts/{attemptId}/answers")
    public ResponseEntity<Void> saveGeneralizedAnswer(
        @AuthenticationPrincipal StudentPrincipal principal,
        @PathVariable Long attemptId,
        @Valid @RequestBody AnswerRequest request
    ) {
        generalizedPostTestService.saveAnswer(principal.id(), attemptId, request.questionId(), request.responseData());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generalized-attempts/{attemptId}/submit")
    public GeneralizedPostTestService.SubmissionResult submitGeneralizedAttempt(
        @AuthenticationPrincipal StudentPrincipal principal, @PathVariable Long attemptId
    ) {
        return generalizedPostTestService.submitAttempt(principal.id(), attemptId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> invalidRequest(MethodArgumentNotValidException exception) {
        FieldError field = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        return error(HttpStatus.BAD_REQUEST, field == null ? "Request fields are invalid." : field.getField() + " is required or invalid.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> invalid(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> unavailable(IllegalStateException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            message == null ? "The request could not be completed." : message));
    }

    public record AnswerRequest(@NotNull @Positive Long questionId, @NotBlank String responseData) {}
}
