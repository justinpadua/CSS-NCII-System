package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApprovedAssessmentSourceParserTest {
    private final ApprovedAssessmentSourceParser parser = new ApprovedAssessmentSourceParser();
    private final Path root = Path.of("..", "learning-content").toAbsolutePath().normalize();

    @Test
    void parsesAllApprovedDiscussionSetsAndProtectedKeys() throws Exception {
        int total = 0;
        for (String coc : List.of("COC1", "COC2")) {
            int discussions = "COC1".equals(coc) ? 6 : 3;
            for (int discussion = 1; discussion <= discussions; discussion++) {
                for (String type : List.of("PRE_TEST", "DISCUSSION_POST_TEST")) {
                    ApprovedAssessmentSourceParser.ParsedSet parsed = parser.parse(root, coc, discussion, type);
                    int expected = "COC2".equals(coc) && discussion == 2 ? 30 : 10;
                    if ("COC2".equals(coc) && discussion == 1 && "PRE_TEST".equals(type)) expected = 3;
                    assertEquals(expected, parsed.questions().size(), coc + " D" + discussion + " " + type);
                    parsed.questions().forEach(question -> {
                        assertEquals(4, question.choices().size());
                        org.junit.jupiter.api.Assertions.assertTrue(question.correctChoiceIndex() >= 0
                            && question.correctChoiceIndex() < 4);
                    });
                    total += parsed.questions().size();
                }
            }
            ApprovedAssessmentSourceParser.ParsedSet generalized = parser.parse(root, coc, 0, "GENERALIZED_POST_TEST");
            assertEquals(10, generalized.questions().size());
            total += generalized.questions().size();
        }
        assertEquals(233, total);
    }

    @Test
    void selectsOnlyApprovedCoc2DiscussionOnePretestItems() throws Exception {
        var parsed = parser.parse(root, "COC2", 1, "PRE_TEST");
        assertEquals(List.of(1, 2, 4), parsed.questions().stream()
            .map(ApprovedAssessmentSourceParser.QuestionSource::sourceNumber).toList());
    }

    @Test
    void coc1DiscussionOnePretestChoicesMatchApprovedSourceOrderAndKeys() throws Exception {
        var parsed = parser.parse(root, "COC1", 1, "PRE_TEST");
        assertEquals(List.of("Monitor", "Printer", "Keyboard", "Speakers"),
            parsed.questions().get(0).choices().stream().map(ApprovedAssessmentSourceParser.ChoiceSource::text).toList());
        assertEquals(2, parsed.questions().get(0).correctChoiceIndex());
        assertEquals(List.of("Occupational Health and Safety", "Office Hardware Standards",
                "Online Hardware Setup", "Operating Hardware System"),
            parsed.questions().get(1).choices().stream().map(ApprovedAssessmentSourceParser.ChoiceSource::text).toList());
        assertEquals(0, parsed.questions().get(1).correctChoiceIndex());
    }
}
