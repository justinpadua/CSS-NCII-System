package com.cssncii.service;

import com.cssncii.entity.AnswerChoice;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Question;
import com.cssncii.repository.AnswerChoiceRepository;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.QuestionRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Idempotent, guarded import of approved assessment Markdown through existing service validation. */
@Service
public class ApprovedAssessmentBulkImportService {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final BigDecimal ONE_POINT = BigDecimal.ONE;
    private final ApprovedAssessmentSourceParser parser;
    private final AssessmentManagementService managementService;
    private final CompetencyRepository competencyRepository;
    private final DiscussionRepository discussionRepository;
    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final AnswerChoiceRepository choiceRepository;
    private final AssessmentAttemptRepository attemptRepository;

    public ApprovedAssessmentBulkImportService(
        ApprovedAssessmentSourceParser parser, AssessmentManagementService managementService,
        CompetencyRepository competencyRepository, DiscussionRepository discussionRepository,
        AssessmentRepository assessmentRepository, QuestionRepository questionRepository,
        AnswerChoiceRepository choiceRepository, AssessmentAttemptRepository attemptRepository
    ) {
        this.parser = parser;
        this.managementService = managementService;
        this.competencyRepository = competencyRepository;
        this.discussionRepository = discussionRepository;
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.choiceRepository = choiceRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional
    public ImportSummary importApproved(Path learningContentRoot) {
        List<ApprovedAssessmentSourceParser.ParsedSet> sets = parseAll(learningContentRoot);
        Map<String, Competency> competencies = new HashMap<>();
        for (String code : List.of("COC1", "COC2")) {
            Competency competency = competencyRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Missing competency " + code));
            competencies.put(code, competency);
            int expectedDiscussions = "COC1".equals(code) ? 6 : 3;
            List<Discussion> discussions = discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(competency.getId());
            if (discussions.size() != expectedDiscussions) {
                throw new IllegalStateException(code + " has " + discussions.size() + " Discussions; expected " + expectedDiscussions);
            }
        }
        verifyExistingCoc1DiscussionOneDraft(sets, competencies.get("COC1"));

        int createdAssessments = 0;
        int createdQuestions = 0;
        int preservedQuestions = 0;
        List<String> scopes = new ArrayList<>();
        for (ApprovedAssessmentSourceParser.ParsedSet set : sets) {
            Competency competency = competencies.get(set.competencyCode());
            List<Discussion> discussions = discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(competency.getId());
            Long discussionId = "GENERALIZED_POST_TEST".equals(set.type())
                ? null : discussions.get(set.discussionOrder() - 1).getId();
            Long competencyId = discussionId == null ? competency.getId() : null;
            Assessment assessment = discussionId == null
                ? assessmentRepository.findByCompetency_IdAndAssessmentType(competencyId, set.type()).orElse(null)
                : assessmentRepository.findByDiscussion_IdAndAssessmentType(discussionId, set.type()).orElse(null);
            boolean newlyCreated = assessment == null;
            if (assessment == null) {
                String title = assessmentTitle(set);
                assessment = managementService.createAssessment(set.type(), discussionId, competencyId, title,
                    set.instructions(), false);
                createdAssessments++;
            } else {
                if (assessment.isActive()) {
                    throw new IllegalStateException("Refusing to change active assessment: " + assessment.getId()
                        + " (" + assessmentTitle(set) + "). Deactivate it separately before importing.");
                }
                if ("GENERALIZED_POST_TEST".equals(set.type())
                    && (assessment.getPassPercentage() == null
                        || assessment.getPassPercentage().compareTo(new BigDecimal("70.00")) != 0)) {
                    throw new IllegalStateException("Existing generalized assessment has a non-70% threshold: " + assessment.getId());
                }
            }

            List<Question> existingQuestions = questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(assessment.getId());
            Map<String, Question> existingByText = new HashMap<>();
            for (Question existing : existingQuestions) {
                String key = normalize(existing.getQuestionText());
                if (existingByText.put(key, existing) != null) {
                    throw new IllegalStateException("Duplicate existing question text in assessment " + assessment.getId());
                }
            }
            Set<String> expectedTexts = new HashSet<>();
            int addedForSet = 0;
            long missingCount = set.questions().stream()
                .filter(expected -> !existingByText.containsKey(normalize(expected.questionText()))).count();
            requireAttemptsResolved(assessment.getId(), missingCount, newlyCreated);
            for (int index = 0; index < set.questions().size(); index++) {
                long expectedPosition = index + 1L;
                ApprovedAssessmentSourceParser.QuestionSource expected = set.questions().get(index);
                String key = normalize(expected.questionText());
                if (!expectedTexts.add(key)) throw new IllegalStateException("Duplicate source question in " + assessmentTitle(set));
                Question existing = existingByText.get(key);
                if (existing != null) {
                    verifyExistingQuestion(assessment, existing, expected, index + 1);
                    preservedQuestions++;
                    continue;
                }
                if (existingQuestions.stream().anyMatch(q -> q.getDisplayOrder() == expectedPosition)) {
                    throw new IllegalStateException("Question order conflict in assessment " + assessment.getId()
                        + " at position " + expectedPosition);
                }
                List<AssessmentManagementService.ChoiceDefinition> choices = expected.choices().stream()
                    .map(choice -> new AssessmentManagementService.ChoiceDefinition(choice.text(),
                        "A".equals(choice.letter()) ? 1 : choice.letter().charAt(0) - 'A' + 1))
                    .toList();
                managementService.createQuestionWithChoices(assessment.getId(), "MULTIPLE_CHOICE",
                    expected.questionText(), ONE_POINT, expectedPosition, choices,
                    List.of(expected.correctChoiceIndex()), null);
                createdQuestions++;
                addedForSet++;
            }
            if (existingQuestions.size() > set.questions().size()) {
                throw new IllegalStateException("Existing assessment contains extra questions: " + assessment.getId());
            }
            verifyImportedScope(assessment, set);
            scopes.add(assessmentTitle(set) + " — " + (addedForSet == 0 ? "already complete" : "added " + addedForSet));
        }
        return new ImportSummary(sets.size(), createdAssessments, createdQuestions, preservedQuestions, List.copyOf(scopes));
    }

    void requireAttemptsResolved(Long assessmentId, long missingQuestionCount, boolean newlyCreated) {
        if (missingQuestionCount > 0 && !newlyCreated
            && attemptRepository.existsByAssessment_IdAndStatusNot(assessmentId, "SUBMITTED")) {
            throw new IllegalStateException("Refusing to extend assessment " + assessmentId
                + " while it has an unresolved attempt.");
        }
    }

    private void verifyImportedScope(Assessment assessment, ApprovedAssessmentSourceParser.ParsedSet set) {
        if (assessment.isActive()) throw new IllegalStateException("Imported assessment is unexpectedly active: " + assessment.getId());
        if ("GENERALIZED_POST_TEST".equals(set.type())
            && (assessment.getPassPercentage() == null
                || assessment.getPassPercentage().compareTo(new BigDecimal("70.00")) != 0)) {
            throw new IllegalStateException("Generalized Post-test must retain the 70% passing threshold.");
        }
        List<Question> saved = questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(assessment.getId());
        if (saved.size() != set.questions().size()) {
            throw new IllegalStateException("Postflight question count mismatch for assessment " + assessment.getId());
        }
        for (int i = 0; i < saved.size(); i++) {
            verifyExistingQuestion(assessment, saved.get(i), set.questions().get(i), i + 1);
        }
    }

    private void verifyExistingCoc1DiscussionOneDraft(
        List<ApprovedAssessmentSourceParser.ParsedSet> sets, Competency coc1
    ) {
        Discussion firstDiscussion = discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(coc1.getId()).getFirst();
        Assessment assessment = assessmentRepository.findByDiscussion_IdAndAssessmentType(firstDiscussion.getId(), "PRE_TEST")
            .orElseThrow(() -> new IllegalStateException("Expected existing COC 1 Discussion 1 Pre-test draft is missing."));
        if (!assessment.getId().equals(1L)) {
            throw new IllegalStateException("Expected COC 1 Discussion 1 Pre-test to be Assessment ID 1.");
        }
        if (assessment.isActive()) {
            throw new IllegalStateException("Assessment ID 1 must remain Draft/Inactive; import stopped.");
        }
        List<Question> existing = questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(assessment.getId());
        if (existing.size() != 2) {
            throw new IllegalStateException("Assessment ID 1 must contain exactly the two existing questions before import.");
        }
        ApprovedAssessmentSourceParser.ParsedSet approved = sets.stream()
            .filter(set -> "COC1".equals(set.competencyCode()) && set.discussionOrder() == 1
                && "PRE_TEST".equals(set.type()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Approved COC 1 Discussion 1 Pre-test source is missing."));
        for (int i = 0; i < existing.size(); i++) {
            verifyExistingQuestion(assessment, existing.get(i), approved.questions().get(i), i + 1);
        }
    }

    private List<ApprovedAssessmentSourceParser.ParsedSet> parseAll(Path root) {
        List<ApprovedAssessmentSourceParser.ParsedSet> sets = new ArrayList<>();
        try {
            for (String code : List.of("COC1", "COC2")) {
                int discussionCount = "COC1".equals(code) ? 6 : 3;
                for (int order = 1; order <= discussionCount; order++) {
                    for (String type : List.of("PRE_TEST", "DISCUSSION_POST_TEST")) {
                        sets.add(parser.parse(root, code, order, type));
                    }
                }
                sets.add(parser.parse(root, code, 0, "GENERALIZED_POST_TEST"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not read approved assessment sources.", exception);
        }
        int questionTotal = sets.stream().mapToInt(set -> set.questions().size()).sum();
        if (sets.size() != 20 || questionTotal != 233) {
            throw new IllegalStateException("Source inventory mismatch: expected 20 assessment sets and 233 questions; found "
                + sets.size() + " sets and " + questionTotal + " questions.");
        }
        return List.copyOf(sets);
    }

    private void verifyExistingQuestion(Assessment assessment, Question existing,
        ApprovedAssessmentSourceParser.QuestionSource expected, int expectedOrder) {
        if (!"MULTIPLE_CHOICE".equals(existing.getQuestionType()) || existing.getDisplayOrder() != expectedOrder
            || existing.getPoints().compareTo(ONE_POINT) != 0) {
            throw new IllegalStateException("Existing question does not match approved type/order/points in assessment " + assessment.getId());
        }
        List<AnswerChoice> choices = choiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(existing.getId());
        if (choices.size() != expected.choices().size()) throw new IllegalStateException("Existing choice count conflicts with source in assessment " + assessment.getId());
        for (int i = 0; i < choices.size(); i++) {
            AnswerChoice choice = choices.get(i);
            ApprovedAssessmentSourceParser.ChoiceSource source = expected.choices().get(i);
            if (choice.getDisplayOrder() != i + 1 || !choice.getChoiceText().equals(source.text())) {
                throw new IllegalStateException("Existing choices conflict with approved source in assessment " + assessment.getId());
            }
        }
        Set<Long> expectedCorrect = Set.of(choices.get(expected.correctChoiceIndex()).getId());
        if (!readCorrectIds(existing.getCorrectAnswerData()).equals(expectedCorrect)) {
            throw new IllegalStateException("Existing correct answer conflicts with approved source in assessment " + assessment.getId());
        }
    }

    private Set<Long> readCorrectIds(String data) {
        try {
            JsonNode node = JSON.readTree(data);
            if (node.has("choiceId")) return Set.of(node.get("choiceId").longValue());
            if (node.has("choiceIds") && node.get("choiceIds").isArray()) {
                Set<Long> ids = new HashSet<>();
                node.get("choiceIds").forEach(value -> ids.add(value.longValue()));
                return ids;
            }
            return Set.of();
        } catch (JacksonException exception) {
            throw new IllegalStateException("Existing assessment answer key is not valid JSON.", exception);
        }
    }

    private String assessmentTitle(ApprovedAssessmentSourceParser.ParsedSet set) {
        return "GENERALIZED_POST_TEST".equals(set.type())
            ? set.cocName() + " Generalized Post-test"
            : set.cocName() + " Discussion " + set.discussionOrder() + " "
                + ("PRE_TEST".equals(set.type()) ? "Pre-test" : "Discussion Post-test");
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public record ImportSummary(int assessmentScopes, int assessmentsCreated, int questionsCreated,
                                int questionsPreserved, List<String> scopeResults) {}
}
