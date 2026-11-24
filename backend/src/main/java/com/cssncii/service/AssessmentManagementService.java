package com.cssncii.service;

import com.cssncii.entity.AnswerChoice;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Question;
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
        Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "MULTIPLE_SELECT");
    private static final String GENERALIZED_POST_TEST = "GENERALIZED_POST_TEST";

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
        assessment.setActive(active);
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
        validateQuestion(questionType, questionText, correctAnswerData, points, displayOrder);
        ensureQuestionOrderAvailable(assessmentId, displayOrder, null);

        LocalDateTime now = utcNow();
        Question question = new Question();
        question.setAssessment(assessment);
        question.setQuestionType(normalizeQuestionType(questionType));
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
        Assessment assessment = findAssessment(assessmentId);
        String type = normalizeQuestionType(questionType);
        validateQuestion(type, questionText, "{}", points, displayOrder);
        ensureQuestionOrderAvailable(assessmentId, displayOrder, null);
        List<ChoiceDefinition> definitions = choiceDefinitions == null ? List.of() : choiceDefinitions;
        List<Integer> correctIndexes = correctChoiceIndexes == null ? List.of() : correctChoiceIndexes;

        if ("TRUE_FALSE".equals(type)) {
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
        if ("MULTIPLE_CHOICE".equals(type)) {
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
            throw new IllegalArgumentException("questionType must be MULTIPLE_CHOICE, TRUE_FALSE, or MULTIPLE_SELECT.");
        }
        return normalized;
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
