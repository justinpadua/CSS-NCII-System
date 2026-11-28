package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cssncii.entity.AnswerChoice;
import com.cssncii.entity.Assessment;
import com.cssncii.entity.AssessmentAttempt;
import com.cssncii.entity.AttemptAnswer;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Question;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.AnswerChoiceRepository;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.AttemptAnswerRepository;
import com.cssncii.repository.QuestionRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssessmentAttemptServiceTest {
    private AssessmentAttemptService service;
    private StudentRepository studentRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentAttemptRepository attemptRepository;
    private QuestionRepository questionRepository;
    private AttemptAnswerRepository answerRepository;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        attemptRepository = mock(AssessmentAttemptRepository.class);
        questionRepository = mock(QuestionRepository.class);
        answerRepository = mock(AttemptAnswerRepository.class);
        service = new AssessmentAttemptService(
            studentRepository,
            assessmentRepository,
            attemptRepository,
            answerRepository,
            questionRepository,
            mock(AnswerChoiceRepository.class),
            mock(StudentStageProgressRepository.class),
            mock(StudentProgressService.class)
        );
    }

    @Test
    void resumesExistingInProgressAttemptEvenWhenAssessmentIsInactive() {
        Student student = activeStudent(7L);
        Assessment assessment = inactivePretest();
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setId(1L);
        attempt.setAssessment(assessment);
        attempt.setStudent(student);
        attempt.setStatus("IN_PROGRESS");
        attempt.setAttemptNumber(1);
        attempt.setStartedAt(LocalDateTime.parse("2026-11-23T10:00:00"));
        when(studentRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(student));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(attemptRepository.findByStudent_IdAndAssessment_Id(7L, 1L)).thenReturn(Optional.of(attempt));

        AssessmentAttemptService.AttemptSummary resumed = service.startOrResumeAttempt(7L, 1L);

        assertEquals(1L, resumed.attemptId());
        assertEquals("IN_PROGRESS", resumed.status());
        verify(attemptRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(questionRepository, never()).findByAssessment_IdOrderByDisplayOrderAsc(1L);
    }

    @Test
    void stillRejectsStartingNewAttemptOnInactiveAssessment() {
        when(studentRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(activeStudent(7L)));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(inactivePretest()));
        when(attemptRepository.findByStudent_IdAndAssessment_Id(7L, 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.startOrResumeAttempt(7L, 1L));
        verify(attemptRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(questionRepository, never()).findByAssessment_IdOrderByDisplayOrderAsc(1L);
    }

    @Test
    void scoresMultipleChoiceByExactChoiceId() {
        Question question = question("MULTIPLE_CHOICE", "{\"choiceId\":12}");
        List<AnswerChoice> choices = List.of(choice(12L), choice(13L));

        AssessmentAttemptService.ScoredAnswer result =
            service.scoreResponse(question, "{\"choiceId\":12}", choices);

        assertTrue(result.correct());
        assertEquals(new BigDecimal("2.00"), result.awardedPoints());
        assertFalse(service.scoreResponse(question, "{\"choiceId\":13}", choices).correct());
    }

    @Test
    void existingAssessmentTypesKeepTheirCorrectAnswerFeedbackPolicies() {
        Student student = activeStudent(7L);
        when(studentRepository.findById(7L)).thenReturn(Optional.of(student));
        long attemptId = 50L;
        for (String type : List.of("PRE_TEST", "ACTIVITY", "DISCUSSION_POST_TEST")) {
            Assessment assessment = new Assessment();
            assessment.setId(attemptId);
            assessment.setAssessmentType(type);
            assessment.setDiscussion(new Discussion());
            Question question = question("MULTIPLE_CHOICE", "{\"choiceId\":12}");
            question.setId(attemptId);
            question.setAssessment(assessment);
            AssessmentAttempt attempt = new AssessmentAttempt();
            attempt.setId(attemptId);
            attempt.setAssessment(assessment);
            attempt.setStudent(student);
            attempt.setStatus("SUBMITTED");
            when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
            when(questionRepository.findByAssessment_IdOrderByDisplayOrderAsc(attemptId)).thenReturn(List.of(question));
            AttemptAnswer answer = new AttemptAnswer();
            answer.setQuestion(question);
            answer.setResponseData("{\"choiceId\":13}");
            answer.setCorrect(false);
            when(answerRepository.findByAttempt_Id(attemptId)).thenReturn(List.of(answer));

            var incorrect = service.getSubmissionResult(7L, attemptId).incorrectlyAnsweredQuestions().getFirst();
            if ("PRE_TEST".equals(type)) {
                assertNull(incorrect.correctAnswerData());
            } else {
                assertEquals("{\"choiceId\":12}", incorrect.correctAnswerData());
            }
            attemptId++;
        }
    }

    @Test
    void matchingAwardsRoundedPartialCreditAndRejectsUnknownOptionIds() {
        Question question = question("MATCHING_TYPE",
            "{\"matches\":[{\"promptIndex\":0,\"choiceId\":12},{\"promptIndex\":1,\"choiceId\":13},{\"promptIndex\":2,\"choiceId\":14}]}");
        question.setQuestionConfig("{\"matchingPrompts\":[\"A\",\"B\",\"C\"]}");
        question.setPoints(new BigDecimal("5.00"));
        List<AnswerChoice> choices = List.of(choice(12L), choice(13L), choice(14L), choice(15L));
        var partial = service.scoreResponse(question,
            "{\"matches\":[{\"promptIndex\":0,\"choiceId\":12},{\"promptIndex\":1,\"choiceId\":15},{\"promptIndex\":2,\"choiceId\":null}]}", choices);
        assertFalse(partial.correct());
        assertEquals(new BigDecimal("1.67"), partial.awardedPoints());
        assertThrows(IllegalArgumentException.class, () -> service.scoreResponse(question,
            "{\"matches\":[{\"promptIndex\":0,\"choiceId\":99},{\"promptIndex\":1,\"choiceId\":13},{\"promptIndex\":2,\"choiceId\":14}]}", choices));
    }

    @Test
    void matchingRejectsReusedOptionsIncludingDistractorsButAcceptsUniquePartialAnswers() {
        Question question = question("MATCHING_TYPE",
            "{\"matches\":[{\"promptIndex\":0,\"choiceId\":12},{\"promptIndex\":1,\"choiceId\":13},{\"promptIndex\":2,\"choiceId\":14}]}");
        question.setQuestionConfig("{\"matchingPrompts\":[\"A\",\"B\",\"C\"]}");
        question.setPoints(new BigDecimal("5.00"));
        List<AnswerChoice> choices = List.of(choice(12L), choice(13L), choice(14L), choice(15L));

        var uniquePartial = service.scoreResponse(question,
            "{\"matches\":[{\"promptIndex\":0,\"choiceId\":12},{\"promptIndex\":1,\"choiceId\":15},{\"promptIndex\":2,\"choiceId\":null}]}", choices);
        assertFalse(uniquePartial.correct());
        assertEquals(new BigDecimal("1.67"), uniquePartial.awardedPoints());
        assertThrows(IllegalArgumentException.class, () -> service.scoreResponse(question,
            "{\"matches\":[{\"promptIndex\":0,\"choiceId\":12},{\"promptIndex\":1,\"choiceId\":12},{\"promptIndex\":2,\"choiceId\":14}]}", choices));
        assertThrows(IllegalArgumentException.class, () -> service.scoreResponse(question,
            "{\"matches\":[{\"promptIndex\":0,\"choiceId\":15},{\"promptIndex\":1,\"choiceId\":15},{\"promptIndex\":2,\"choiceId\":14}]}", choices));
    }

    @Test
    void enumerationSupportsOrderedUnorderedDuplicatesAndBlankFields() {
        Question unordered = question("ENUMERATION", "{\"acceptedAnswers\":[\"RAM\",\"CPU\",\"SSD\"]}");
        unordered.setQuestionConfig("{\"requiredAnswerCount\":3,\"ordered\":false}");
        var unorderedResult = service.scoreResponse(unordered,
            "{\"answers\":[\" cpu \",\"RAM\",\"ram\"]}", List.of());
        assertFalse(unorderedResult.correct());
        assertEquals(new BigDecimal("1.33"), unorderedResult.awardedPoints());
        var blanks = service.scoreResponse(unordered, "{\"answers\":[\"SSD\",\"\",\"\"]}", List.of());
        assertEquals(new BigDecimal("0.67"), blanks.awardedPoints());

        Question ordered = question("ENUMERATION", "{\"acceptedAnswers\":[\"RAM\",\"CPU\"]}");
        ordered.setQuestionConfig("{\"requiredAnswerCount\":2,\"ordered\":true}");
        assertFalse(service.scoreResponse(ordered, "{\"answers\":[\"CPU\",\"RAM\"]}", List.of()).correct());
        assertTrue(service.scoreResponse(ordered, "{\"answers\":[\" ram \",\"cpu\"]}", List.of()).correct());
    }

    @Test
    void newQuestionTypesRejectMalformedStoredKeysAndResponses() {
        Question question = question("ENUMERATION", "{\"acceptedAnswers\":[\"CPU\"]}");
        question.setQuestionConfig("{\"requiredAnswerCount\":1,\"ordered\":false}");
        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(question, "{\"value\":\"CPU\"}", List.of()));
        question.setCorrectAnswerData("not-json");
        assertThrows(IllegalStateException.class,
            () -> service.scoreResponse(question, "{\"answers\":[\"CPU\"]}", List.of()));
    }

    @Test
    void scoresTrueFalseOnlyFromJsonBooleans() {
        Question question = question("TRUE_FALSE", "{\"value\":true}");

        assertTrue(service.scoreResponse(question, "{\"value\":true}", List.of()).correct());
        assertFalse(service.scoreResponse(question, "{\"value\":false}", List.of()).correct());
        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(question, "{\"value\":\"true\"}", List.of()));
    }

    @Test
    void scoresMultipleSelectWithoutDependingOnArrayOrder() {
        Question question = question("MULTIPLE_SELECT", "{\"choiceIds\":[12,14,16]}");
        List<AnswerChoice> choices = List.of(choice(12L), choice(14L), choice(16L), choice(18L));

        assertTrue(service.scoreResponse(question, "{\"choiceIds\":[16,12,14]}", choices).correct());
        assertFalse(service.scoreResponse(question, "{\"choiceIds\":[12,14]}", choices).correct());
    }

    @Test
    void rejectsMalformedUnsupportedAndInvalidChoiceResponses() {
        Question multipleChoice = question("MULTIPLE_CHOICE", "{\"choiceId\":12}");
        List<AnswerChoice> choices = List.of(choice(12L));

        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(multipleChoice, "{bad json}", choices));
        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(multipleChoice, "{\"choiceId\":99}", choices));
        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(multipleChoice, "{\"choiceId\":12,\"extra\":true}", choices));
        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(multipleChoice, "{\"choiceId\":12,\"choiceId\":12}", choices));
        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(question("UNSUPPORTED", "{\"value\":\"x\"}"),
                "{\"value\":\"x\"}", List.of()));
    }

    @Test
    void rejectsDuplicateOrForeignMultipleSelectIds() {
        Question question = question("MULTIPLE_SELECT", "{\"choiceIds\":[12,14]}");
        List<AnswerChoice> choices = List.of(choice(12L), choice(14L), choice(18L));

        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(question, "{\"choiceIds\":[12,12]}", choices));
        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(question, "{\"choiceIds\":[12,19]}", choices));
        assertFalse(service.scoreResponse(question, "{\"choiceIds\":[12,14,18]}", choices).correct());
    }

    @Test
    void rejectsEmptyMultipleSelectResponseAsUnanswered() {
        Question question = question("MULTIPLE_SELECT", "{\"choiceIds\":[12,14]}");
        List<AnswerChoice> choices = List.of(choice(12L), choice(14L));

        assertThrows(IllegalArgumentException.class,
            () -> service.scoreResponse(question, "{\"choiceIds\":[]}", choices));
    }

    private Question question(String type, String correctAnswerData) {
        Assessment assessment = new Assessment();
        assessment.setId(1L);
        Question question = new Question();
        question.setId(1L);
        question.setAssessment(assessment);
        question.setQuestionType(type);
        question.setCorrectAnswerData(correctAnswerData);
        question.setPoints(new BigDecimal("2.00"));
        return question;
    }

    private AnswerChoice choice(Long id) {
        AnswerChoice choice = new AnswerChoice();
        choice.setId(id);
        return choice;
    }

    private Student activeStudent(Long id) {
        Student student = new Student();
        student.setId(id);
        student.setStatus(StudentStatus.ACTIVE);
        return student;
    }

    private Assessment inactivePretest() {
        Discussion discussion = new Discussion();
        discussion.setId(3L);
        Assessment assessment = new Assessment();
        assessment.setId(1L);
        assessment.setDiscussion(discussion);
        assessment.setAssessmentType("PRE_TEST");
        assessment.setTitle("COC 1 Discussion 1 Pre-test");
        assessment.setActive(false);
        return assessment;
    }
}
