package com.cssncii.service;

import com.cssncii.entity.Competency;
import com.cssncii.entity.Discussion;
import com.cssncii.entity.LearningStageContent;
import com.cssncii.entity.Topic;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates the documented initial COC structure while preserving existing records and content. */
@Service
public class CourseStructureInitializationService {
    private static final List<String> STAGE_CODES = List.of(
        "PRE_TEST", "DEFINITIONS", "EXPLANATION", "EXAMPLES", "VIDEO_LESSONS", "ACTIVITY", "POST_TEST"
    );
    private static final List<CourseSeed> COURSES = List.of(
        new CourseSeed("COC1", "COC 1", 1L, List.of(
            new DiscussionSeed("Discuss fundamental concepts of computer systems servicing", List.of(
                "Fundamentals concepts of computers (software, hardware PC)",
                "Occupational Health Standards (OHS)",
                "Introduce the needed tools and equipment",
                "Business Costings (Repair, Installing, Reformat, etc.)",
                "Creating a small computer shop (needs/structure to have a standard computer shop by size)",
                "Costing computation, tools and equipment checklist, safety compliance plan, and a short career pathway reflection"
            )),
            new DiscussionSeed("Discuss elements of computer systems", List.of(
                "Computer parts, software, peopleware, and dataware",
                "Identify and differentiate system and application software"
            )),
            new DiscussionSeed("Perform disassembly and assembly of computer hardware", List.of(
                "Internal parts of system unit",
                "How to disassemble system unit PC (step-by-step procedure)",
                "How to assemble system unit PC (step-by-step procedure)",
                "How to troubleshoot a malfunctioning computer system"
            )),
            new DiscussionSeed("Create portable bootable device", List.of(
                "How to create a functional ISO image and bootable device",
                "How to create ISO image using third-party application or DOS commands"
            )),
            new DiscussionSeed("Perform installation of system software and application software", List.of(
                "Apply BIOS/UEFI configuration, disk management, and driver installation to set up a fully functional system",
                "Configure BIOS/UEFI settings, install an operating system, manage disk partitions, and install all necessary system and device drivers"
            )),
            new DiscussionSeed("Perform computer testing and documentation", List.of(
                "Discuss how to apply stress testing techniques to evaluate system performance and stability",
                "Perform a stress or burning test on a computer system, record the results, and analyze any issues observed during testing"
            ))
        )),
        new CourseSeed("COC2", "COC 2", 2L, List.of(
            new DiscussionSeed("Discuss the concepts of computer network", List.of(
                "Different types of networking, topologies, architectural design",
                "Assigning IP Addresses (IPv4 or IPv6)",
                "Difference between peer-to-peer computer and client-server computer"
            )),
            new DiscussionSeed("Perform setting up of computer network", List.of(
                "Different types of LAN cable (Straight through-A, Straight through-B, Cross-over cable) and how to create them",
                "How to apply network planning and implementation skills using proper tools, devices, and cable management techniques"
            )),
            new DiscussionSeed("Perform network testing and documentation", List.of(
                "How to apply connectivity test using PING"
            ))
        ))
    );

    private final CourseManagementService courseManagementService;
    private final LearningContentService learningContentService;

    public CourseStructureInitializationService(
        CourseManagementService courseManagementService,
        LearningContentService learningContentService
    ) {
        this.courseManagementService = courseManagementService;
        this.learningContentService = learningContentService;
    }

    @Transactional
    public InitializationResult initialize() {
        int competencies = 0;
        int discussions = 0;
        int topics = 0;
        int createdTopics = 0;
        int learningStages = 0;

        for (CourseSeed courseSeed : COURSES) {
            Competency competency = findOrCreateCompetency(courseSeed);
            competencies++;
            List<Discussion> existingDiscussions = new java.util.ArrayList<>(courseManagementService
                .getDiscussionsByCompetency(competency.getId()));

            for (int discussionIndex = 0; discussionIndex < courseSeed.discussions().size(); discussionIndex++) {
                DiscussionSeed discussionSeed = courseSeed.discussions().get(discussionIndex);
                long order = discussionIndex + 1L;
                Discussion discussion = findOrCreateDiscussion(competency, existingDiscussions,
                    discussionSeed.title(), order);
                discussions++;

                // createDiscussion also initializes new records; this idempotent call verifies and repairs all seven.
                learningStages += ensureStages(discussion.getId());
                createdTopics += ensureTopics(discussion, discussionSeed.topics());
                topics += discussionSeed.topics().size();
                if (existingDiscussions.stream().noneMatch(item -> Objects.equals(item.getId(), discussion.getId()))) {
                    existingDiscussions.add(discussion);
                }
            }
            if (existingDiscussions.size() != courseSeed.discussions().size()) {
                throw new IllegalStateException(courseSeed.code()
                    + " contains additional Discussions beyond the documented structure. Review them before initialization.");
            }
        }

        if (learningStages != 63) {
            throw new IllegalStateException("Course structure initialization expected 63 canonical learning stages.");
        }
        return new InitializationResult(competencies, discussions, topics, learningStages, createdTopics);
    }

    private Competency findOrCreateCompetency(CourseSeed seed) {
        List<Competency> matches = courseManagementService.getAllCompetencies().stream()
            .filter(item -> seed.code().equalsIgnoreCase(item.getCode()))
            .toList();
        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple competency records use " + seed.code() + ". Resolve the duplicate before initialization.");
        }
        if (matches.isEmpty()) {
            return courseManagementService.createCompetency(seed.code(), seed.title(), null, seed.displayOrder());
        }

        Competency competency = matches.get(0);
        if (!seed.title().equals(competency.getTitle())) {
            throw new IllegalStateException("The existing " + seed.code() + " title differs from the documented course title. Resolve it before initialization.");
        }
        if (!competency.isActive()) {
            competency = courseManagementService.setCompetencyActive(competency.getId(), true);
        }
        return competency;
    }

    private Discussion findOrCreateDiscussion(
        Competency competency, List<Discussion> existing, String title, long order
    ) {
        List<Discussion> atOrder = existing.stream().filter(item -> item.getDisplayOrder() == order).toList();
        if (atOrder.size() > 1) {
            throw new IllegalStateException("Multiple Discussions use order " + order + " in " + competency.getCode() + ".");
        }
        if (!atOrder.isEmpty()) {
            Discussion discussion = atOrder.get(0);
            if (!title.equals(discussion.getTitle())) {
                throw new IllegalStateException("Discussion order " + order + " in " + competency.getCode()
                    + " is already used by a different title. Resolve it before initialization.");
            }
            if (!discussion.isActive()) {
                discussion = courseManagementService.setDiscussionActive(discussion.getId(), true);
            }
            return discussion;
        }

        boolean titleAlreadyExists = existing.stream().anyMatch(item -> title.equals(item.getTitle()));
        if (titleAlreadyExists) {
            throw new IllegalStateException("The documented Discussion title already exists at a different order in "
                + competency.getCode() + ". Resolve it before initialization.");
        }
        return courseManagementService.createDiscussion(competency.getId(), title, null, order);
    }

    private int ensureTopics(Discussion discussion, List<String> requiredTitles) {
        List<Topic> existing = new java.util.ArrayList<>(
            courseManagementService.getTopicsByDiscussion(discussion.getId()));
        int created = 0;
        for (int index = 0; index < requiredTitles.size(); index++) {
            String title = requiredTitles.get(index);
            long order = index + 1L;
            List<Topic> atOrder = existing.stream().filter(item -> item.getDisplayOrder() == order).toList();
            if (atOrder.size() > 1) {
                throw new IllegalStateException("Multiple topics use order " + order + " in Discussion " + discussion.getId() + ".");
            }
            if (!atOrder.isEmpty()) {
                Topic topic = atOrder.get(0);
                if (!title.equals(topic.getTitle())) {
                    throw new IllegalStateException("Topic order " + order + " in Discussion " + discussion.getId()
                        + " is already used by a different title. Resolve it before initialization.");
                }
                if (!topic.isActive()) {
                    courseManagementService.setTopicActive(topic.getId(), true);
                }
                continue;
            }
            if (existing.stream().anyMatch(item -> title.equals(item.getTitle()))) {
                throw new IllegalStateException("A documented topic already exists at a different order in Discussion "
                    + discussion.getId() + ". Resolve it before initialization.");
            }
            Topic createdTopic = courseManagementService.createTopic(discussion.getId(), title, null, order);
            existing.add(createdTopic);
            created++;
        }
        if (existing.size() != requiredTitles.size()) {
            throw new IllegalStateException("Discussion " + discussion.getId()
                + " contains additional topics beyond the documented structure. Review them before initialization.");
        }
        return created;
    }

    private int ensureStages(Long discussionId) {
        List<LearningStageContent> stages = learningContentService.initializeDiscussionStages(discussionId);
        if (stages.size() != STAGE_CODES.size()) {
            throw new IllegalStateException("Discussion " + discussionId + " must have exactly seven learning stages.");
        }
        for (int index = 0; index < STAGE_CODES.size(); index++) {
            LearningStageContent stage = stages.get(index);
            if (!STAGE_CODES.get(index).equals(stage.getStageCode()) || stage.getDisplayOrder() != index + 1) {
                throw new IllegalStateException("Discussion " + discussionId + " has an invalid canonical learning-stage order.");
            }
        }
        return stages.size();
    }

    private record CourseSeed(String code, String title, long displayOrder, List<DiscussionSeed> discussions) {}
    private record DiscussionSeed(String title, List<String> topics) {}

    public record InitializationResult(
        int competencyCount,
        int discussionCount,
        int topicCount,
        int learningStageCount,
        int createdTopicCount
    ) {}
}
