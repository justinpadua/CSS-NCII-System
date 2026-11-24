package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.LearningStageContent;
import com.cssncii.entity.Topic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CourseStructureInitializationServiceTest {
    private CourseManagementService courseManagementService;
    private LearningContentService learningContentService;
    private CourseStructureInitializationService initializer;
    private final List<Competency> competencies = new ArrayList<>();
    private final Map<Long, List<Discussion>> discussions = new HashMap<>();
    private final Map<Long, List<Topic>> topics = new HashMap<>();
    private final Map<Long, List<LearningStageContent>> stages = new HashMap<>();
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        courseManagementService = mock(CourseManagementService.class);
        learningContentService = mock(LearningContentService.class);
        initializer = new CourseStructureInitializationService(courseManagementService, learningContentService);

        when(courseManagementService.getAllCompetencies()).thenAnswer(call -> List.copyOf(competencies));
        doAnswer(call -> {
            Competency competency = new Competency();
            competency.setId(ids.getAndIncrement());
            competency.setCode(call.getArgument(0));
            competency.setTitle(call.getArgument(1));
            competency.setDisplayOrder(call.getArgument(3));
            competency.setActive(true);
            competencies.add(competency);
            discussions.put(competency.getId(), new ArrayList<>());
            return competency;
        }).when(courseManagementService).createCompetency(anyString(), anyString(), nullable(String.class), anyLong());

        when(courseManagementService.getDiscussionsByCompetency(anyLong()))
            .thenAnswer(call -> List.copyOf(discussions.get(call.getArgument(0))));
        doAnswer(call -> {
            Discussion discussion = new Discussion();
            discussion.setId(ids.getAndIncrement());
            discussion.setCompetency(competencies.stream()
                .filter(item -> item.getId().equals(call.getArgument(0))).findFirst().orElseThrow());
            discussion.setTitle(call.getArgument(1));
            discussion.setDisplayOrder(call.getArgument(3));
            discussion.setActive(true);
            discussions.get(discussion.getCompetency().getId()).add(discussion);
            topics.put(discussion.getId(), new ArrayList<>());
            return discussion;
        }).when(courseManagementService).createDiscussion(anyLong(), anyString(), nullable(String.class), anyLong());

        when(courseManagementService.getTopicsByDiscussion(anyLong()))
            .thenAnswer(call -> topics.get(call.getArgument(0)));
        doAnswer(call -> {
            Topic topic = new Topic();
            topic.setId(ids.getAndIncrement());
            topic.setTitle(call.getArgument(1));
            topic.setDisplayOrder(call.getArgument(3));
            topic.setActive(true);
            topics.get(call.getArgument(0)).add(topic);
            return topic;
        }).when(courseManagementService).createTopic(anyLong(), anyString(), nullable(String.class), anyLong());

        when(learningContentService.initializeDiscussionStages(anyLong())).thenAnswer(call -> {
            Long discussionId = call.getArgument(0);
            return stages.computeIfAbsent(discussionId, ignored -> canonicalStages(discussionId));
        });
    }

    @Test
    void initializesDocumentedCoursesDiscussionsTopicsAndExactlySevenStagesIdempotently() {
        CourseStructureInitializationService.InitializationResult first = initializer.initialize();

        assertEquals(2, first.competencyCount());
        assertEquals(9, first.discussionCount());
        assertEquals(24, first.topicCount());
        assertEquals(63, first.learningStageCount());
        assertEquals(24, first.createdTopicCount());
        assertEquals(List.of("COC1", "COC2"), competencies.stream().map(Competency::getCode).toList());
        assertTrue(competencies.stream().allMatch(Competency::isActive));
        assertEquals(List.of(6, 3), competencies.stream()
            .map(item -> discussions.get(item.getId()).size()).toList());
        assertEquals(List.of(
            "Discuss fundamental concepts of computer systems servicing",
            "Discuss elements of computer systems",
            "Perform disassembly and assembly of computer hardware",
            "Create portable bootable device",
            "Perform installation of system software and application software",
            "Perform computer testing and documentation",
            "Discuss the concepts of computer network",
            "Perform setting up of computer network",
            "Perform network testing and documentation"
        ), competencies.stream().flatMap(item -> discussions.get(item.getId()).stream())
            .map(Discussion::getTitle).toList());
        assertEquals(List.of(6, 2, 4, 2, 2, 2, 3, 2, 1), competencies.stream()
            .flatMap(item -> discussions.get(item.getId()).stream())
            .map(item -> topics.get(item.getId()).size()).toList());
        assertEquals(24, topics.values().stream().mapToInt(List::size).sum());
        assertEquals(63, stages.values().stream().mapToInt(List::size).sum());
        assertEquals(List.of("PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"),
            stages.values().iterator().next().stream().map(LearningStageContent::getStageCode).toList());

        initializer.initialize();

        assertEquals(2, competencies.size());
        assertEquals(9, discussions.values().stream().mapToInt(List::size).sum());
        assertEquals(24, topics.values().stream().mapToInt(List::size).sum());
        assertEquals(63, stages.values().stream().mapToInt(List::size).sum());
    }

    @Test
    void stopsInsteadOfOverwritingAnExistingDiscussionAtTheDocumentedOrder() {
        Competency competency = new Competency();
        competency.setId(10L);
        competency.setCode("COC1");
        competency.setTitle("COC 1");
        competency.setActive(true);
        competencies.add(competency);
        Discussion conflicting = new Discussion();
        conflicting.setId(11L);
        conflicting.setCompetency(competency);
        conflicting.setTitle("Admin-managed different discussion");
        conflicting.setDisplayOrder(1);
        conflicting.setActive(true);
        discussions.put(10L, new ArrayList<>(List.of(conflicting)));

        assertThrows(IllegalStateException.class, () -> initializer.initialize());
        assertEquals("Admin-managed different discussion", discussions.get(10L).getFirst().getTitle());
    }

    private List<LearningStageContent> canonicalStages(Long discussionId) {
        List<String> codes = List.of("PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST");
        List<LearningStageContent> rows = new ArrayList<>();
        for (int index = 0; index < codes.size(); index++) {
            LearningStageContent stage = new LearningStageContent();
            stage.setDiscussion(new Discussion());
            stage.setStageCode(codes.get(index));
            stage.setDisplayOrder((short) (index + 1));
            stage.setContentBody("");
            rows.add(stage);
        }
        return rows;
    }
}
