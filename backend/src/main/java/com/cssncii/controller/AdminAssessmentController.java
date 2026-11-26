package com.cssncii.controller;

import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.dto.AssessmentManagementDtos.AssessmentRequest;
import com.cssncii.dto.AssessmentManagementDtos.AssessmentResponse;
import com.cssncii.dto.AssessmentManagementDtos.AssessmentUpdateRequest;
import com.cssncii.dto.AssessmentManagementDtos.ChoiceRequest;
import com.cssncii.dto.AssessmentManagementDtos.ChoiceResponse;
import com.cssncii.dto.AssessmentManagementDtos.QuestionCreateRequest;
import com.cssncii.dto.AssessmentManagementDtos.QuestionResponse;
import com.cssncii.dto.AssessmentManagementDtos.QuestionUpdateRequest;
import com.cssncii.entity.AnswerChoice;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Question;
import com.cssncii.service.AssessmentManagementService;
import com.cssncii.service.CourseManagementService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAssessmentController {
    private static final Set<String> DISCUSSION_TYPES = Set.of("PRE_TEST", "ACTIVITY", "DISCUSSION_POST_TEST");
    private static final String GENERALIZED_TYPE = "GENERALIZED_POST_TEST";

    private final AssessmentManagementService assessmentService;
    private final CourseManagementService courseService;

    public AdminAssessmentController(
        AssessmentManagementService assessmentService,
        CourseManagementService courseService
    ) {
        this.assessmentService = assessmentService;
        this.courseService = courseService;
    }

    @GetMapping("/competencies/{competencyId}/discussions/{discussionId}/assessments")
    public List<AssessmentResponse> getDiscussionAssessments(
        @PathVariable Long competencyId, @PathVariable Long discussionId
    ) {
        requireDiscussionInCompetency(competencyId, discussionId);
        return assessmentService.getAssessmentsByDiscussion(discussionId).stream().map(this::assessmentResponse).toList();
    }

    @PostMapping("/competencies/{competencyId}/discussions/{discussionId}/assessments")
    public ResponseEntity<AssessmentResponse> createDiscussionAssessment(
        @PathVariable Long competencyId, @PathVariable Long discussionId,
        @Valid @RequestBody AssessmentRequest request
    ) {
        requireDiscussionInCompetency(competencyId, discussionId);
        String type = request.assessmentType().trim().toUpperCase(java.util.Locale.ROOT);
        if (!DISCUSSION_TYPES.contains(type)) {
            throw new IllegalArgumentException("Discussion assessment type must be PRE_TEST, ACTIVITY, or DISCUSSION_POST_TEST.");
        }
        Assessment saved = assessmentService.createAssessment(type, discussionId, null, request.title(), request.instructions(),
            false);
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentResponse(saved));
    }

    @GetMapping("/competencies/{competencyId}/assessments")
    public List<AssessmentResponse> getCompetencyAssessments(@PathVariable Long competencyId) {
        return assessmentService.getAssessmentsByCompetency(competencyId).stream().map(this::assessmentResponse).toList();
    }

    @PostMapping("/competencies/{competencyId}/assessments")
    public ResponseEntity<AssessmentResponse> createCompetencyAssessment(
        @PathVariable Long competencyId, @Valid @RequestBody AssessmentRequest request
    ) {
        if (!GENERALIZED_TYPE.equals(request.assessmentType().trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Only GENERALIZED_POST_TEST can be assigned to a competency.");
        }
        Assessment saved = assessmentService.createAssessment(
            GENERALIZED_TYPE, null, competencyId, request.title(), request.instructions(), false);
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentResponse(saved));
    }

    @PutMapping("/assessments/{assessmentId}")
    public AssessmentResponse updateAssessment(
        @PathVariable Long assessmentId, @Valid @RequestBody AssessmentUpdateRequest request
    ) {
        Assessment saved = assessmentService.updateAssessment(
            assessmentId, request.title(), request.instructions(), request.active());
        return assessmentResponse(saved);
    }

    @GetMapping("/assessments/{assessmentId}/questions")
    public List<QuestionResponse> getQuestions(@PathVariable Long assessmentId) {
        return assessmentService.getQuestionsByAssessment(assessmentId).stream().map(this::questionResponse).toList();
    }

    @PostMapping("/assessments/{assessmentId}/questions")
    public ResponseEntity<QuestionResponse> createQuestion(
        @PathVariable Long assessmentId, @Valid @RequestBody QuestionCreateRequest request
    ) {
        List<AssessmentManagementService.ChoiceDefinition> choices = request.choices().stream()
            .map(input -> new AssessmentManagementService.ChoiceDefinition(input.choiceText(), input.displayOrder()))
            .toList();
        AssessmentManagementService.ConfiguredQuestion configured = assessmentService.createQuestionWithChoices(
            assessmentId, request.questionType(), request.questionText(), request.points(), request.displayOrder(),
            choices, request.correctChoiceIndexes(), request.correctValue());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(questionResponse(configured.question(), configured.choices()));
    }

    @PutMapping("/assessments/{assessmentId}/questions/{questionId}")
    public QuestionResponse updateQuestion(
        @PathVariable Long assessmentId, @PathVariable Long questionId,
        @Valid @RequestBody QuestionUpdateRequest request
    ) {
        Question existing = assessmentService.getQuestionById(questionId);
        requireQuestionInAssessment(assessmentId, existing);
        List<AnswerChoice> choices = assessmentService.getAnswerChoicesByQuestion(questionId);
        String type = request.questionType().trim().toUpperCase(java.util.Locale.ROOT);
        String answerData = answerData(type, request.correctChoiceIds(), request.correctValue(), choices);
        Question updated = assessmentService.updateQuestion(questionId, type, request.questionText(), answerData,
            request.points(), request.displayOrder());
        return questionResponse(updated, choices);
    }

    @DeleteMapping("/assessments/{assessmentId}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long assessmentId, @PathVariable Long questionId) {
        Question existing = assessmentService.getQuestionById(questionId);
        requireQuestionInAssessment(assessmentId, existing);
        assessmentService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/questions/{questionId}/choices")
    public ResponseEntity<ChoiceResponse> createChoice(
        @PathVariable Long questionId, @Valid @RequestBody ChoiceRequest request
    ) {
        AnswerChoice choice = assessmentService.createAnswerChoice(questionId, request.choiceText(), request.displayOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(choiceResponse(choice, false));
    }

    @PutMapping("/questions/{questionId}/choices/{choiceId}")
    public ChoiceResponse updateChoice(
        @PathVariable Long questionId, @PathVariable Long choiceId,
        @Valid @RequestBody ChoiceRequest request
    ) {
        AnswerChoice existing = assessmentService.getAnswerChoiceById(choiceId);
        requireChoiceInQuestion(questionId, existing);
        AnswerChoice updated = assessmentService.updateAnswerChoice(choiceId, request.choiceText(), request.displayOrder());
        return choiceResponse(updated, isChoiceCorrect(updated));
    }

    @DeleteMapping("/questions/{questionId}/choices/{choiceId}")
    public ResponseEntity<Void> deleteChoice(@PathVariable Long questionId, @PathVariable Long choiceId) {
        AnswerChoice existing = assessmentService.getAnswerChoiceById(choiceId);
        requireChoiceInQuestion(questionId, existing);
        if (isChoiceCorrect(existing)) {
            throw new IllegalArgumentException("Update the question's correct answer before deleting this choice.");
        }
        assessmentService.deleteAnswerChoice(choiceId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> invalidRequest(MethodArgumentNotValidException exception) {
        FieldError error = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        return error(HttpStatus.BAD_REQUEST, error == null ? "Request contains invalid fields."
            : error.getField() + " is required or invalid.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> invalidArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> conflict(IllegalStateException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Assessment, question, choice, or selected course record was not found.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> dataConflict(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "This assessment record conflicts with an existing assessment or order.");
    }

    private void requireDiscussionInCompetency(Long competencyId, Long discussionId) {
        Discussion discussion = courseService.getDiscussionById(discussionId);
        if (discussion.getCompetency() == null || !discussion.getCompetency().getId().equals(competencyId)) {
            throw new EntityNotFoundException("Discussion does not belong to competency.");
        }
    }

    private void requireQuestionInAssessment(Long assessmentId, Question question) {
        if (question.getAssessment() == null || !question.getAssessment().getId().equals(assessmentId)) {
            throw new EntityNotFoundException("Question not found in assessment.");
        }
    }

    private void requireChoiceInQuestion(Long questionId, AnswerChoice choice) {
        if (choice.getQuestion() == null || !choice.getQuestion().getId().equals(questionId)) {
            throw new EntityNotFoundException("Choice not found in question.");
        }
    }

    private String answerData(String type, List<Long> correctIds, Boolean correctValue, List<AnswerChoice> choices) {
        if ("TRUE_FALSE".equals(type)) {
            if (correctValue == null || (correctIds != null && !correctIds.isEmpty())) {
                throw new IllegalArgumentException("TRUE_FALSE requires a boolean correctValue and no correctChoiceIds.");
            }
            return "{\"value\":" + correctValue + "}";
        }
        if (correctValue != null || correctIds == null || correctIds.isEmpty()
            || ("MULTIPLE_CHOICE".equals(type) && correctIds.size() != 1)) {
            throw new IllegalArgumentException("Choose exactly one correct choice for MULTIPLE_CHOICE or one or more for MULTIPLE_SELECT.");
        }
        Set<Long> available = choices.stream().map(AnswerChoice::getId).collect(java.util.stream.Collectors.toSet());
        if (correctIds.stream().anyMatch(id -> id == null || !available.contains(id))
            || correctIds.stream().distinct().count() != correctIds.size()) {
            throw new IllegalArgumentException("Correct choice IDs must be distinct choices belonging to this question.");
        }
        if ("MULTIPLE_CHOICE".equals(type)) return "{\"choiceId\":" + correctIds.getFirst() + "}";
        if (!"MULTIPLE_SELECT".equals(type)) throw new IllegalArgumentException("Unsupported question type.");
        return "{\"choiceIds\":[" + String.join(",", correctIds.stream().map(String::valueOf).toList()) + "]}";
    }

    private AssessmentResponse assessmentResponse(Assessment assessment) {
        return new AssessmentResponse(assessment.getId(),
            assessment.getDiscussion() == null ? null : assessment.getDiscussion().getId(),
            assessment.getCompetency() == null ? null : assessment.getCompetency().getId(),
            assessment.getAssessmentType(), assessment.getTitle(), assessment.getInstructions(),
            assessment.getPassPercentage(), assessment.isActive());
    }

    private QuestionResponse questionResponse(Question question) {
        return questionResponse(question, assessmentService.getAnswerChoicesByQuestion(question.getId()));
    }

    private QuestionResponse questionResponse(Question question, List<AnswerChoice> choices) {
        String data = question.getCorrectAnswerData();
        if (data == null) throw new IllegalStateException("Stored question answer configuration is invalid.");
        List<Long> correctIds = List.of();
        Boolean correctValue = null;
        java.util.regex.Matcher oneChoice = java.util.regex.Pattern
            .compile("\\\"choiceId\\\"\\s*:\\s*([0-9]+)").matcher(data);
        java.util.regex.Matcher manyChoices = java.util.regex.Pattern
            .compile("\\\"choiceIds\\\"\\s*:\\s*\\[([0-9,\\s]+)]").matcher(data);
        java.util.regex.Matcher bool = java.util.regex.Pattern
            .compile("\\\"value\\\"\\s*:\\s*(true|false)").matcher(data);
        if (oneChoice.find()) {
            correctIds = List.of(Long.parseLong(oneChoice.group(1)));
        } else if (manyChoices.find()) {
            correctIds = java.util.Arrays.stream(manyChoices.group(1).split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(Long::parseLong).toList();
        } else if (bool.find()) {
            correctValue = Boolean.valueOf(bool.group(1));
        } else {
            throw new IllegalStateException("Stored question answer configuration is invalid.");
        }
        List<Long> selectedCorrectIds = correctIds;
        return new QuestionResponse(question.getId(), question.getAssessment().getId(), question.getQuestionType(),
            question.getQuestionText(), question.getPoints(), question.getDisplayOrder(), correctIds, correctValue,
            choices.stream().map(choice -> choiceResponse(choice, selectedCorrectIds.contains(choice.getId()))).toList());
    }

    private boolean isChoiceCorrect(AnswerChoice choice) {
        return questionResponse(choice.getQuestion()).correctChoiceIds().contains(choice.getId());
    }

    private ChoiceResponse choiceResponse(AnswerChoice choice, boolean correct) {
        return new ChoiceResponse(choice.getId(), choice.getQuestion().getId(), choice.getChoiceText(),
            choice.getDisplayOrder(), correct);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(message == null
            ? "Request could not be completed." : message));
    }
}
