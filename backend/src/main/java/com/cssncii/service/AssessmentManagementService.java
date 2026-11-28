package com.cssncii.service;

import com.cssncii.entity.AnswerChoice;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Question;
import com.cssncii.dto.AssessmentManagementDtos.QuestionConfigInput;
import com.cssncii.dto.AssessmentManagementDtos.MatchingOptionInput;
import com.cssncii.dto.AssessmentManagementDtos.MatchingPairInput;
import com.cssncii.repository.AnswerChoiceRepository;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.AttemptAnswerRepository;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessmentManagementService {
    private static final long MAX_UNSIGNED_INT = 4_294_967_295L;
    private static final int MAX_UNSIGNED_SMALLINT = 65_535;
    private static final BigDecimal GENERALIZED_PASS_PERCENTAGE = new BigDecimal("70.00");
    private static final List<String> DISCUSSION_ASSESSMENT_TYPES =
        List.of("PRE_TEST", "ACTIVITY", "DISCUSSION_POST_TEST");
    private static final Set<String> SUPPORTED_QUESTION_TYPES =
        Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "MULTIPLE_SELECT", "MATCHING_TYPE", "ENUMERATION");
    private static final Set<String> LEGACY_QUESTION_TYPES =
        Set.of("MULTIPLE_CHOICE", "MULTIPLE_SELECT", "TRUE_FALSE");
    private static final String GENERALIZED_POST_TEST = "GENERALIZED_POST_TEST";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final AnswerChoiceRepository answerChoiceRepository;
    private final DiscussionRepository discussionRepository;
    private final CompetencyRepository competencyRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;

    public AssessmentManagementService(
        AssessmentRepository assessmentRepository,
        QuestionRepository questionRepository,
        AnswerChoiceRepository answerChoiceRepository,
        DiscussionRepository discussionRepository,
        CompetencyRepository competencyRepository,
        AttemptAnswerRepository attemptAnswerRepository,
        AssessmentAttemptRepository assessmentAttemptRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.answerChoiceRepository = answerChoiceRepository;
        this.discussionRepository = discussionRepository;
        this.competencyRepository = competencyRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
    }

    @Transactional
    public Assessment createAssessment(
        String assessmentType, Long discussionId, Long competencyId, String title, String instructions, boolean active
    ) {
        String type = normalizeAssessmentType(assessmentType);
        validateScope(type, discussionId, competencyId);
        Discussion discussion = discussionId == null ? null : findDiscussion(discussionId);
        Competency competency = competencyId == null ? null : findCompetency(competencyId);
        ensureAssessmentTypeAvailable(type, discussionId, competencyId, null);

        LocalDateTime now = utcNow();
        Assessment assessment = new Assessment();
        assessment.setAssessmentType(type);
        assessment.setDiscussion(discussion);
        assessment.setCompetency(competency);
        assessment.setTitle(requiredText("title", title, 200));
        assessment.setInstructions(optionalText(instructions));
        assessment.setPassPercentage(GENERALIZED_POST_TEST.equals(type)
            ? GENERALIZED_PASS_PERCENTAGE : null);
        // Creation always produces a Draft. Activation is a separate Admin update operation.
        assessment.setActive(false);
        assessment.setCreatedAt(now);
        assessment.setUpdatedAt(now);
        return assessmentRepository.save(assessment);
    }

    public List<Assessment> getAssessmentsByDiscussion(Long discussionId) {
        findDiscussion(discussionId);
        return assessmentRepository.findByDiscussion_IdOrderByAssessmentTypeAsc(discussionId);
    }

    public List<Assessment> getAssessmentsByCompetency(Long competencyId) {
        findCompetency(competencyId);
        return assessmentRepository.findByCompetency_IdOrderByAssessmentTypeAsc(competencyId);
    }

    public Assessment getAssessmentById(Long assessmentId) {
        return findAssessment(assessmentId);
    }

    @Transactional
    public Assessment updateAssessment(Long assessmentId, String title, String instructions, boolean active) {
        Assessment assessment = findAssessment(assessmentId);
        assessment.setTitle(requiredText("title", title, 200));
        assessment.setInstructions(optionalText(instructions));
        assessment.setActive(active);
        assessment.setUpdatedAt(utcNow());
        return assessmentRepository.save(assessment);
    }

    @Transactional
    public Question createQuestion(
        Long assessmentId,
        String questionType,
        String questionText,
        String correctAnswerData,
        BigDecimal points,
        long displayOrder
    ) {
        Assessment assessment = findAssessment(assessmentId);
        String type = normalizeQuestionType(questionType);
        if (!LEGACY_QUESTION_TYPES.contains(type)) {
            throw new IllegalArgumentException("New question types must use configured question creation.");
        }
        validateQuestion(type, questionText, correctAnswerData, points, displayOrder);
        ensureQuestionOrderAvailable(assessmentId, displayOrder, null);

        LocalDateTime now = utcNow();
        Question question = new Question();
        question.setAssessment(assessment);
        question.setQuestionType(type);
        question.setQuestionText(requiredBody("questionText", questionText));
        question.setCorrectAnswerData(requiredBody("correctAnswerData", correctAnswerData));
        question.setPoints(points);
        question.setDisplayOrder(displayOrder);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);
        return questionRepository.save(question);
    }

    /** Creates the question, choices, and final answer key in one transaction. */
    @Transactional
    public ConfiguredQuestion createQuestionWithChoices(
        Long assessmentId,
        String questionType,
        String questionText,
        BigDecimal points,
        long displayOrder,
        List<ChoiceDefinition> choiceDefinitions,
        List<Integer> correctChoiceIndexes,
        Boolean correctValue
    ) {
        return createQuestionWithChoices(assessmentId, questionType, questionText, points, displayOrder,
            choiceDefinitions, correctChoiceIndexes, correctValue, null);
    }

    @Transactional
    public ConfiguredQuestion createQuestionWithChoices(
        Long assessmentId,
        String questionType,
        String questionText,
        BigDecimal points,
        long displayOrder,
        List<ChoiceDefinition> choiceDefinitions,
        List<Integer> correctChoiceIndexes,
        Boolean correctValue,
        QuestionConfigInput questionConfig
    ) {
        Assessment assessment = findAssessment(assessmentId);
        String type = normalizeQuestionType(questionType);
        if ("ACTIVITY".equals(assessment.getAssessmentType()) && isNewQuestionType(type)) {
            throw new IllegalArgumentException("New automatically graded question types are not available for Activities.");
        }
        validateQuestion(type, questionText, "{}", points, displayOrder);
        ensureQuestionOrderAvailable(assessmentId, displayOrder, null);
        List<ChoiceDefinition> definitions = choiceDefinitions == null ? List.of() : choiceDefinitions;
        List<Integer> correctIndexes = correctChoiceIndexes == null ? List.of() : correctChoiceIndexes;

        if (isNewQuestionType(type)) {
            validateNewQuestionConfiguration(type, questionConfig);
            if (correctValue != null || !correctIndexes.isEmpty()) {
                throw new IllegalArgumentException("New question types use their type-specific answer configuration.");
            }
            if ("MATCHING_TYPE".equals(type) && definitions.size() < questionConfig.matchingPairs().size()) {
                throw new IllegalArgumentException("Matching questions require an answer option for every pair.");
            }
            if (!"MATCHING_TYPE".equals(type) && !definitions.isEmpty()) {
                throw new IllegalArgumentException("Only Matching Type questions use answer choices.");
            }
        } else if ("TRUE_FALSE".equals(type)) {
            if (!definitions.isEmpty() || !correctIndexes.isEmpty() || correctValue == null) {
                throw new IllegalArgumentException("TRUE_FALSE questions require a correctValue and no answer choices.");
            }
        } else {
            if (correctValue != null || definitions.size() < 2) {
                throw new IllegalArgumentException("Choice questions require at least two answer choices and no correctValue.");
            }
            if (correctIndexes.isEmpty() || ("MULTIPLE_CHOICE".equals(type) && correctIndexes.size() != 1)) {
                throw new IllegalArgumentException("Select exactly one correct answer for MULTIPLE_CHOICE and at least one for MULTIPLE_SELECT.");
            }
            if (correctIndexes.stream().anyMatch(index -> index == null || index < 0 || index >= definitions.size())
                || correctIndexes.stream().distinct().count() != correctIndexes.size()) {
                throw new IllegalArgumentException("Correct answer indexes must identify distinct existing choices.");
            }
        }

        LocalDateTime now = utcNow();
        Question question = new Question();
        question.setAssessment(assessment);
        question.setQuestionType(type);
        question.setQuestionText(requiredBody("questionText", questionText));
        question.setCorrectAnswerData("TRUE_FALSE".equals(type) ? "{\"value\":" + correctValue + "}" : "{}");
        question.setQuestionConfig(serializeQuestionConfig(type, questionConfig));
        question.setPoints(points);
        question.setDisplayOrder(displayOrder);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);
        question = questionRepository.save(question);

        java.util.ArrayList<AnswerChoice> choices = new java.util.ArrayList<>();
        for (ChoiceDefinition definition : definitions) {
            choices.add(createAnswerChoice(question.getId(), definition.choiceText(), definition.displayOrder()));
        }
        List<Long> correctIds = correctIndexes.stream().map(choices::get).map(AnswerChoice::getId).toList();
        if ("ENUMERATION".equals(type)) {
            question.setCorrectAnswerData(writeJson(Map.of("acceptedAnswers", questionConfig.acceptedAnswers())));
        } else if ("MATCHING_TYPE".equals(type)) {
            List<Map<String, Object>> pairs = new ArrayList<>();
            for (int i = 0; i < questionConfig.matchingPairs().size(); i++) {
                String answerText = normalizeAnswer(questionConfig.matchingPairs().get(i).answerText());
                AnswerChoice correctChoice = choices.stream()
                    .filter(choice -> normalizeAnswer(choice.getChoiceText()).equals(answerText)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Each matching pair answer must be present in the shared options."));
                pairs.add(Map.of("promptIndex", i, "choiceId", correctChoice.getId()));
            }
            question.setCorrectAnswerData(writeJson(Map.of("matches", pairs)));
        } else if ("MULTIPLE_CHOICE".equals(type)) {
            question.setCorrectAnswerData("{\"choiceId\":" + correctIds.getFirst() + "}");
        } else if ("MULTIPLE_SELECT".equals(type)) {
            question.setCorrectAnswerData("{\"choiceIds\":["
                + String.join(",", correctIds.stream().map(String::valueOf).toList()) + "]}");
        }
        question.setUpdatedAt(utcNow());
        return new ConfiguredQuestion(questionRepository.save(question), List.copyOf(choices));
    }

    public List<Question> getQuestionsByAssessment(Long assessmentId) {
        findAssessment(assessmentId);
        return questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(assessmentId);
    }

    public Question getQuestionById(Long questionId) {
        return findQuestion(questionId);
    }

    @Transactional
    public Question updateQuestion(
        Long questionId,
        String questionType,
        String questionText,
        String correctAnswerData,
        BigDecimal points,
        long displayOrder
    ) {
        Question question = findQuestion(questionId);
        validateQuestion(questionType, questionText, correctAnswerData, points, displayOrder);
        ensureQuestionOrderAvailable(question.getAssessment().getId(), displayOrder, questionId);

        question.setQuestionType(normalizeQuestionType(questionType));
        question.setQuestionText(requiredBody("questionText", questionText));
        question.setCorrectAnswerData(requiredBody("correctAnswerData", correctAnswerData));
        question.setPoints(points);
        question.setDisplayOrder(displayOrder);
        question.setUpdatedAt(utcNow());
        return questionRepository.save(question);
    }

    @Transactional
    public Question updateQuestionConfiguration(Long questionId, String questionText, BigDecimal points,
                                                long displayOrder, QuestionConfigInput configuration,
                                                String correctAnswerData, String questionConfig) {
        Question question = findQuestion(questionId);
        String type = question.getQuestionType();
        if (!isNewQuestionType(type)) throw new IllegalArgumentException("Only new question types use this update path.");
        validateNewQuestionConfiguration(type, configuration);
        if (assessmentAttemptRepository.existsByAssessment_Id(question.getAssessment().getId())) {
            throw new IllegalStateException("Question configuration cannot be changed after student attempts exist.");
        }
        validateQuestion(type, questionText, correctAnswerData, points, displayOrder);
        ensureQuestionOrderAvailable(question.getAssessment().getId(), displayOrder, questionId);
        if ("MATCHING_TYPE".equals(type)) {
            List<AnswerChoice> existing = answerChoiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(questionId);
            if (existing.size() != configuration.matchingPairs().size() + configuration.distractors().size()) {
                throw new IllegalArgumentException("Save matching option rows before saving this configuration.");
            }
        }
        question.setQuestionText(requiredBody("questionText", questionText));
        question.setCorrectAnswerData(requiredBody("correctAnswerData", correctAnswerData));
        question.setQuestionConfig(requiredBody("questionConfig", questionConfig));
        question.setPoints(points);
        question.setDisplayOrder(displayOrder);
        question.setUpdatedAt(utcNow());
        return questionRepository.save(question);
    }

    @Transactional
    public Question updateMatchingQuestionConfiguration(Long questionId, String questionText, BigDecimal points,
                                                         long displayOrder, QuestionConfigInput configuration) {
        Question question = findQuestion(questionId);
        if (!"MATCHING_TYPE".equals(question.getQuestionType())) {
            throw new IllegalArgumentException("Only Matching Type questions use this update path.");
        }
        validateNewQuestionConfiguration("MATCHING_TYPE", configuration);
        if (assessmentAttemptRepository.existsByAssessment_Id(question.getAssessment().getId())) {
            throw new IllegalStateException("Question configuration cannot be changed after student attempts exist.");
        }
        validateQuestion("MATCHING_TYPE", questionText, "{}", points, displayOrder);
        ensureQuestionOrderAvailable(question.getAssessment().getId(), displayOrder, questionId);

        List<AnswerChoice> existing = answerChoiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(questionId);
        List<MatchingOptionInput> requestedOptions = matchingOptionsForUpdate(configuration, existing);
        Map<Long, AnswerChoice> existingById = existing.stream()
            .collect(java.util.stream.Collectors.toMap(AnswerChoice::getId, choice -> choice));
        Set<Long> retainedIds = new HashSet<>();
        Set<String> optionTexts = new HashSet<>();
        Map<String, AnswerChoice> choicesByText = new HashMap<>();
        int nextOrder = existing.stream().mapToInt(AnswerChoice::getDisplayOrder).max().orElse(0) + 1;

        for (MatchingOptionInput option : requestedOptions) {
            String text = requiredText("matching option", option.optionText(), 65_535);
            String normalized = normalizeAnswer(text);
            if (!optionTexts.add(normalized)) {
                throw new IllegalArgumentException("Matching options must be unique after whitespace and case normalization.");
            }
            AnswerChoice choice;
            if (option.id() == null) {
                choice = new AnswerChoice();
                choice.setQuestion(question);
                choice.setChoiceText(text);
                choice.setDisplayOrder(nextOrder++);
                choice.setCreatedAt(utcNow());
                choice.setUpdatedAt(utcNow());
                choice = answerChoiceRepository.save(choice);
            } else {
                choice = existingById.get(option.id());
                if (choice == null || !retainedIds.add(option.id())) {
                    throw new IllegalArgumentException("Matching option IDs must be distinct choices belonging to this question.");
                }
                choice.setChoiceText(text);
                choice.setUpdatedAt(utcNow());
                choice = answerChoiceRepository.save(choice);
            }
            choicesByText.put(normalized, choice);
        }

        List<Map<String, Object>> matches = new ArrayList<>();
        for (int index = 0; index < configuration.matchingPairs().size(); index++) {
            MatchingPairInput pair = configuration.matchingPairs().get(index);
            AnswerChoice choice = choicesByText.get(normalizeAnswer(pair.answerText()));
            if (choice == null) {
                throw new IllegalArgumentException("Every correct matching answer must reference a shared option.");
            }
            matches.add(Map.of("promptIndex", index, "choiceId", choice.getId()));
        }
        List<AnswerChoice> removed = existing.stream().filter(choice -> !retainedIds.contains(choice.getId())).toList();
        if (!removed.isEmpty()) answerChoiceRepository.deleteAll(removed);

        question.setQuestionText(requiredBody("questionText", questionText));
        question.setCorrectAnswerData(writeJson(Map.of("matches", matches)));
        question.setQuestionConfig(writeJson(Map.of("matchingPrompts", configuration.matchingPairs().stream()
            .map(pair -> requiredText("matching prompt", pair.prompt(), 65_535)).toList())));
        question.setPoints(points);
        question.setDisplayOrder(displayOrder);
        question.setUpdatedAt(utcNow());
        return questionRepository.save(question);
    }

    private List<MatchingOptionInput> matchingOptionsForUpdate(QuestionConfigInput configuration,
                                                                List<AnswerChoice> existing) {
        if (configuration.sharedOptions() != null) return configuration.sharedOptions();
        Map<String, AnswerChoice> existingByText = existing.stream()
            .collect(java.util.stream.Collectors.toMap(choice -> normalizeAnswer(choice.getChoiceText()), choice -> choice));
        List<String> values = new ArrayList<>();
        configuration.matchingPairs().forEach(pair -> values.add(pair.answerText()));
        if (configuration.distractors() != null) values.addAll(configuration.distractors());
        List<MatchingOptionInput> options = new ArrayList<>();
        for (String value : values) {
            AnswerChoice existingChoice = existingByText.get(normalizeAnswer(value));
            options.add(new MatchingOptionInput(existingChoice == null ? null : existingChoice.getId(), value));
        }
        return options;
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = findQuestion(questionId);
        if (assessmentAttemptRepository.existsByAssessment_Id(question.getAssessment().getId())
            || attemptAnswerRepository.existsByQuestion_Id(questionId)) {
            throw new IllegalStateException("A question belonging to an assessment with student attempt history cannot be deleted.");
        }
        List<AnswerChoice> choices = answerChoiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(questionId);
        answerChoiceRepository.deleteAll(choices);
        questionRepository.delete(question);
    }

    @Transactional
    public AnswerChoice createAnswerChoice(Long questionId, String choiceText, int displayOrder) {
        Question question = findQuestion(questionId);
        String normalizedText = requiredText("choiceText", choiceText, 65_535);
        validateChoiceDisplayOrder(displayOrder);
        List<AnswerChoice> existing = answerChoiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(questionId);
        ensureChoiceOrderAvailable(existing, displayOrder, null);

        LocalDateTime now = utcNow();
        AnswerChoice choice = new AnswerChoice();
        choice.setQuestion(question);
        choice.setChoiceText(normalizedText);
        choice.setDisplayOrder(displayOrder);
        choice.setCreatedAt(now);
        choice.setUpdatedAt(now);
        return answerChoiceRepository.save(choice);
    }

    public List<AnswerChoice> getAnswerChoicesByQuestion(Long questionId) {
        findQuestion(questionId);
        return answerChoiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(questionId);
    }

    public AnswerChoice getAnswerChoiceById(Long choiceId) {
        return findAnswerChoice(choiceId);
    }

    @Transactional
    public AnswerChoice updateAnswerChoice(Long choiceId, String choiceText, int displayOrder) {
        AnswerChoice choice = findAnswerChoice(choiceId);
        String normalizedText = requiredText("choiceText", choiceText, 65_535);
        validateChoiceDisplayOrder(displayOrder);
        List<AnswerChoice> existing = answerChoiceRepository
            .findByQuestion_IdOrderByDisplayOrderAsc(choice.getQuestion().getId());
        reorderAnswerChoices(existing, choice, displayOrder);

        choice.setChoiceText(normalizedText);
        choice.setUpdatedAt(utcNow());
        return answerChoiceRepository.save(choice);
    }

    private void reorderAnswerChoices(List<AnswerChoice> existing, AnswerChoice movingChoice, int targetOrder) {
        int currentOrder = movingChoice.getDisplayOrder();
        if (currentOrder == targetOrder) {
            return;
        }

        boolean targetIsOccupied = existing.stream()
            .anyMatch(choice -> choice.getDisplayOrder() == targetOrder);
        if (!targetIsOccupied) {
            movingChoice.setDisplayOrder(targetOrder);
            return;
        }

        // Stage the affected rows in unused order slots first. This avoids transient
        // collisions with uq_answer_choices_question_order while preserving row IDs.
        List<AnswerChoice> affected = existing.stream()
            .filter(choice -> choice == movingChoice
                || (targetOrder < currentOrder
                    && choice.getDisplayOrder() >= targetOrder
                    && choice.getDisplayOrder() < currentOrder)
                || (targetOrder > currentOrder
                    && choice.getDisplayOrder() > currentOrder
                    && choice.getDisplayOrder() <= targetOrder))
            .toList();
        Map<Long, Integer> originalOrders = new HashMap<>();
        Set<Integer> usedOrders = new HashSet<>();
        for (AnswerChoice choice : existing) {
            originalOrders.put(choice.getId(), choice.getDisplayOrder());
            usedOrders.add(choice.getDisplayOrder());
        }
        List<Integer> temporaryOrders = new ArrayList<>(affected.size());
        for (int candidate = 0; candidate <= MAX_UNSIGNED_SMALLINT && temporaryOrders.size() < affected.size(); candidate++) {
            if (!usedOrders.contains(candidate)) {
                temporaryOrders.add(candidate);
            }
        }
        if (temporaryOrders.size() != affected.size()) {
            throw new IllegalStateException("Unable to safely reorder answer choices.");
        }

        for (int index = 0; index < affected.size(); index++) {
            affected.get(index).setDisplayOrder(temporaryOrders.get(index));
        }
        answerChoiceRepository.flush();

        LocalDateTime now = utcNow();
        for (AnswerChoice choice : existing) {
            int originalOrder = originalOrders.get(choice.getId());
            if (choice == movingChoice) {
                choice.setDisplayOrder(targetOrder);
            } else if (targetOrder < currentOrder
                && originalOrder >= targetOrder && originalOrder < currentOrder) {
                choice.setDisplayOrder(originalOrder + 1);
                choice.setUpdatedAt(now);
            } else if (targetOrder > currentOrder
                && originalOrder > currentOrder && originalOrder <= targetOrder) {
                choice.setDisplayOrder(originalOrder - 1);
                choice.setUpdatedAt(now);
            }
        }
    }

    @Transactional
    public void deleteAnswerChoice(Long choiceId) {
        AnswerChoice choice = findAnswerChoice(choiceId);
        if (assessmentAttemptRepository.existsByAssessment_Id(choice.getQuestion().getAssessment().getId())
            || attemptAnswerRepository.existsByQuestion_Id(choice.getQuestion().getId())) {
            throw new IllegalStateException("An answer choice belonging to an assessment with student attempt history cannot be deleted.");
        }
        answerChoiceRepository.delete(choice);
    }

    private void validateScope(String type, Long discussionId, Long competencyId) {
        if (DISCUSSION_ASSESSMENT_TYPES.contains(type)) {
            if (discussionId == null || competencyId != null) {
                throw new IllegalArgumentException(type + " must belong to a Discussion only.");
            }
        } else if (discussionId != null || competencyId == null) {
            throw new IllegalArgumentException("GENERALIZED_POST_TEST must belong to a competency only.");
        }
    }

    private String normalizeAssessmentType(String assessmentType) {
        String type = requiredText("assessmentType", assessmentType, 30).toUpperCase(Locale.ROOT);
        if (!DISCUSSION_ASSESSMENT_TYPES.contains(type) && !GENERALIZED_POST_TEST.equals(type)) {
            throw new IllegalArgumentException("assessmentType must be one of the four approved types.");
        }
        return type;
    }

    private void ensureAssessmentTypeAvailable(
        String type, Long discussionId, Long competencyId, Long excludedAssessmentId
    ) {
        Assessment existing = discussionId != null
            ? assessmentRepository.findByDiscussion_IdAndAssessmentType(discussionId, type).orElse(null)
            : assessmentRepository.findByCompetency_IdAndAssessmentType(competencyId, type).orElse(null);
        if (existing != null && !existing.getId().equals(excludedAssessmentId)) {
            throw new IllegalArgumentException("This assessment type already exists for the selected scope.");
        }
    }

    private void validateQuestion(
        String questionType,
        String questionText,
        String correctAnswerData,
        BigDecimal points,
        long displayOrder
    ) {
        normalizeQuestionType(questionType);
        requiredBody("questionText", questionText);
        requiredBody("correctAnswerData", correctAnswerData);
        if (points == null || points.signum() < 0 || points.scale() > 2 || points.precision() > 8) {
            throw new IllegalArgumentException("points must be non-negative and fit DECIMAL(8,2).");
        }
        validateDisplayOrder(displayOrder);
    }

    private String normalizeQuestionType(String questionType) {
        String normalized = requiredText("questionType", questionType, 40).toUpperCase(Locale.ROOT);
        if (!SUPPORTED_QUESTION_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported question type.");
        }
        return normalized;
    }

    private boolean isNewQuestionType(String type) {
        return Set.of("MATCHING_TYPE", "ENUMERATION").contains(type);
    }

    private void validateNewQuestionConfiguration(String type, QuestionConfigInput config) {
        if (config == null) throw new IllegalArgumentException("Question type configuration is required.");
        if ("ENUMERATION".equals(type)) {
            requireAcceptedAnswers(config.acceptedAnswers());
            int count = config.requiredAnswerCount() == null ? 0 : config.requiredAnswerCount();
            if (count < 1 || count > config.acceptedAnswers().size()) {
                throw new IllegalArgumentException("Enumeration answer count must be positive and no greater than accepted answers.");
            }
            if (config.ordered() == null || config.matchingPairs() != null || config.distractors() != null
                || config.sharedOptions() != null) {
                throw new IllegalArgumentException("Enumeration requires ordered and acceptedAnswers only.");
            }
            if (config.ordered() && count != config.acceptedAnswers().size()) {
                throw new IllegalArgumentException("Ordered Enumeration requires one accepted answer for each answer field.");
            }
        } else {
            List<com.cssncii.dto.AssessmentManagementDtos.MatchingPairInput> pairs = config.matchingPairs();
            if (pairs == null || pairs.isEmpty() || config.acceptedAnswers() != null
                || config.requiredAnswerCount() != null || config.ordered() != null) {
                throw new IllegalArgumentException("Matching Type requires matchingPairs only.");
            }
            Set<String> prompts = new HashSet<>();
            Set<String> answers = new HashSet<>();
            for (var pair : pairs) {
                String prompt = requiredText("matching prompt", pair.prompt(), 65_535);
                String answer = requiredText("matching answer", pair.answerText(), 65_535);
                if (!prompts.add(normalizeAnswer(prompt)) || !answers.add(normalizeAnswer(answer))) {
                    throw new IllegalArgumentException("Matching prompts and correct options must be unique.");
                }
            }
            List<String> distractors = config.distractors() == null ? List.of() : config.distractors();
            for (String distractor : distractors) {
                if (!answers.add(normalizeAnswer(requiredText("distractor", distractor, 65_535)))) {
                    throw new IllegalArgumentException("Matching distractors must be distinct from correct options.");
                }
            }
            if (config.sharedOptions() != null) {
                Set<String> options = new HashSet<>();
                for (MatchingOptionInput option : config.sharedOptions()) {
                    String text = normalizeAnswer(requiredText("matching option", option.optionText(), 65_535));
                    if (!options.add(text)) throw new IllegalArgumentException("Matching options must be unique after whitespace and case normalization.");
                }
                Set<String> correctOptions = pairs.stream().map(pair -> normalizeAnswer(pair.answerText()))
                    .collect(java.util.stream.Collectors.toSet());
                Set<String> distractorOptions = (distractors == null ? List.<String>of() : distractors).stream()
                    .map(this::normalizeAnswer).collect(java.util.stream.Collectors.toSet());
                if (!options.containsAll(correctOptions) || !options.containsAll(distractorOptions)
                    || options.size() != correctOptions.size() + distractorOptions.size()) {
                    throw new IllegalArgumentException("Every matching answer must exist in the shared options.");
                }
                Set<Long> optionIds = new HashSet<>();
                for (MatchingOptionInput option : config.sharedOptions()) {
                    if (option.id() != null && !optionIds.add(option.id())) {
                        throw new IllegalArgumentException("Matching option IDs must be distinct.");
                    }
                }
            }
        }
    }

    private void requireAcceptedAnswers(List<String> answers) {
        if (answers == null || answers.isEmpty() || answers.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("At least one non-blank accepted answer is required.");
        }
        Set<String> normalized = new HashSet<>();
        if (answers.stream().map(this::normalizeAnswer).anyMatch(value -> !normalized.add(value))) {
            throw new IllegalArgumentException("Accepted answers must be unique after whitespace and case normalization.");
        }
    }

    private String serializeQuestionConfig(String type, QuestionConfigInput config) {
        if ("MATCHING_TYPE".equals(type)) {
            return writeJson(Map.of("matchingPrompts", config.matchingPairs().stream()
                .map(pair -> requiredText("matching prompt", pair.prompt(), 65_535)).toList()));
        }
        if ("ENUMERATION".equals(type)) {
            return writeJson(Map.of("requiredAnswerCount", config.requiredAnswerCount(), "ordered", config.ordered()));
        }
        return "{}";
    }

    private String writeJson(Object value) {
        try { return JSON.writeValueAsString(value); }
        catch (JacksonException exception) { throw new IllegalArgumentException("Question configuration is invalid.", exception); }
    }

    private String normalizeAnswer(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private void ensureQuestionOrderAvailable(Long assessmentId, long displayOrder, Long excludedQuestionId) {
        boolean duplicate = questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(assessmentId).stream()
            .anyMatch(question -> question.getDisplayOrder() == displayOrder
                && (excludedQuestionId == null || !excludedQuestionId.equals(question.getId())));
        if (duplicate) {
            throw new IllegalArgumentException("A question already uses this display order in the assessment.");
        }
    }

    private void ensureChoiceOrderAvailable(List<AnswerChoice> existing, int displayOrder, Long excludedChoiceId) {
        boolean duplicate = existing.stream().anyMatch(choice -> choice.getDisplayOrder() == displayOrder
            && (excludedChoiceId == null || !excludedChoiceId.equals(choice.getId())));
        if (duplicate) {
            throw new IllegalArgumentException("An answer choice already uses this display order for the question.");
        }
    }

    private Assessment findAssessment(Long assessmentId) {
        requireId("assessmentId", assessmentId);
        return assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assessment not found: " + assessmentId));
    }

    private Question findQuestion(Long questionId) {
        requireId("questionId", questionId);
        return questionRepository.findById(questionId)
            .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));
    }

    private AnswerChoice findAnswerChoice(Long choiceId) {
        requireId("choiceId", choiceId);
        return answerChoiceRepository.findById(choiceId)
            .orElseThrow(() -> new EntityNotFoundException("Answer choice not found: " + choiceId));
    }

    private Discussion findDiscussion(Long discussionId) {
        requireId("discussionId", discussionId);
        return discussionRepository.findById(discussionId)
            .orElseThrow(() -> new EntityNotFoundException("Discussion not found: " + discussionId));
    }

    private Competency findCompetency(Long competencyId) {
        requireId("competencyId", competencyId);
        return competencyRepository.findById(competencyId)
            .orElseThrow(() -> new EntityNotFoundException("Competency not found: " + competencyId));
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

    private String requiredBody(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateDisplayOrder(long displayOrder) {
        if (displayOrder < 0 || displayOrder > MAX_UNSIGNED_INT) {
            throw new IllegalArgumentException("displayOrder must fit an unsigned MySQL INT.");
        }
    }

    private void validateChoiceDisplayOrder(int displayOrder) {
        if (displayOrder < 0 || displayOrder > MAX_UNSIGNED_SMALLINT) {
            throw new IllegalArgumentException("displayOrder must fit an unsigned MySQL SMALLINT.");
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

    public record ChoiceDefinition(String choiceText, int displayOrder) {}
    public record ConfiguredQuestion(Question question, List<AnswerChoice> choices) {}
}
