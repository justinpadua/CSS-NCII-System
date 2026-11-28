package com.cssncii.service;

import com.cssncii.entity.AnswerChoice;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.AssessmentAttempt;
import com.cssncii.entity.AttemptAnswer;
import com.cssncii.entity.Question;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStageProgress;
import com.cssncii.repository.AnswerChoiceRepository;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.AttemptAnswerRepository;
import com.cssncii.repository.QuestionRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Locale;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessmentAttemptService {
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String AVAILABLE = "AVAILABLE";
    private static final List<String> DISCUSSION_TYPES =
        List.of("PRE_TEST", "ACTIVITY", "DISCUSSION_POST_TEST");
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .build();

    private final StudentRepository studentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AnswerChoiceRepository choiceRepository;
    private final StudentStageProgressRepository stageProgressRepository;
    private final StudentProgressService studentProgressService;

    public AssessmentAttemptService(
        StudentRepository studentRepository,
        AssessmentRepository assessmentRepository,
        AssessmentAttemptRepository attemptRepository,
        AttemptAnswerRepository answerRepository,
        QuestionRepository questionRepository,
        AnswerChoiceRepository choiceRepository,
        StudentStageProgressRepository stageProgressRepository,
        StudentProgressService studentProgressService
    ) {
        this.studentRepository = studentRepository;
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.choiceRepository = choiceRepository;
        this.stageProgressRepository = stageProgressRepository;
        this.studentProgressService = studentProgressService;
    }

    @Transactional
    public AttemptSummary startOrResumeAttempt(Long studentId, Long assessmentId) {
        Student student = findActiveStudentForUpdate(studentId);
        Assessment assessment = findAssessment(assessmentId);
        validateDiscussionAssessment(assessment);
        AssessmentAttempt existing = attemptRepository
            .findByStudent_IdAndAssessment_Id(studentId, assessmentId)
            .orElse(null);
        if (existing != null) {
            if (IN_PROGRESS.equals(existing.getStatus())) {
                return toSummary(existing);
            }
            throw new IllegalStateException("This Discussion assessment has already been submitted.");
        }

        if (!assessment.isActive()) {
            throw new IllegalStateException("This assessment is inactive.");
        }
        if (questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(assessmentId).isEmpty()) {
            throw new IllegalStateException("The assessment has no configured questions.");
        }
        requireAssessmentStageAvailable(studentId, assessment);

        LocalDateTime now = utcNow();
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setStudent(student);
        attempt.setAssessment(assessment);
        attempt.setAttemptNumber(1);
        attempt.setStatus(IN_PROGRESS);
        attempt.setStartedAt(now);
        attempt.setSubmittedAt(null);
        attempt.setCreatedAt(now);
        attempt.setUpdatedAt(now);
        return toSummary(attemptRepository.save(attempt));
    }

    public List<QuestionPrompt> getQuestionsForAttempt(Long studentId, Long attemptId) {
        findActiveStudent(studentId);
        AssessmentAttempt attempt = findAttempt(attemptId);
        requireAttemptOwner(studentId, attempt);
        requireInProgress(attempt);

        List<QuestionPrompt> prompts = new ArrayList<>();
        for (Question question : questionRepository
            .findByAssessment_IdOrderByDisplayOrderAsc(attempt.getAssessment().getId())) {
            List<ChoicePrompt> choices = choiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(question.getId())
                .stream()
                .map(this::toChoicePrompt)
                .toList();
            prompts.add(new QuestionPrompt(
                question.getId(), question.getQuestionType(), question.getQuestionText(),
                question.getPoints(), question.getDisplayOrder(), choices, matchingPrompts(question), question.getQuestionConfig()));
        }
        return prompts;
    }

    /** Returns only this student's saved responses, never the assessment answer key. */
    public List<SavedAnswer> getSavedAnswers(Long studentId, Long attemptId) {
        findActiveStudent(studentId);
        AssessmentAttempt attempt = findAttempt(attemptId);
        requireAttemptOwner(studentId, attempt);
        requireInProgress(attempt);
        return answerRepository.findByAttempt_Id(attemptId).stream()
            .map(answer -> new SavedAnswer(answer.getQuestion().getId(), answer.getResponseData()))
            .toList();
    }

    @Transactional
    public AttemptAnswer saveAnswer(Long studentId, Long attemptId, Long questionId, String responseData) {
        findActiveStudentForUpdate(studentId);
        AssessmentAttempt attempt = findAttempt(attemptId);
        requireAttemptOwner(studentId, attempt);
        requireInProgress(attempt);
        if (questionId == null || questionId <= 0) {
            throw new IllegalArgumentException("questionId must be a positive ID.");
        }
        if (responseData == null || responseData.isBlank()) {
            throw new IllegalArgumentException("responseData is required.");
        }

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));
        if (!question.getAssessment().getId().equals(attempt.getAssessment().getId())) {
            throw new IllegalArgumentException("Question does not belong to this assessment.");
        }
        scoreResponse(question, responseData,
            choiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(questionId));

        LocalDateTime now = utcNow();
        AttemptAnswer answer = answerRepository.findByAttempt_IdAndQuestion_Id(attemptId, questionId)
            .orElseGet(() -> {
                AttemptAnswer newAnswer = new AttemptAnswer();
                newAnswer.setAttempt(attempt);
                newAnswer.setQuestion(question);
                newAnswer.setCreatedAt(now);
                return newAnswer;
            });
        answer.setResponseData(responseData);
        answer.setCorrect(null);
        answer.setAwardedPoints(null);
        answer.setUpdatedAt(now);
        attempt.setUpdatedAt(now);
        attemptRepository.save(attempt);
        return answerRepository.save(answer);
    }

    @Transactional
    public SubmissionResult submitAttempt(Long studentId, Long attemptId) {
        findActiveStudentForUpdate(studentId);
        AssessmentAttempt attempt = findAttempt(attemptId);
        requireAttemptOwner(studentId, attempt);
        requireInProgress(attempt);

        List<Question> questions = questionRepository
            .findByAssessment_IdOrderByDisplayOrderAsc(attempt.getAssessment().getId());
        if (questions.isEmpty()) {
            throw new IllegalStateException("The assessment has no configured questions.");
        }

        Map<Long, AttemptAnswer> savedAnswers = new HashMap<>();
        for (AttemptAnswer answer : answerRepository.findByAttempt_Id(attemptId)) {
            savedAnswers.put(answer.getQuestion().getId(), answer);
        }
        for (Question question : questions) {
            if (!savedAnswers.containsKey(question.getId())) {
                throw new IllegalArgumentException("Every question must be answered before submission.");
            }
        }

        BigDecimal totalPossible = BigDecimal.ZERO;
        BigDecimal totalScore = BigDecimal.ZERO;
        LocalDateTime now = utcNow();
        for (Question question : questions) {
            List<AnswerChoice> choices = choiceRepository
                .findByQuestion_IdOrderByDisplayOrderAsc(question.getId());
            AttemptAnswer answer = savedAnswers.get(question.getId());
            ScoredAnswer scored = scoreResponse(question, answer.getResponseData(), choices);
            answer.setCorrect(scored.correct());
            answer.setAwardedPoints(scored.awardedPoints());
            answer.setUpdatedAt(now);
            answerRepository.save(answer);
            totalPossible = totalPossible.add(question.getPoints());
            totalScore = totalScore.add(scored.awardedPoints());
        }
        if (totalPossible.signum() <= 0) {
            throw new IllegalStateException("Assessment total possible points must be greater than zero.");
        }

        BigDecimal percentage = totalScore.multiply(BigDecimal.valueOf(100))
            .divide(totalPossible, 2, RoundingMode.HALF_UP);
        attempt.setScore(totalScore);
        attempt.setTotalPossibleScore(totalPossible);
        attempt.setPercentage(percentage);
        attempt.setPassed(null);
        attempt.setStatus(SUBMITTED);
        attempt.setSubmittedAt(now);
        attempt.setUpdatedAt(now);
        attemptRepository.save(attempt);

        String stageCode = assessmentStageCode(attempt.getAssessment().getAssessmentType());
        studentProgressService.completeStage(
            studentId, attempt.getAssessment().getDiscussion().getId(), stageCode);
        return toSubmissionResult(attempt, questions, savedAnswers);
    }

    public SubmissionResult getSubmissionResult(Long studentId, Long attemptId) {
        findActiveStudent(studentId);
        AssessmentAttempt attempt = findAttempt(attemptId);
        requireAttemptOwner(studentId, attempt);
        if (!SUBMITTED.equals(attempt.getStatus())) {
            throw new IllegalStateException("Feedback is available after the assessment is submitted.");
        }
        List<Question> questions = questionRepository
            .findByAssessment_IdOrderByDisplayOrderAsc(attempt.getAssessment().getId());
        Map<Long, AttemptAnswer> savedAnswers = new HashMap<>();
        for (AttemptAnswer answer : answerRepository.findByAttempt_Id(attemptId)) {
            savedAnswers.put(answer.getQuestion().getId(), answer);
        }
        return toSubmissionResult(attempt, questions, savedAnswers);
    }

    ScoredAnswer scoreResponse(Question question, String responseData, List<AnswerChoice> choices) {
        String questionType = question.getQuestionType();
        if (questionType == null || questionType.isBlank()) {
            throw new IllegalStateException("Question type is required for scoring.");
        }
        JsonNode correct = parseJson(question.getCorrectAnswerData(), "Stored correct answer is not valid JSON.", true);
        JsonNode response = parseJson(responseData, "Student response is not valid JSON.", false);
        Set<Long> validChoiceIds = new LinkedHashSet<>();
        for (AnswerChoice choice : choices) {
            validChoiceIds.add(choice.getId());
        }

        boolean isCorrect;
        switch (questionType) {
            case "MULTIPLE_CHOICE" -> {
                long correctId = singleChoiceId(correct, "choiceId", true);
                long selectedId = singleChoiceId(response, "choiceId", false);
                requireChoiceBelongs(validChoiceIds, correctId, true);
                requireChoiceBelongs(validChoiceIds, selectedId, false);
                isCorrect = selectedId == correctId;
            }
            case "TRUE_FALSE" -> {
                boolean expected = booleanValue(correct, "value", true);
                boolean selected = booleanValue(response, "value", false);
                isCorrect = selected == expected;
            }
            case "MULTIPLE_SELECT" -> {
                Set<Long> expected = choiceIds(correct, "choiceIds", validChoiceIds, true);
                Set<Long> selected = choiceIds(response, "choiceIds", validChoiceIds, false);
                isCorrect = selected.equals(expected);
            }
            case "ENUMERATION" -> {
                int required = requiredAnswerCount(question);
                boolean ordered = orderedEnumeration(question);
                List<String> accepted = acceptedAnswerList(correct);
                if (accepted.size() < required) {
                    throw new IllegalStateException("Stored enumeration accepted answers are incomplete.");
                }
                List<String> submitted = textArray(response, "answers", required);
                int correctCount = 0;
                if (ordered) {
                    for (int i = 0; i < required; i++) {
                        if (normalizeAnswer(submitted.get(i)).equals(normalizeAnswer(accepted.get(i)))) correctCount++;
                    }
                } else {
                    Set<String> unmatched = new HashSet<>(accepted.stream().map(this::normalizeAnswer).toList());
                    for (String answer : submitted) {
                        String normalized = normalizeAnswer(answer);
                        if (!normalized.isEmpty() && unmatched.remove(normalized)) correctCount++;
                    }
                }
                return partialScore(question, correctCount, required);
            }
            case "MATCHING_TYPE" -> {
                int required = matchingPrompts(question).size();
                if (required == 0) throw new IllegalStateException("Matching question has no configured prompts.");
                List<MatchEntry> expected = matchEntries(correct, validChoiceIds, true, required);
                List<MatchEntry> selected = matchEntries(response, validChoiceIds, false, required);
                int correctCount = 0;
                for (int i = 0; i < required; i++) {
                    if (java.util.Objects.equals(expected.get(i).choiceId(), selected.get(i).choiceId())
                        && expected.get(i).choiceId() != null) correctCount++;
                }
                return partialScore(question, correctCount, required);
            }
            default -> throw new IllegalArgumentException("Unsupported question type: " + questionType);
        }
        BigDecimal awarded = isCorrect ? question.getPoints() : BigDecimal.ZERO;
        return new ScoredAnswer(isCorrect, awarded);
    }

    private JsonNode parseJson(String value, String message, boolean storedAnswer) {
        if (value == null || value.isBlank()) {
            throw storedAnswer ? new IllegalStateException(message) : new IllegalArgumentException(message);
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(value);
            if (node == null || !node.isObject()) {
                throw storedAnswer ? new IllegalStateException(message) : new IllegalArgumentException(message);
            }
            return node;
        } catch (JacksonException exception) {
            if (storedAnswer) {
                throw new IllegalStateException(message, exception);
            }
            throw new IllegalArgumentException(message, exception);
        }
    }

    private long singleChoiceId(JsonNode object, String property, boolean storedAnswer) {
        validateObjectShape(object, property, storedAnswer);
        JsonNode value = object.get(property);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0) {
            throw invalidValue(storedAnswer, property + " must be a positive integer choice ID.");
        }
        return value.longValue();
    }

    private boolean booleanValue(JsonNode object, String property, boolean storedAnswer) {
        validateObjectShape(object, property, storedAnswer);
        JsonNode value = object.get(property);
        if (value == null || !value.isBoolean()) {
            throw invalidValue(storedAnswer, property + " must be a JSON boolean.");
        }
        return value.booleanValue();
    }

    private Set<Long> choiceIds(
        JsonNode object, String property, Set<Long> validChoiceIds, boolean storedAnswer
    ) {
        validateObjectShape(object, property, storedAnswer);
        JsonNode array = object.get(property);
        if (array == null || !array.isArray()) {
            throw invalidValue(storedAnswer, property + " must be an array of choice IDs.");
        }
        if (array.isEmpty()) {
            throw invalidValue(storedAnswer, storedAnswer
                ? property + " must contain at least one correct choice ID."
                : "A Multiple Select question must have at least one selected choice before submission.");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (JsonNode item : array) {
            if (!item.isIntegralNumber() || !item.canConvertToLong() || item.longValue() <= 0) {
                throw invalidValue(storedAnswer, property + " must contain positive integer choice IDs.");
            }
            long id = item.longValue();
            if (!ids.add(id)) {
                throw invalidValue(storedAnswer, "Duplicate choice IDs are not allowed.");
            }
            requireChoiceBelongs(validChoiceIds, id, storedAnswer);
        }
        return ids;
    }

    private void validateObjectShape(JsonNode object, String property, boolean storedAnswer) {
        if (object == null || !object.isObject() || object.size() != 1 || !object.has(property)) {
            throw invalidValue(storedAnswer, "JSON object must contain only the required '" + property + "' property.");
        }
    }

    private void requireChoiceBelongs(Set<Long> validChoiceIds, long choiceId, boolean storedAnswer) {
        if (!validChoiceIds.contains(choiceId)) {
            throw invalidValue(storedAnswer, "Choice ID does not belong to this question: " + choiceId);
        }
    }

    private RuntimeException invalidValue(boolean storedAnswer, String message) {
        return storedAnswer ? new IllegalStateException(message) : new IllegalArgumentException(message);
    }

    private ScoredAnswer partialScore(Question question, int correctCount, int total) {
        BigDecimal points = question.getPoints().multiply(BigDecimal.valueOf(correctCount))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new ScoredAnswer(correctCount == total, points);
    }

    private List<String> acceptedAnswerList(JsonNode object) {
        JsonNode values = object.get("acceptedAnswers");
        if (object.size() != 1 || values == null || !values.isArray() || values.isEmpty()) {
            throw new IllegalStateException("Stored accepted answers are invalid.");
        }
        List<String> result = new ArrayList<>();
        values.forEach(value -> {
            if (!value.isTextual() || value.textValue().isBlank()) {
                throw new IllegalStateException("Stored accepted answers are invalid.");
            }
            result.add(value.textValue());
        });
        return result;
    }

    private List<String> textArray(JsonNode object, String name, int required) {
        JsonNode values = object.get(name);
        if (object.size() != 1 || values == null || !values.isArray() || values.size() != required) {
            throw new IllegalArgumentException("Response must contain exactly the configured number of answers.");
        }
        List<String> result = new ArrayList<>();
        values.forEach(value -> {
            if (!value.isTextual()) throw new IllegalArgumentException("Each enumeration answer must be text.");
            result.add(value.textValue());
        });
        return result;
    }

    private int requiredAnswerCount(Question question) {
        JsonNode config = parseJson(question.getQuestionConfig(), "Stored enumeration configuration is invalid.", true);
        JsonNode count = config.get("requiredAnswerCount");
        if (config.size() != 2 || count == null || !count.canConvertToInt() || count.intValue() < 1
            || !config.has("ordered") || !config.get("ordered").isBoolean()) {
            throw new IllegalStateException("Stored enumeration configuration is invalid.");
        }
        return count.intValue();
    }

    private boolean orderedEnumeration(Question question) {
        JsonNode config = parseJson(question.getQuestionConfig(), "Stored enumeration configuration is invalid.", true);
        return config.get("ordered").booleanValue();
    }

    private List<String> matchingPrompts(Question question) {
        if (!"MATCHING_TYPE".equals(question.getQuestionType())) return List.of();
        JsonNode config = parseJson(question.getQuestionConfig(), "Stored matching configuration is invalid.", true);
        JsonNode prompts = config.get("matchingPrompts");
        if (config.size() != 1 || prompts == null || !prompts.isArray() || prompts.isEmpty()) {
            throw new IllegalStateException("Stored matching configuration is invalid.");
        }
        List<String> result = new ArrayList<>();
        prompts.forEach(prompt -> {
            if (!prompt.isTextual() || prompt.textValue().isBlank()) {
                throw new IllegalStateException("Stored matching configuration is invalid.");
            }
            result.add(prompt.textValue());
        });
        return result;
    }

    private List<MatchEntry> matchEntries(JsonNode object, Set<Long> validChoiceIds, boolean stored, int required) {
        JsonNode values = object.get("matches");
        if (object.size() != 1 || values == null || !values.isArray() || values.size() != required) {
            throw invalidValue(stored, "Matching response must include one entry for every prompt.");
        }
        MatchEntry[] indexed = new MatchEntry[required];
        Set<Long> usedOptions = new HashSet<>();
        for (JsonNode item : values) {
            JsonNode indexNode = item.get("promptIndex");
            JsonNode choiceNode = item.get("choiceId");
            if (!item.isObject() || item.size() != 2 || indexNode == null || !indexNode.canConvertToInt()
                || choiceNode == null) throw invalidValue(stored, "Matching entries are malformed.");
            int index = indexNode.intValue();
            if (index < 0 || index >= required || indexed[index] != null) {
                throw invalidValue(stored, "Matching prompt indexes must be unique and in range.");
            }
            Long choiceId = null;
            if (!choiceNode.isNull()) {
                if (!choiceNode.isIntegralNumber() || !choiceNode.canConvertToLong() || choiceNode.longValue() <= 0) {
                    throw invalidValue(stored, "Matching choice IDs must be positive integers or null.");
                }
                choiceId = choiceNode.longValue();
                requireChoiceBelongs(validChoiceIds, choiceId, stored);
                if (!usedOptions.add(choiceId)) {
                    throw invalidValue(stored, stored
                        ? "A correct matching option cannot be mapped more than once."
                        : "A matching option may only be selected once.");
                }
            } else if (stored) {
                throw new IllegalStateException("Stored matching answer cannot be blank.");
            }
            indexed[index] = new MatchEntry(index, choiceId);
        }
        if (java.util.Arrays.stream(indexed).anyMatch(java.util.Objects::isNull)) {
            throw invalidValue(stored, "Every matching prompt must have an entry.");
        }
        return List.of(indexed);
    }

    private String normalizeAnswer(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private record MatchEntry(int promptIndex, Long choiceId) {}

    private SubmissionResult toSubmissionResult(
        AssessmentAttempt attempt, List<Question> questions, Map<Long, AttemptAnswer> answers
    ) {
        String type = attempt.getAssessment().getAssessmentType();
        boolean showCorrectAnswer = "ACTIVITY".equals(type) || "DISCUSSION_POST_TEST".equals(type);
        List<IncorrectAnswer> incorrectAnswers = new ArrayList<>();
        for (Question question : questions) {
            AttemptAnswer answer = answers.get(question.getId());
            if (answer != null && Boolean.FALSE.equals(answer.getCorrect())) {
                incorrectAnswers.add(new IncorrectAnswer(
                    question.getId(), question.getQuestionText(), answer.getResponseData(),
                    showCorrectAnswer ? question.getCorrectAnswerData() : null,
                    answer.getAwardedPoints()));
            }
        }
        return new SubmissionResult(
            attempt.getId(), type, attempt.getScore(), attempt.getTotalPossibleScore(),
            attempt.getPercentage(), attempt.getSubmittedAt(), List.copyOf(incorrectAnswers));
    }

    private String assessmentStageCode(String assessmentType) {
        return switch (assessmentType) {
            case "PRE_TEST" -> "PRE_TEST";
            case "ACTIVITY" -> "ACTIVITY";
            case "DISCUSSION_POST_TEST" -> "POST_TEST";
            default -> throw new IllegalArgumentException("Only Discussion assessments can be submitted here.");
        };
    }

    private Student findActiveStudent(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("studentId must be a positive ID.");
        }
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));
        if (!student.isActive()) {
            throw new IllegalStateException("Inactive students cannot access assessments.");
        }
        return student;
    }

    private Student findActiveStudentForUpdate(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("studentId must be a positive ID.");
        }
        Student student = studentRepository.findByIdForUpdate(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));
        if (!student.isActive()) {
            throw new IllegalStateException("Inactive students cannot access assessments.");
        }
        return student;
    }

    private Assessment findAssessment(Long assessmentId) {
        if (assessmentId == null || assessmentId <= 0) {
            throw new IllegalArgumentException("assessmentId must be a positive ID.");
        }
        return assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assessment not found: " + assessmentId));
    }

    private AssessmentAttempt findAttempt(Long attemptId) {
        if (attemptId == null || attemptId <= 0) {
            throw new IllegalArgumentException("attemptId must be a positive ID.");
        }
        return attemptRepository.findById(attemptId)
            .orElseThrow(() -> new EntityNotFoundException("Assessment attempt not found: " + attemptId));
    }

    private void validateDiscussionAssessment(Assessment assessment) {
        if (assessment.getDiscussion() == null || assessment.getCompetency() != null
            || !DISCUSSION_TYPES.contains(assessment.getAssessmentType())) {
            throw new IllegalArgumentException("Only Discussion assessments are handled by this service.");
        }
    }

    private void requireAssessmentStageAvailable(Long studentId, Assessment assessment) {
        String stageCode = switch (assessment.getAssessmentType()) {
            case "PRE_TEST" -> "PRE_TEST";
            case "ACTIVITY" -> "ACTIVITY";
            case "DISCUSSION_POST_TEST" -> "POST_TEST";
            default -> throw new IllegalArgumentException("Unsupported Discussion assessment type.");
        };
        List<StudentStageProgress> rows = stageProgressRepository.findByStudent_IdAndDiscussion_Id(
            studentId, assessment.getDiscussion().getId());
        boolean available = rows.stream().anyMatch(row -> stageCode.equals(row.getStageCode())
            && (AVAILABLE.equals(row.getStatus()) || "IN_PROGRESS".equals(row.getStatus())));
        if (!available) {
            throw new IllegalStateException("The assessment stage is not currently available.");
        }
    }

    private void requireAttemptOwner(Long studentId, AssessmentAttempt attempt) {
        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new EntityNotFoundException("Assessment attempt not found: " + attempt.getId());
        }
        validateDiscussionAssessment(attempt.getAssessment());
    }

    private void requireInProgress(AssessmentAttempt attempt) {
        if (!IN_PROGRESS.equals(attempt.getStatus())) {
            throw new IllegalStateException("This assessment attempt is no longer in progress.");
        }
    }

    private AttemptSummary toSummary(AssessmentAttempt attempt) {
        Assessment assessment = attempt.getAssessment();
        return new AttemptSummary(
            attempt.getId(), assessment.getId(), assessment.getAssessmentType(), assessment.getTitle(),
            assessment.getInstructions(), attempt.getAttemptNumber(), attempt.getStatus(), attempt.getStartedAt());
    }

    private ChoicePrompt toChoicePrompt(AnswerChoice choice) {
        return new ChoicePrompt(choice.getId(), choice.getChoiceText(), choice.getDisplayOrder());
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public record AttemptSummary(
        Long attemptId,
        Long assessmentId,
        String assessmentType,
        String title,
        String instructions,
        int attemptNumber,
        String status,
        LocalDateTime startedAt
    ) {}

    public record QuestionPrompt(
        Long questionId,
        String questionType,
        String questionText,
        BigDecimal points,
        long displayOrder,
        List<ChoicePrompt> choices,
        List<String> matchingPrompts,
        String questionConfig
    ) {
        public QuestionPrompt(Long questionId, String questionType, String questionText,
                              BigDecimal points, long displayOrder, List<ChoicePrompt> choices) {
            this(questionId, questionType, questionText, points, displayOrder, choices, List.of(), null);
        }
        public QuestionPrompt(Long questionId, String questionType, String questionText,
                              BigDecimal points, long displayOrder, List<ChoicePrompt> choices,
                              List<String> matchingPrompts) {
            this(questionId, questionType, questionText, points, displayOrder, choices, matchingPrompts, null);
        }
    }

    public record ChoicePrompt(Long choiceId, String choiceText, int displayOrder) {}

    public record SavedAnswer(Long questionId, String responseData) {}

    public record IncorrectAnswer(
        Long questionId,
        String questionText,
        String responseData,
        String correctAnswerData,
        BigDecimal awardedPoints
    ) {}

    public record SubmissionResult(
        Long attemptId,
        String assessmentType,
        BigDecimal score,
        BigDecimal totalPossibleScore,
        BigDecimal percentage,
        LocalDateTime submittedAt,
        List<IncorrectAnswer> incorrectlyAnsweredQuestions
    ) {}

    record ScoredAnswer(boolean correct, BigDecimal awardedPoints) {}
}
