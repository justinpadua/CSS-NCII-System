package com.cssncii.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Reads the reviewed Markdown assessment sources without importing instructor-only prose. */
@Component
public class ApprovedAssessmentSourceParser {
    private static final Pattern NUMBERED = Pattern.compile("^\\s*(?:###\\s*)?(\\d+)\\.\\s*(.*)$");
    private static final Pattern CHOICE = Pattern.compile("^\\s*-?\\s*([A-D])\\.\\s*(.*)$");
    private static final Pattern DISCUSSION_KEY_HEADER = Pattern.compile(
        "^##\\s+COC-([12])\\s+—\\s+Lesson-(\\d+)\\s+—\\s+(01-Pretest|07-Posttest)\\.md\\s*$");
    private static final Pattern GENERAL_KEY_HEADER = Pattern.compile(
        "^##\\s+INSTRUCTOR KEY\\s+—\\s+(coc[12]-generalized-post-test\\.md)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_ROW = Pattern.compile("^\\s*\\|\\s*(\\d+)\\s*\\|\\s*(.*?)\\s*\\|.*$");
    private static final Pattern GENERAL_TABLE_ROW = Pattern.compile(
        "^\\s*\\|\\s*(\\d+)\\s*\\|\\s*\\([^)]*\\)\\s*\\|\\s*([A-D])\\s*\\|.*$");
    private static final Pattern KEY_LETTER = Pattern.compile("^\\s*([A-D])(?:\\s+[—–-].*)?$", Pattern.CASE_INSENSITIVE);

    public ParsedSet parse(Path root, String cocCode, int discussionOrder, String type) throws IOException {
        boolean generalized = "GENERALIZED_POST_TEST".equals(type);
        Path source = generalized
            ? root.resolve("assessments/generalized-post-tests/" + cocCode.toLowerCase(Locale.ROOT)
                + "-generalized-post-test.md")
            : root.resolve("COC-" + cocCode.substring(3) + "/Lesson-%02d/%s".formatted(
                discussionOrder, "PRE_TEST".equals(type) ? "01-Pretest.md" : "07-Posttest.md"));
        if (!Files.isRegularFile(source)) throw new IllegalStateException("Missing assessment source: " + source);
        List<QuestionSource> questions = parseQuestionBlocks(Files.readAllLines(source, StandardCharsets.UTF_8), generalized);
        Map<Integer, String> keys = parseKeys(root.resolve("instructor-only/assessment-answer-keys.md"), cocCode,
            discussionOrder, type, generalized);
        if (!generalized && "COC2".equals(cocCode) && discussionOrder == 1 && "PRE_TEST".equals(type)) {
            questions = questions.stream().filter(q -> List.of(1, 2, 4).contains(q.sourceNumber())).toList();
            Map<Integer, String> selected = new LinkedHashMap<>();
            for (int n : List.of(1, 2, 4)) {
                if (!keys.containsKey(n)) throw new IllegalStateException("Missing approved COC 2 Discussion 1 Pre-test key for source item " + n);
                selected.put(n, keys.get(n));
            }
            keys = selected;
        }
        if (questions.isEmpty() || questions.size() != keys.size()) {
            throw new IllegalStateException("Question/key count mismatch for " + cocCode + " D" + discussionOrder + " " + type);
        }
        List<QuestionSource> configured = new ArrayList<>();
        for (QuestionSource question : questions) {
            int answerRow = "COC2".equals(cocCode) && discussionOrder == 1
                && "PRE_TEST".equals(type) ? question.sourceNumber() : configured.size() + 1;
            String key = keys.get(answerRow);
            if (key == null) throw new IllegalStateException("Missing key for " + source + " item " + question.sourceNumber());
            int correct = question.choices().stream().map(ChoiceSource::letter).toList().indexOf(key);
            if (correct < 0) throw new IllegalStateException("Key does not match a choice for " + source + " item " + question.sourceNumber());
            if (question.choices().size() != 4) throw new IllegalStateException("Expected four choices for " + source + " item " + question.sourceNumber());
            configured.add(new QuestionSource(question.sourceNumber(), question.questionText(), question.choices(), correct));
        }
        String instructions = extractInstructions(Files.readAllLines(source, StandardCharsets.UTF_8));
        return new ParsedSet(source, "COC2".equals(cocCode) ? "COC 2" : "COC 1", cocCode,
            discussionOrder, type, instructions, List.copyOf(configured));
    }

    private List<QuestionSource> parseQuestionBlocks(List<String> lines, boolean generalized) {
        List<QuestionSource> result = new ArrayList<>();
        String question = null;
        List<ChoiceSource> choices = new ArrayList<>();
        int number = 0;
        for (String line : lines) {
            Matcher numbered = NUMBERED.matcher(line);
            boolean questionHeading = numbered.matches() && (!generalized || line.stripLeading().startsWith("###"));
            if (questionHeading) {
                if (question != null) addQuestion(result, number, question, choices);
                number = Integer.parseInt(numbered.group(1));
                question = numbered.group(2).replaceFirst("^\\([^)]*\\)\\s*", "")
                    .replaceAll("\\s+\\([^)]*\\)\\s*$", "").trim();
                choices = new ArrayList<>();
                continue;
            }
            if (question == null) continue;
            Matcher choice = CHOICE.matcher(line);
            if (choice.matches()) choices.add(new ChoiceSource(choice.group(1), choice.group(2).trim()));
            else if (!line.isBlank() && !line.stripLeading().startsWith("#")
                && !line.toLowerCase(Locale.ROOT).startsWith("directions:")) {
                if (choices.isEmpty()) question += " " + line.trim();
            }
        }
        if (question != null) addQuestion(result, number, question, choices);
        return result;
    }

    private void addQuestion(List<QuestionSource> result, int number, String text, List<ChoiceSource> choices) {
        if (text.isBlank() || choices.size() != 4) throw new IllegalStateException("Malformed multiple-choice source item " + number);
        for (int i = 0; i < 4; i++) {
            if (!choices.get(i).letter().equals(String.valueOf((char) ('A' + i))) || choices.get(i).text().isBlank()) {
                throw new IllegalStateException("Choices must be nonempty A-D in source item " + number);
            }
        }
        result.add(new QuestionSource(number, text, List.copyOf(choices), -1));
    }

    private Map<Integer, String> parseKeys(Path keyFile, String coc, int discussion, String type, boolean generalized)
        throws IOException {
        List<String> lines = Files.readAllLines(keyFile, StandardCharsets.UTF_8);
        String expected = generalized
            ? "## INSTRUCTOR KEY — " + coc.toLowerCase(Locale.ROOT) + "-generalized-post-test.md"
            : "## " + coc.replace("COC", "COC-") + " — Lesson-%02d — %s.md".formatted(
                discussion, "PRE_TEST".equals(type) ? "01-Pretest" : "07-Posttest");
        boolean inSection = false;
        Map<Integer, String> result = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.startsWith("## ")) {
                if (inSection) break;
                inSection = line.trim().equals(expected);
                continue;
            }
            if (!inSection) continue;
            Matcher row = (generalized ? GENERAL_TABLE_ROW : TABLE_ROW).matcher(line);
            if (!row.matches()) continue;
            int item = Integer.parseInt(row.group(1));
            String keyCell = generalized ? row.group(2) : row.group(2);
            Matcher letter = KEY_LETTER.matcher(keyCell);
            if (letter.matches()) result.put(item, letter.group(1).toUpperCase(Locale.ROOT));
        }
        return result;
    }

    private String extractInstructions(List<String> lines) {
        return lines.stream().map(String::trim)
            .filter(line -> line.toLowerCase(Locale.ROOT).startsWith("directions:")
                || line.toLowerCase(Locale.ROOT).startsWith("**instructions:**"))
            .findFirst().map(line -> line.replaceFirst("(?i)^\\*\\*Instructions:\\*\\*\\s*", "")
                .replaceFirst("(?i)^Directions:\\s*", "").trim())
            .orElse("Choose the one best answer for each question.");
    }

    public record ChoiceSource(String letter, String text) {}
    public record QuestionSource(int sourceNumber, String questionText, List<ChoiceSource> choices, int correctChoiceIndex) {}
    public record ParsedSet(Path source, String cocName, String competencyCode, int discussionOrder, String type,
                            String instructions, List<QuestionSource> questions) {}
}
