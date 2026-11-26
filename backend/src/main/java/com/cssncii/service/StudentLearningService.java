package com.cssncii.service;

import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.LearningStageContent;
import com.cssncii.entity.Student;
import com.cssncii.entity.StudentCompetencyProgress;
import com.cssncii.entity.StudentStageProgress;
import com.cssncii.entity.StudentStatus;
import com.cssncii.repository.CompetencyRepository;
import com.cssncii.repository.DiscussionRepository;
import com.cssncii.repository.LearningStageContentRepository;
import com.cssncii.repository.StudentCompetencyProgressRepository;
import com.cssncii.repository.StudentRepository;
import com.cssncii.repository.StudentStageProgressRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentLearningService {
    public static final List<String> STAGE_CODES = List.of(
        "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST");
    public static final List<String> STAGE_TITLES = List.of(
        "Pre-test", "Definition of Terms", "Explanation", "Examples", "Video Lessons", "Activity", "Discussion Post-test");

    private final StudentRepository studentRepository;
    private final CompetencyRepository competencyRepository;
    private final DiscussionRepository discussionRepository;
    private final LearningStageContentRepository contentRepository;
    private final StudentCompetencyProgressRepository competencyProgressRepository;
    private final StudentStageProgressRepository stageProgressRepository;
    private final StudentProgressService progressService;

    public StudentLearningService(
        StudentRepository studentRepository,
        CompetencyRepository competencyRepository,
        DiscussionRepository discussionRepository,
        LearningStageContentRepository contentRepository,
        StudentCompetencyProgressRepository competencyProgressRepository,
        StudentStageProgressRepository stageProgressRepository,
        StudentProgressService progressService
    ) {
        this.studentRepository = studentRepository;
        this.competencyRepository = competencyRepository;
        this.discussionRepository = discussionRepository;
        this.contentRepository = contentRepository;
        this.competencyProgressRepository = competencyProgressRepository;
        this.stageProgressRepository = stageProgressRepository;
        this.progressService = progressService;
    }

    @Transactional
    public Dashboard getDashboard(Long studentId) {
        Student student = requireActiveStudent(studentId);
        progressService.initializeStudentProgress(studentId);
        List<StudentCompetencyProgress> progressRows = competencyProgressRepository
            .findByStudent_IdOrderByCompetency_DisplayOrderAsc(studentId);
        Map<Long, String> competencyStatuses = new LinkedHashMap<>();
        progressRows.forEach(row -> competencyStatuses.put(row.getCompetency().getId(), row.getStatus()));

        List<CompetencySummary> competencies = competencyRepository
            .findAll(Sort.by(Sort.Direction.ASC, "displayOrder")).stream()
            .filter(Competency::isActive)
            .filter(competency -> "COC1".equals(competency.getCode()) || "COC2".equals(competency.getCode()))
            .map(competency -> toCompetencySummary(studentId, competency,
                competencyStatuses.getOrDefault(competency.getId(), "LOCKED")))
            .toList();
        return new Dashboard(student.getFullName(), student.getAccountId(), student.getProgram(), competencies);
    }

    @Transactional
    public StageContent openStage(Long studentId, Long competencyId, Long discussionId, String stageCode) {
        requireActiveStudent(studentId);
        Competency competency = competencyRepository.findById(competencyId)
            .filter(Competency::isActive)
            .orElseThrow(() -> new EntityNotFoundException("Competency not found."));
        Discussion discussion = discussionRepository.findById(discussionId)
            .filter(Discussion::isActive)
            .orElseThrow(() -> new EntityNotFoundException("Discussion not found."));
        if (discussion.getCompetency() == null || !discussion.getCompetency().getId().equals(competency.getId())) {
            throw new EntityNotFoundException("Discussion not found in this competency.");
        }
        String normalizedCode = stageCode == null ? "" : stageCode.trim().toUpperCase(java.util.Locale.ROOT);
        int index = STAGE_CODES.indexOf(normalizedCode);
        if (index < 0) throw new IllegalArgumentException("stageCode must be one of the seven standard stage codes.");
        StudentStageProgress progress = progressService.startStage(studentId, discussionId, normalizedCode);
        LearningStageContent content = contentRepository.findByDiscussion_IdOrderByDisplayOrderAsc(discussionId)
            .stream().filter(row -> row.isActive() && normalizedCode.equals(row.getStageCode()))
            .findFirst().orElse(null);
        return new StageContent(discussionId, normalizedCode, STAGE_TITLES.get(index), progress.getStatus(),
            content == null ? null : content.getTitle(), content == null ? null : content.getContentBody(),
            content != null && content.getContentBody() != null && !content.getContentBody().isBlank(),
            content == null || !"VIDEO_LESSONS".equals(normalizedCode) ? List.of()
                : content.getYoutubeVideoUrls().stream().map(YouTubeVideoUrls::videoId).toList());
    }

    @Transactional
    public StageStatus completeNonAssessmentStage(Long studentId, Long competencyId, Long discussionId, String stageCode) {
        requireActiveStudent(studentId);
        if ("PRE_TEST".equals(stageCode) || "ACTIVITY".equals(stageCode) || "POST_TEST".equals(stageCode)) {
            throw new IllegalArgumentException("Assessment stages are completed by submitting their assessment.");
        }
        openStage(studentId, competencyId, discussionId, stageCode);
        LearningStageContent content = contentRepository.findByDiscussion_IdOrderByDisplayOrderAsc(discussionId)
            .stream().filter(row -> row.isActive() && stageCode.equals(row.getStageCode()))
            .findFirst().orElseThrow(() -> new IllegalStateException("This learning stage has not been configured yet."));
        if (content.getContentBody() == null || content.getContentBody().isBlank()) {
            throw new IllegalStateException("This learning stage has no published content yet.");
        }
        StudentStageProgress progress = progressService.completeStage(studentId, discussionId, stageCode);
        return new StageStatus(progress.getDiscussion().getId(), progress.getStageCode(), progress.getStatus(), progress.getCompletedAt());
    }

    private CompetencySummary toCompetencySummary(Long studentId, Competency competency, String status) {
        List<DiscussionSummary> discussions = discussionRepository
            .findByCompetency_IdOrderByDisplayOrderAsc(competency.getId()).stream()
            .filter(Discussion::isActive)
            .map(discussion -> toDiscussionSummary(studentId, discussion))
            .toList();
        int completed = (int) discussions.stream().filter(DiscussionSummary::completed).count();
        int remaining = discussions.size() - completed;
        return new CompetencySummary(competency.getId(), competency.getCode(), competency.getTitle(),
            competency.getDescription(), status, discussions.size(), completed, remaining,
            discussions.isEmpty() ? 0 : (completed * 100 / discussions.size()), discussions);
    }

    private DiscussionSummary toDiscussionSummary(Long studentId, Discussion discussion) {
        Map<String, StudentStageProgress> byCode = new LinkedHashMap<>();
        stageProgressRepository.findByStudent_IdAndDiscussion_Id(studentId, discussion.getId())
            .forEach(row -> byCode.put(row.getStageCode(), row));
        List<StageSummary> stages = STAGE_CODES.stream().map(code -> {
            StudentStageProgress row = byCode.get(code);
            return new StageSummary(code, STAGE_TITLES.get(STAGE_CODES.indexOf(code)),
                row == null ? "LOCKED" : row.getStatus());
        }).toList();
        boolean complete = stages.size() == STAGE_CODES.size()
            && stages.stream().allMatch(stage -> "COMPLETED".equals(stage.status()));
        return new DiscussionSummary(discussion.getId(), discussion.getTitle(), discussion.getDescription(),
            discussion.getDisplayOrder(), complete, (int) stages.stream().filter(stage -> "COMPLETED".equals(stage.status())).count(), stages);
    }

    private Student requireActiveStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException("Student account not found."));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new IllegalStateException("This student account is not active.");
        }
        return student;
    }

    public record Dashboard(String fullName, String accountId, String program, List<CompetencySummary> competencies) {}
    public record CompetencySummary(Long id, String code, String title, String description, String status,
        int discussionCount, int completedDiscussions, int remainingDiscussions, int completionPercent,
        List<DiscussionSummary> discussions) {}
    public record DiscussionSummary(Long id, String title, String description, long displayOrder,
        boolean completed, int completedStages, List<StageSummary> stages) {}
    public record StageSummary(String stageCode, String title, String status) {}
    public record StageContent(Long discussionId, String stageCode, String stageName, String status,
        String title, String contentBody, boolean hasContent, List<String> youtubeVideoIds) {
        public StageContent(Long discussionId, String stageCode, String stageName, String status,
            String title, String contentBody, boolean hasContent) {
            this(discussionId, stageCode, stageName, status, title, contentBody, hasContent, List.of());
        }
    }
    public record StageStatus(Long discussionId, String stageCode, String status, java.time.LocalDateTime completedAt) {}
}
