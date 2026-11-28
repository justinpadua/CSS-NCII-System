package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cssncii.entity.AnswerChoice;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Question;
import com.cssncii.dto.AssessmentManagementDtos.MatchingPairInput;
import com.cssncii.dto.AssessmentManagementDtos.MatchingOptionInput;
import com.cssncii.dto.AssessmentManagementDtos.QuestionConfigInput;
import com.cssncii.repository.AnswerChoiceRepository;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.AttemptAnswerRepository;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssessmentManagementServiceTest {
    private AssessmentManagementService service;
    private AssessmentRepository assessmentRepository;
    private QuestionRepository questionRepository;
    private AnswerChoiceRepository choiceRepository;
    private AttemptAnswerRepository attemptAnswerRepository;
    private AssessmentAttemptRepository assessmentAttemptRepository;
    private DiscussionRepository discussionRepository;
    private CompetencyRepository competencyRepository;
    private Assessment assessment;
    private Question savedQuestion;
    private final List<AnswerChoice> savedChoices = new ArrayList<>();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        questionRepository = mock(QuestionRepository.class);
        choiceRepository = mock(AnswerChoiceRepository.class);
        attemptAnswerRepository = mock(AttemptAnswerRepository.class);
        assessmentAttemptRepository = mock(AssessmentAttemptRepository.class);
        discussionRepository = mock(DiscussionRepository.class);
        competencyRepository = mock(CompetencyRepository.class);
        assessment = new Assessment(); assessment.setId(8L); assessment.setAssessmentType("PRE_TEST");
        when(assessmentRepository.findById(8L)).thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(8L)).thenReturn(List.of());
        when(choiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(21L)).thenAnswer(call -> List.copyOf(savedChoices));
        when(questionRepository.save(any(Question.class))).thenAnswer(call -> {
            Question question = call.getArgument(0); question.setId(21L); savedQuestion = question; return question;
        });
        when(questionRepository.findById(21L)).thenAnswer(call -> Optional.ofNullable(savedQuestion));
        when(choiceRepository.save(any(AnswerChoice.class))).thenAnswer(call -> {
            AnswerChoice choice = call.getArgument(0);
            if (choice.getId() == null) choice.setId(30L + savedChoices.size());
            if (!savedChoices.contains(choice)) savedChoices.add(choice);
            return choice;
        });
        doAnswer(call -> {
            Iterable<AnswerChoice> removed = call.getArgument(0);
            removed.forEach(savedChoices::remove);
            return null;
        }).when(choiceRepository).deleteAll(any(Iterable.class));
        service = new AssessmentManagementService(
            assessmentRepository, questionRepository, choiceRepository,
            discussionRepository, competencyRepository, attemptAnswerRepository,
            assessmentAttemptRepository);
    }

    @Test
    void newAssessmentEntityDefaultsToDraft() {
        assertEquals(false, new Assessment().isActive());
    }

    @Test
    void createsDiscussionAssessmentAsDraftEvenWhenCallerRequestsActive() {
        Discussion discussion = new Discussion();
        discussion.setId(41L);
        when(discussionRepository.findById(41L)).thenReturn(Optional.of(discussion));
        when(assessmentRepository.findByDiscussion_IdAndAssessmentType(41L, "PRE_TEST"))
            .thenReturn(Optional.empty());
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(call -> call.getArgument(0));

        Assessment created = service.createAssessment("PRE_TEST", 41L, null, "Pre-test", null, true);

        assertEquals(false, created.isActive());
        verify(assessmentRepository).save(argThat(value -> !value.isActive()));
    }

    @Test
    void legacyQuestionCreationRejectsTypesThatRequireConfiguration() {
        for (String type : List.of("MATCHING_TYPE", "ENUMERATION")) {
            assertThrows(IllegalArgumentException.class, () -> service.createQuestion(
                8L, type, "Prompt", "{}", BigDecimal.ONE, 1));
        }
        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    void generalizedPostTestCanBeCreatedForCoc1AndCoc2Competencies() {
        for (long competencyId : List.of(2L, 3L)) {
            Competency competency = new Competency();
            competency.setId(competencyId);
            competency.setCode(competencyId == 2L ? "COC1" : "COC2");
            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(competency));
            when(assessmentRepository.findByCompetency_IdAndAssessmentType(competencyId, "GENERALIZED_POST_TEST"))
                .thenReturn(Optional.empty());
        }
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(call -> call.getArgument(0));

        Assessment coc1Assessment = service.createAssessment("GENERALIZED_POST_TEST", null, 2L,
            "COC 1 Generalized Post-test", null, false);
        Assessment coc2Assessment = service.createAssessment("GENERALIZED_POST_TEST", null, 3L,
            "COC 2 Generalized Post-test", null, false);

        assertEquals("COC1", coc1Assessment.getCompetency().getCode());
        assertEquals("COC2", coc2Assessment.getCompetency().getCode());
    }

    @Test
    void createsMatchingPairsAndStoresPromptsSeparatelyFromCorrectMappings() {
        QuestionConfigInput config = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU", "Processor"), new MatchingPairInput("RAM", "Memory")),
            List.of("Storage"));
        var created = service.createQuestionWithChoices(8L, "MATCHING_TYPE", "Match each item.",
            new BigDecimal("5.00"), 1,
            List.of(new AssessmentManagementService.ChoiceDefinition("Processor", 1),
                new AssessmentManagementService.ChoiceDefinition("Memory", 2),
                new AssessmentManagementService.ChoiceDefinition("Storage", 3)), List.of(), null, config);

        assertEquals("{\"matchingPrompts\":[\"CPU\",\"RAM\"]}", created.question().getQuestionConfig());
        assertTrue(created.question().getCorrectAnswerData().contains("\"matches\""));
        assertTrue(created.question().getCorrectAnswerData().contains("\"choiceId\":30"));
        assertEquals(3, created.choices().size());
    }

    @Test
    void updatesMatchingPromptsOptionsMappingsAndSupportsDifferentOptionAndPairCounts() {
        QuestionConfigInput original = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU", "Processor"), new MatchingPairInput("RAM", "Memory")),
            List.of("Storage"));
        Question question = service.createQuestionWithChoices(8L, "MATCHING_TYPE", "Match each item.",
            new BigDecimal("5.00"), 1,
            List.of(new AssessmentManagementService.ChoiceDefinition("Processor", 1),
                new AssessmentManagementService.ChoiceDefinition("Memory", 2),
                new AssessmentManagementService.ChoiceDefinition("Storage", 3)), List.of(), null, original).question();

        QuestionConfigInput edited = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU core", "Central Processor"),
                new MatchingPairInput("RAM", "Memory"),
                new MatchingPairInput("Disk", "Storage")),
            List.of("Spare"), List.of(new MatchingOptionInput(30L, "Central Processor"),
                new MatchingOptionInput(31L, "Memory"), new MatchingOptionInput(32L, "Storage"),
                new MatchingOptionInput(null, "Spare")));
        Question updated = service.updateMatchingQuestionConfiguration(question.getId(), "Match updated items.",
            new BigDecimal("5.00"), 1, edited);

        assertEquals("{\"matchingPrompts\":[\"CPU core\",\"RAM\",\"Disk\"]}", updated.getQuestionConfig());
        assertTrue(updated.getCorrectAnswerData().contains("\"choiceId\":30"));
        assertTrue(updated.getCorrectAnswerData().contains("\"choiceId\":31"));
        assertTrue(updated.getCorrectAnswerData().contains("\"choiceId\":32"));
        assertEquals(List.of("Central Processor", "Memory", "Storage", "Spare"),
            savedChoices.stream().map(AnswerChoice::getChoiceText).toList());

        QuestionConfigInput reduced = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU core", "Central Processor"),
                new MatchingPairInput("Disk", "Storage")), List.of(),
            List.of(new MatchingOptionInput(30L, "Central Processor"),
                new MatchingOptionInput(32L, "Storage")));
        Question reducedQuestion = service.updateMatchingQuestionConfiguration(question.getId(), "Match two items.",
            new BigDecimal("5.00"), 1, reduced);
        assertEquals("{\"matchingPrompts\":[\"CPU core\",\"Disk\"]}", reducedQuestion.getQuestionConfig());
        verify(choiceRepository).deleteAll(argThat((Iterable<AnswerChoice> removed) ->
            java.util.stream.StreamSupport.stream(removed.spliterator(), false)
                .map(AnswerChoice::getId).collect(java.util.stream.Collectors.toSet()).equals(Set.of(31L, 33L))));

        AssessmentAttemptService grading = new AssessmentAttemptService(mock(com.cssncii.repository.StudentRepository.class),
            assessmentRepository, assessmentAttemptRepository, attemptAnswerRepository, questionRepository, choiceRepository,
            mock(com.cssncii.repository.StudentStageProgressRepository.class), mock(StudentProgressService.class));
        var partial = grading.scoreResponse(reducedQuestion,
            "{\"matches\":[{\"promptIndex\":0,\"choiceId\":30},{\"promptIndex\":1,\"choiceId\":null}]}",
            savedChoices);
        assertEquals(new BigDecimal("2.50"), partial.awardedPoints());
    }

    @Test
    void matchingUpdatesRejectMissingMappingsDuplicatePromptsAndForeignOptionIds() {
        QuestionConfigInput original = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU", "Processor")), List.of("Storage"));
        Question question = service.createQuestionWithChoices(8L, "MATCHING_TYPE", "Match.", BigDecimal.ONE, 1,
            List.of(new AssessmentManagementService.ChoiceDefinition("Processor", 1),
                new AssessmentManagementService.ChoiceDefinition("Storage", 2)), List.of(), null, original).question();

        QuestionConfigInput missingOption = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU", "Missing")), List.of(),
            List.of(new MatchingOptionInput(30L, "Processor")));
        assertThrows(IllegalArgumentException.class, () -> service.updateMatchingQuestionConfiguration(
            question.getId(), "Match.", BigDecimal.ONE, 1, missingOption));

        QuestionConfigInput duplicatePrompt = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU", "Processor"), new MatchingPairInput(" cpu ", "Storage")),
            List.of(), List.of(new MatchingOptionInput(30L, "Processor"), new MatchingOptionInput(31L, "Storage")));
        assertThrows(IllegalArgumentException.class, () -> service.updateMatchingQuestionConfiguration(
            question.getId(), "Match.", BigDecimal.ONE, 1, duplicatePrompt));

        QuestionConfigInput foreignChoice = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU", "Processor")), List.of(),
            List.of(new MatchingOptionInput(999L, "Processor")));
        assertThrows(IllegalArgumentException.class, () -> service.updateMatchingQuestionConfiguration(
            question.getId(), "Match.", BigDecimal.ONE, 1, foreignChoice));
    }

    @Test
    void matchingUpdatePreservesExistingAttemptProtectionBeforeChangingOptions() {
        QuestionConfigInput original = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU", "Processor")), List.of("Storage"));
        Question question = service.createQuestionWithChoices(8L, "MATCHING_TYPE", "Match.", BigDecimal.ONE, 1,
            List.of(new AssessmentManagementService.ChoiceDefinition("Processor", 1),
                new AssessmentManagementService.ChoiceDefinition("Storage", 2)), List.of(), null, original).question();
        QuestionConfigInput revised = new QuestionConfigInput(null, null, null,
            List.of(new MatchingPairInput("CPU", "Core")), List.of("Storage"),
            List.of(new MatchingOptionInput(30L, "Core"), new MatchingOptionInput(31L, "Storage")));
        when(assessmentAttemptRepository.existsByAssessment_Id(8L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.updateMatchingQuestionConfiguration(
            question.getId(), "Should not save", BigDecimal.ONE, 1, revised));
        assertEquals("Processor", savedChoices.getFirst().getChoiceText());
        assertTrue(question.getCorrectAnswerData().contains("\"choiceId\":30"));
    }

    @Test
    void rejectsNewQuestionTypesForActivities() {
        assessment.setAssessmentType("ACTIVITY");
        QuestionConfigInput config = new QuestionConfigInput(List.of("CPU"), 1, false, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.createQuestionWithChoices(8L, "ENUMERATION",
            "Name CPU", BigDecimal.ONE, 1, List.of(), List.of(), null, config));
        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    void updatesNewQuestionConfigurationBeforeAttemptsAndProtectsItAfterAttemptsExist() {
        QuestionConfigInput original = new QuestionConfigInput(List.of("CPU"), 1, false, null, null);
        Question question = service.createQuestionWithChoices(8L, "ENUMERATION", "Name CPU",
            BigDecimal.ONE, 1, List.of(), List.of(), null, original).question();
        QuestionConfigInput revised = new QuestionConfigInput(List.of("CPU"), 1, false, null, null);

        Question updated = service.updateQuestionConfiguration(question.getId(), "Name the processor",
            BigDecimal.ONE, 1, revised, "{\"acceptedAnswers\":[\"CPU\"]}",
            "{\"requiredAnswerCount\":1,\"ordered\":false}");
        assertEquals("Name the processor", updated.getQuestionText());
        assertEquals("{\"acceptedAnswers\":[\"CPU\"]}", updated.getCorrectAnswerData());

        when(assessmentAttemptRepository.existsByAssessment_Id(8L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.updateQuestionConfiguration(question.getId(),
            "Should not save", BigDecimal.ONE, 1, revised,
            "{\"acceptedAnswers\":[\"Should not save\"]}",
            "{\"requiredAnswerCount\":1,\"ordered\":false}"));
        assertEquals("Name the processor", savedQuestion.getQuestionText());
    }

    @Test
    void createsGeneralizedAssessmentAsDraftWhenCallerDoesNotRequestActivation() {
        Competency competency = new Competency();
        competency.setId(2L);
        when(competencyRepository.findById(2L)).thenReturn(Optional.of(competency));
        when(assessmentRepository.findByCompetency_IdAndAssessmentType(2L, "GENERALIZED_POST_TEST"))
            .thenReturn(Optional.empty());
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(call -> call.getArgument(0));

        Assessment created = service.createAssessment("GENERALIZED_POST_TEST", null, 2L,
            "COC 1 Generalized Post-test", null, false);

        assertEquals(false, created.isActive());
        verify(assessmentRepository).save(argThat(value -> !value.isActive()));
    }

    @Test
    void rejectsAssessmentScopeCombinationsThatDoNotMatchTheirType() {
        assertThrows(IllegalArgumentException.class,
            () -> service.createAssessment("PRE_TEST", null, null, "Invalid", null, false));
        assertThrows(IllegalArgumentException.class,
            () -> service.createAssessment("PRE_TEST", 41L, 2L, "Invalid", null, false));
        assertThrows(IllegalArgumentException.class,
            () -> service.createAssessment("GENERALIZED_POST_TEST", 41L, null, "Invalid", null, false));
        assertThrows(IllegalArgumentException.class,
            () -> service.createAssessment("GENERALIZED_POST_TEST", null, null, "Invalid", null, false));

        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void existingAssessmentCanStillBeActivatedThroughUpdate() {
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(call -> call.getArgument(0));

        Assessment updated = service.updateAssessment(8L, "Pre-test", "Instructions", true);

        assertEquals(true, updated.isActive());
        verify(assessmentRepository).save(argThat(Assessment::isActive));
    }

    @Test
    void createsChoiceQuestionAndCorrectAnswerFromThePersistedChoiceIds() {
        AssessmentManagementService.ConfiguredQuestion result = service.createQuestionWithChoices(
            8L, "MULTIPLE_CHOICE", "Pick the correct item", new BigDecimal("2.50"), 1,
            List.of(new AssessmentManagementService.ChoiceDefinition("First", 1),
                new AssessmentManagementService.ChoiceDefinition("Second", 2)),
            List.of(1), null);

        assertEquals("{\"choiceId\":31}", result.question().getCorrectAnswerData());
        assertEquals(2, result.choices().size());
        assertEquals(31L, result.choices().get(1).getId());
    }

    @Test
    void createsBooleanQuestionWithNoChoices() {
        AssessmentManagementService.ConfiguredQuestion result = service.createQuestionWithChoices(
            8L, "TRUE_FALSE", "Statement", BigDecimal.ONE, 1, List.of(), List.of(), true);

        assertEquals("{\"value\":true}", result.question().getCorrectAnswerData());
        assertEquals(List.of(), result.choices());
    }

    @Test
    void rejectsInvalidCorrectChoiceIndexesAndDuplicateQuestionOrder() {
        assertThrows(IllegalArgumentException.class, () -> service.createQuestionWithChoices(
            8L, "MULTIPLE_CHOICE", "Prompt", BigDecimal.ONE, 1,
            List.of(new AssessmentManagementService.ChoiceDefinition("A", 1),
                new AssessmentManagementService.ChoiceDefinition("B", 2)), List.of(2), null));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void refusesToDeleteAnAnswerChoiceReferencedBySavedStudentAnswers() {
        Question question = new Question(); question.setId(21L); question.setAssessment(assessment);
        AnswerChoice choice = new AnswerChoice(); choice.setId(30L); choice.setQuestion(question);
        when(choiceRepository.findById(30L)).thenReturn(Optional.of(choice));
        when(assessmentAttemptRepository.existsByAssessment_Id(8L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteAnswerChoice(30L));
        verify(choiceRepository, never()).delete(any(AnswerChoice.class));
    }

    @Test
    void updatesChoiceDisplayOrderAndShiftsOtherChoicesWithoutChangingTheirIdentityOrAnswerReference() {
        Question question = new Question();
        question.setId(21L);
        question.setAssessment(assessment);
        question.setCorrectAnswerData("{\"choiceId\":30}");
        savedQuestion = question;
        String[] texts = {"Keyboard", "Mouse", "Monitor", "Printer"};
        for (int index = 0; index < texts.length; index++) {
            AnswerChoice choice = new AnswerChoice();
            choice.setId(30L + index);
            choice.setQuestion(question);
            choice.setChoiceText(texts[index]);
            choice.setDisplayOrder(index + 1);
            savedChoices.add(choice);
        }
        when(choiceRepository.findById(30L)).thenReturn(Optional.of(savedChoices.get(0)));
        when(choiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(21L))
            .thenAnswer(call -> savedChoices.stream()
                .sorted(java.util.Comparator.comparingInt(AnswerChoice::getDisplayOrder)).toList());

        AnswerChoice updated = service.updateAnswerChoice(30L, "Keyboard", 3);
        List<AnswerChoice> reloaded = service.getAnswerChoicesByQuestion(21L);

        assertEquals(30L, updated.getId());
        assertEquals("Keyboard", updated.getChoiceText());
        assertEquals(3, updated.getDisplayOrder());
        assertEquals("{\"choiceId\":30}", question.getCorrectAnswerData());
        assertEquals(List.of(31L, 32L, 30L, 33L), reloaded.stream()
            .map(AnswerChoice::getId).toList());
        assertEquals(List.of("Mouse", "Monitor", "Keyboard", "Printer"), reloaded.stream()
            .map(AnswerChoice::getChoiceText).toList());
        verify(choiceRepository).flush();
        verifyNoInteractions(assessmentAttemptRepository, attemptAnswerRepository);
    }

    @Test
    void refusesToDeleteQuestionWhenAssessmentHasAttemptHistoryEvenWithoutSavedAnswers() {
        Question question = new Question(); question.setId(21L); question.setAssessment(assessment);
        when(questionRepository.findById(21L)).thenReturn(Optional.of(question));
        when(assessmentAttemptRepository.existsByAssessment_Id(8L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteQuestion(21L));
        verify(questionRepository, never()).delete(any(Question.class));
    }

    @Test
    void permitsQuestionAndChoiceDeletionBeforeAnyAttemptExists() {
        Question question = new Question(); question.setId(21L); question.setAssessment(assessment);
        AnswerChoice choice = new AnswerChoice(); choice.setId(30L); choice.setQuestion(question);
        when(questionRepository.findById(21L)).thenReturn(Optional.of(question));
        when(choiceRepository.findByQuestion_IdOrderByDisplayOrderAsc(21L)).thenReturn(List.of(choice));
        when(choiceRepository.findById(30L)).thenReturn(Optional.of(choice));
        when(assessmentAttemptRepository.existsByAssessment_Id(8L)).thenReturn(false);
        when(attemptAnswerRepository.existsByQuestion_Id(21L)).thenReturn(false);

        service.deleteAnswerChoice(30L);
        service.deleteQuestion(21L);

        verify(choiceRepository).delete(choice);
        verify(choiceRepository).deleteAll(List.of(choice));
        verify(questionRepository).delete(question);
    }

    @Test
    void reportsMissingAssessmentsWithoutCreatingQuestions() {
        when(assessmentRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.createQuestionWithChoices(
            404L, "TRUE_FALSE", "Prompt", BigDecimal.ONE, 1, List.of(), List.of(), true));
        verify(questionRepository, never()).save(any());
    }
}
