package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentCompetencyProgress;
import com.cssncii.entity.StudentStageProgress;
import com.cssncii.repository.AssessmentAttemptRepository;
import com.cssncii.repository.AssessmentRepository;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.RetakeAuthorizationRepository;
import com.cssncii.repository.StudentCompetencyProgressRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class StudentProgressServiceTest {
    private static final List<String> STAGE_CODES = List.of(
        "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"
    );

    @Test
    void openingUnlockedStagePersistsInProgressWithoutCompletingIt() {
        StudentRepository studentRepository = mock(StudentRepository.class);
        CompetencyRepository competencyRepository = mock(CompetencyRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        StudentStageProgressRepository stageRepository = mock(StudentStageProgressRepository.class);
        StudentCompetencyProgressRepository competencyRepositoryProgress = mock(StudentCompetencyProgressRepository.class);
        Student student = new Student();
        student.setId(8L);
        Competency competency = competency(1L, "COC1");
        Discussion discussion = new Discussion();
        discussion.setId(11L);
        discussion.setCompetency(competency);
        StudentCompetencyProgress competencyProgress = competencyProgress(student, competency, "AVAILABLE");
        List<StudentStageProgress> rows = existingCompletedSequence(student, discussion);
        rows.getFirst().setStatus("AVAILABLE");
        rows.get(1).setStatus("LOCKED");

        when(studentRepository.findById(8L)).thenReturn(Optional.of(student));
        when(competencyRepositoryProgress.findByStudent_IdAndCompetency_Id(8L, 1L))
            .thenReturn(Optional.of(competencyProgress));
        when(discussionRepository.findById(11L)).thenReturn(Optional.of(discussion));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(discussion));
        when(stageRepository.findByStudent_IdAndDiscussion_Id(8L, 11L)).thenAnswer(call -> new ArrayList<>(rows));
        StudentProgressService service = new StudentProgressService(
            studentRepository, competencyRepository, discussionRepository, stageRepository,
            competencyRepositoryProgress, mock(AssessmentRepository.class),
            mock(AssessmentAttemptRepository.class), mock(RetakeAuthorizationRepository.class));

        StudentStageProgress started = service.startStage(8L, 11L, "PRE_TEST");

        assertEquals("IN_PROGRESS", started.getStatus());
        assertEquals(null, started.getCompletedAt());
        verify(stageRepository).save(started);

        started.setStatus("COMPLETED");
        rows.get(1).setStatus("AVAILABLE");
        StudentStageProgress next = service.startStage(8L, 11L, "DEFINITIONS");
        assertEquals("IN_PROGRESS", next.getStatus());
        verify(stageRepository).save(next);
    }

    @Test
    void earlierStagesAreRejectedAfterDiscussionPostTestHasStarted() {
        StudentRepository studentRepository = mock(StudentRepository.class);
        CompetencyRepository competencyRepository = mock(CompetencyRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        StudentStageProgressRepository stageRepository = mock(StudentStageProgressRepository.class);
        StudentCompetencyProgressRepository competencyRepositoryProgress = mock(StudentCompetencyProgressRepository.class);
        Student student = new Student();
        student.setId(8L);
        Competency competency = competency(1L, "COC1");
        Discussion discussion = new Discussion();
        discussion.setId(11L);
        discussion.setCompetency(competency);
        StudentCompetencyProgress competencyProgress = competencyProgress(student, competency, "IN_PROGRESS");
        List<StudentStageProgress> rows = existingCompletedSequence(student, discussion);
        rows.get(1).setStatus("COMPLETED");
        rows.getLast().setStatus("IN_PROGRESS");

        when(studentRepository.findById(8L)).thenReturn(Optional.of(student));
        when(competencyRepositoryProgress.findByStudent_IdAndCompetency_Id(8L, 1L))
            .thenReturn(Optional.of(competencyProgress));
        when(discussionRepository.findById(11L)).thenReturn(Optional.of(discussion));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(discussion));
        when(stageRepository.findByStudent_IdAndDiscussion_Id(8L, 11L)).thenAnswer(call -> new ArrayList<>(rows));
        StudentProgressService service = new StudentProgressService(
            studentRepository, competencyRepository, discussionRepository, stageRepository,
            competencyRepositoryProgress, mock(AssessmentRepository.class),
            mock(AssessmentAttemptRepository.class), mock(RetakeAuthorizationRepository.class));

        assertThrows(IllegalStateException.class, () -> service.startStage(8L, 11L, "DEFINITIONS"));
    }

    @Test
    void repeatedInitializationLocksStudentAndPreservesExistingPassedAndCompletedProgress() {
        StudentRepository studentRepository = mock(StudentRepository.class);
        CompetencyRepository competencyRepository = mock(CompetencyRepository.class);
        DiscussionRepository discussionRepository = mock(DiscussionRepository.class);
        StudentStageProgressRepository stageProgressRepository = mock(StudentStageProgressRepository.class);
        StudentCompetencyProgressRepository competencyProgressRepository =
            mock(StudentCompetencyProgressRepository.class);
        Student student = new Student();
        student.setId(8L);
        Competency coc1 = competency(1L, "COC1");
        Competency coc2 = competency(2L, "COC2");
        StudentCompetencyProgress coc1Progress = competencyProgress(student, coc1, "PASSED");
        StudentCompetencyProgress coc2Progress = competencyProgress(student, coc2, "PASSED");
        Discussion discussion = new Discussion();
        discussion.setId(11L);
        discussion.setCompetency(coc1);
        List<StudentStageProgress> existingStages = existingCompletedSequence(student, discussion);

        when(studentRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(student));
        when(competencyRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder")))
            .thenReturn(List.of(coc1, coc2));
        when(competencyProgressRepository.findByStudent_IdAndCompetency_Id(8L, 1L))
            .thenReturn(Optional.of(coc1Progress));
        when(competencyProgressRepository.findByStudent_IdAndCompetency_Id(8L, 2L))
            .thenReturn(Optional.of(coc2Progress));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(1L))
            .thenReturn(List.of(discussion));
        when(discussionRepository.findByCompetency_IdOrderByDisplayOrderAsc(2L))
            .thenReturn(List.of());
        when(stageProgressRepository.findByStudent_IdAndDiscussion_Id(8L, 11L))
            .thenAnswer(invocation -> new ArrayList<>(existingStages));
        when(competencyProgressRepository.findByStudent_IdOrderByCompetency_DisplayOrderAsc(8L))
            .thenReturn(List.of(coc1Progress, coc2Progress));
        StudentProgressService service = new StudentProgressService(
            studentRepository,
            competencyRepository,
            discussionRepository,
            stageProgressRepository,
            competencyProgressRepository,
            mock(AssessmentRepository.class),
            mock(AssessmentAttemptRepository.class),
            mock(RetakeAuthorizationRepository.class));

        service.initializeStudentProgress(8L);
        service.initializeStudentProgress(8L);

        verify(studentRepository, times(2)).findByIdForUpdate(8L);
        verify(stageProgressRepository, never()).save(org.mockito.ArgumentMatchers.any(StudentStageProgress.class));
        verify(competencyProgressRepository, never())
            .save(org.mockito.ArgumentMatchers.any(StudentCompetencyProgress.class));
        assertEquals("PASSED", coc1Progress.getStatus());
        assertEquals("PASSED", coc2Progress.getStatus());
        assertEquals("COMPLETED", existingStages.getFirst().getStatus());
    }

    private Competency competency(Long id, String code) {
        Competency competency = new Competency();
        competency.setId(id);
        competency.setCode(code);
        return competency;
    }

    private StudentCompetencyProgress competencyProgress(Student student, Competency competency, String status) {
        StudentCompetencyProgress progress = new StudentCompetencyProgress();
        progress.setStudent(student);
        progress.setCompetency(competency);
        progress.setStatus(status);
        return progress;
    }

    private List<StudentStageProgress> existingCompletedSequence(Student student, Discussion discussion) {
        List<StudentStageProgress> rows = new ArrayList<>();
        for (int index = 0; index < STAGE_CODES.size(); index++) {
            StudentStageProgress row = new StudentStageProgress();
            row.setStudent(student);
            row.setDiscussion(discussion);
            row.setStageCode(STAGE_CODES.get(index));
            row.setStatus(index == 0 ? "COMPLETED" : index == 1 ? "AVAILABLE" : "LOCKED");
            rows.add(row);
        }
        return rows;
    }
}
