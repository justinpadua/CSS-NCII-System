package com.cssncii.controller;

import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.dto.AssessmentManagementDtos.AssessmentRequest;
import com.cssncii.dto.AssessmentManagementDtos.AssessmentResponse;
import com.cssncii.dto.AssessmentManagementDtos.AssessmentUpdateRequest;
import com.cssncii.dto.AssessmentManagementDtos.ChoiceRequest;
import com.cssncii.dto.AssessmentManagementDtos.ChoiceResponse;
import com.cssncii.dto.AssessmentManagementDtos.QuestionCreateRequest;
import com.cssncii.dto.AssessmentManagementDtos.QuestionConfigInput;
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
        String type = request.questionType().trim().toUpperCase(java.util.Locale.ROOT);
        List<AssessmentManagementService.ChoiceDefinition> choices = "MATCHING_TYPE".equals(type)
            ? matchingChoiceDefinitions(request.questionConfig())
            : request.choices().stream()
                .map(input -> new AssessmentManagementService.ChoiceDefinition(input.choiceText(), input.displayOrder()))
                .toList();
        AssessmentManagementService.ConfiguredQuestion configured = Set.of("MATCHING_TYPE", "ENUMERATION")
            .contains(type)
            ? assessmentService.createQuestionWithChoices(assessmentId, request.questionType(), request.questionText(),
                request.points(), request.displayOrder(), choices, request.correctChoiceIndexes(), request.correctValue(),
                request.questionConfig())
            : assessmentService.createQuestionWithChoices(assessmentId, request.questionType(), request.questionText(),
                request.points(), request.displayOrder(), choices, request.correctChoiceIndexes(), request.correctValue());
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
        if (!existing.getQuestionType().equals(type)) {
            throw new IllegalArgumentException("Question type cannot be changed after creation.");
        }
        if ("MATCHING_TYPE".equals(type)) {
            Question updated = assessmentService.updateMatchingQuestionConfiguration(questionId, request.questionText(),
                request.points(), request.displayOrder(), request.questionConfig());
            return questionResponse(updated);
        }
        if ("ENUMERATION".equals(type)) {
            String key = newQuestionAnswerData(type, request.questionConfig(), choices);
            String config = questionConfigData(type, request.questionConfig());
            Question updated = assessmentService.updateQuestionConfiguration(questionId, request.questionText(),
                request.points(), request.displayOrder(), request.questionConfig(), key, config);
            return questionResponse(updated, choices);
        }
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

    private List<AssessmentManagementService.ChoiceDefinition> matchingChoiceDefinitions(QuestionConfigInput config) {
        if (config == null || config.matchingPairs() == null) return List.of();
        List<AssessmentManagementService.ChoiceDefinition> definitions = new java.util.ArrayList<>();
        java.util.LinkedHashSet<String> options = new java.util.LinkedHashSet<>();
        for (var pair : config.matchingPairs()) {
            String answer = pair.answerText().trim();
            if (options.add(answer.toLowerCase(java.util.Locale.ROOT))) {
                definitions.add(new AssessmentManagementService.ChoiceDefinition(answer, definitions.size() + 1));
            }
        }
        if (config.distractors() != null) {
            for (String option : config.distractors()) {
                String value = option.trim();
                if (options.add(value.toLowerCase(java.util.Locale.ROOT))) {
                    definitions.add(new AssessmentManagementService.ChoiceDefinition(value, definitions.size() + 1));
                }
            }
        }
        return definitions;
    }

    private String newQuestionAnswerData(String type, QuestionConfigInput config, List<AnswerChoice> choices) {
        if (config == null) throw new IllegalArgumentException("Question type configuration is required.");
        if ("ENUMERATION".equals(type)) {
            if (config.acceptedAnswers() == null) throw new IllegalArgumentException("Accepted answers are required.");
            return json(java.util.Map.of("acceptedAnswers", config.acceptedAnswers()));
        }
        if (!"MATCHING_TYPE".equals(type) || config.matchingPairs() == null) {
            throw new IllegalArgumentException("Matching pairs are required.");
        }
        List<java.util.Map<String, Object>> matches = new java.util.ArrayList<>();
        for (int i = 0; i < config.matchingPairs().size(); i++) {
            String text = config.matchingPairs().get(i).answerText().trim();
            AnswerChoice choice = choices.stream().filter(item -> normalizeAnswer(item.getChoiceText()).equals(normalizeAnswer(text)))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Matching answer must exist in the shared options."));
            matches.add(java.util.Map.of("promptIndex", i, "choiceId", choice.getId()));
        }
        return json(java.util.Map.of("matches", matches));
    }

    private String questionConfigData(String type, QuestionConfigInput config) {
        if ("MATCHING_TYPE".equals(type)) {
            return json(java.util.Map.of("matchingPrompts", config.matchingPairs().stream()
                .map(pair -> pair.prompt().trim()).toList()));
        }
        if ("ENUMERATION".equals(type)) {
            return json(java.util.Map.of("requiredAnswerCount", config.requiredAnswerCount(), "ordered", config.ordered()));
        }
        return "{}";
    }

    private String json(Object value) {
        try { return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(value); }
        catch (tools.jackson.core.JacksonException exception) {
            throw new IllegalArgumentException("Question configuration is invalid.", exception);
        }
    }

    private String normalizeAnswer(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
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
        if ("ENUMERATION".equals(question.getQuestionType())) {
            return new QuestionResponse(question.getId(), question.getAssessment().getId(), question.getQuestionType(),
                question.getQuestionText(), question.getPoints(), question.getDisplayOrder(), List.of(), null,
                choices.stream().map(choice -> choiceResponse(choice, false)).toList(),
                question.getQuestionConfig(), adminQuestionConfiguration(question, choices));
        }
        if ("MATCHING_TYPE".equals(question.getQuestionType())) {
            java.util.regex.Matcher matching = java.util.regex.Pattern
                .compile("\\\"choiceId\\\"\\s*:\\s*([0-9]+)").matcher(data);
            List<Long> matches = new java.util.ArrayList<>();
            while (matching.find()) matches.add(Long.parseLong(matching.group(1)));
            correctIds = List.copyOf(matches);
        }
        java.util.regex.Matcher oneChoice = java.util.regex.Pattern
            .compile("\\\"choiceId\\\"\\s*:\\s*([0-9]+)").matcher(data);
        java.util.regex.Matcher manyChoices = java.util.regex.Pattern
            .compile("\\\"choiceIds\\\"\\s*:\\s*\\[([0-9,\\s]+)]").matcher(data);
        java.util.regex.Matcher bool = java.util.regex.Pattern
            .compile("\\\"value\\\"\\s*:\\s*(true|false)").matcher(data);
        if ("MATCHING_TYPE".equals(question.getQuestionType())) {
            // Matching correctness is represented by the per-prompt choice IDs above.
        } else if (oneChoice.find()) {
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
            choices.stream().map(choice -> choiceResponse(choice, selectedCorrectIds.contains(choice.getId()))).toList(),
            question.getQuestionConfig(), Set.of("MATCHING_TYPE").contains(question.getQuestionType())
                ? adminQuestionConfiguration(question, choices) : null);
    }

    private String adminQuestionConfiguration(Question question, List<AnswerChoice> choices) {
        try {
            var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
            var key = mapper.readTree(question.getCorrectAnswerData());
            var config = question.getQuestionConfig() == null ? mapper.createObjectNode()
                : mapper.readTree(question.getQuestionConfig());
            if ("ENUMERATION".equals(question.getQuestionType())) {
                java.util.Map<String, Object> value = new java.util.LinkedHashMap<>();
                value.put("acceptedAnswers", mapper.treeToValue(key.get("acceptedAnswers"), List.class));
                value.put("requiredAnswerCount", config.get("requiredAnswerCount").intValue());
                value.put("ordered", config.get("ordered").booleanValue());
                return mapper.writeValueAsString(value);
            }
            var prompts = config.get("matchingPrompts");
            var matches = key.get("matches");
            if (prompts == null || !prompts.isArray() || matches == null || !matches.isArray()) {
                throw new IllegalStateException("Stored matching configuration is invalid.");
            }
            java.util.Map<Long, String> choiceText = choices.stream().collect(java.util.stream.Collectors.toMap(
                AnswerChoice::getId, AnswerChoice::getChoiceText));
            List<java.util.Map<String, String>> pairs = new java.util.ArrayList<>();
            java.util.Set<Long> correctIds = new java.util.HashSet<>();
            for (var match : matches) {
                long id = match.get("choiceId").longValue();
                int index = match.get("promptIndex").intValue();
                if (index < 0 || index >= prompts.size() || !choiceText.containsKey(id)) {
                    throw new IllegalStateException("Stored matching answer map is invalid.");
                }
                pairs.add(java.util.Map.of("prompt", prompts.get(index).textValue(), "answerText", choiceText.get(id)));
                correctIds.add(id);
            }
            java.util.Map<String, Object> value = new java.util.LinkedHashMap<>();
            value.put("matchingPairs", pairs);
            value.put("distractors", choices.stream().filter(choice -> !correctIds.contains(choice.getId()))
                .map(AnswerChoice::getChoiceText).toList());
            return mapper.writeValueAsString(value);
        } catch (tools.jackson.core.JacksonException exception) {
            throw new IllegalStateException("Stored question configuration is invalid.", exception);
        }
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
