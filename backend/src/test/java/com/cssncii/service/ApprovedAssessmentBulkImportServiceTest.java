package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cssncii.repository.AnswerChoiceRepository;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovedAssessmentBulkImportServiceTest {
    private AssessmentAttemptRepository attempts;
    private ApprovedAssessmentBulkImportService importer;

    @BeforeEach
    void setUp() {
        attempts = mock(AssessmentAttemptRepository.class);
        importer = new ApprovedAssessmentBulkImportService(
            mock(ApprovedAssessmentSourceParser.class), mock(AssessmentManagementService.class),
            mock(CompetencyRepository.class), mock(DiscussionRepository.class),
            mock(AssessmentRepository.class), mock(QuestionRepository.class),
            mock(AnswerChoiceRepository.class), attempts);
    }

    @Test
    void inProgressAttemptBlocksAddingMissingQuestions() {
        when(attempts.existsByAssessment_IdAndStatusNot(1L, "SUBMITTED")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> importer.requireAttemptsResolved(1L, 8, false));
        verify(attempts).existsByAssessment_IdAndStatusNot(1L, "SUBMITTED");
        verify(attempts, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void submittedAttemptHistoryAllowsMissingQuestionsWithoutChangingHistory() {
        when(attempts.existsByAssessment_IdAndStatusNot(1L, "SUBMITTED")).thenReturn(false);

        assertDoesNotThrow(() -> importer.requireAttemptsResolved(1L, 8, false));
        verify(attempts).existsByAssessment_IdAndStatusNot(1L, "SUBMITTED");
        verify(attempts, never()).save(org.mockito.ArgumentMatchers.any());
        verify(attempts, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void exactRerunAndNewAssessmentDoNotNeedAnAttemptHistoryCheck() {
        assertDoesNotThrow(() -> importer.requireAttemptsResolved(1L, 0, false));
        assertDoesNotThrow(() -> importer.requireAttemptsResolved(2L, 8, true));
        verify(attempts, never()).existsByAssessment_IdAndStatusNot(1L, "SUBMITTED");
        verify(attempts, never()).existsByAssessment_IdAndStatusNot(2L, "SUBMITTED");
    }
}
