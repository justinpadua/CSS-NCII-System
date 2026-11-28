package com.cssncii.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class AssessmentManagementDtos {
    private AssessmentManagementDtos() {}

    public record AssessmentRequest(
        @NotBlank @Size(max = 30) String assessmentType,
        @NotBlank @Size(max = 200) String title,
        String instructions,
        Boolean active
    ) {}

    public record AssessmentUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        String instructions,
        @NotNull Boolean active
    ) {}

    public record AssessmentResponse(
        Long id, Long discussionId, Long competencyId, String assessmentType,
        String title, String instructions, BigDecimal passPercentage, boolean active
    ) {}

    public record ChoiceInput(
        @NotBlank @Size(max = 65_535) String choiceText,
        @NotNull Integer displayOrder
    ) {}

    public record MatchingPairInput(@NotBlank @Size(max = 65_535) String prompt,
                                    @NotBlank @Size(max = 65_535) String answerText) {}

    public record MatchingOptionInput(Long id, @NotBlank @Size(max = 65_535) String optionText) {}

    public record QuestionConfigInput(
        List<@NotBlank @Size(max = 65_535) String> acceptedAnswers,
        Integer requiredAnswerCount,
        Boolean ordered,
        List<@Valid MatchingPairInput> matchingPairs,
        List<@NotBlank @Size(max = 65_535) String> distractors,
        List<@Valid MatchingOptionInput> sharedOptions
    ) {
        public QuestionConfigInput(List<String> acceptedAnswers, Integer requiredAnswerCount, Boolean ordered,
                                   List<MatchingPairInput> matchingPairs, List<String> distractors) {
            this(acceptedAnswers, requiredAnswerCount, ordered, matchingPairs, distractors, null);
        }
    }

    public record QuestionCreateRequest(
        @NotBlank @Size(max = 40) String questionType,
        @NotBlank String questionText,
        @NotNull BigDecimal points,
        @NotNull Long displayOrder,
        @NotNull @Size(min = 0) List<@Valid ChoiceInput> choices,
        List<Integer> correctChoiceIndexes,
        Boolean correctValue,
        @Valid QuestionConfigInput questionConfig
    ) {}

    public record QuestionUpdateRequest(
        @NotBlank @Size(max = 40) String questionType,
        @NotBlank String questionText,
        @NotNull BigDecimal points,
        @NotNull Long displayOrder,
        List<Long> correctChoiceIds,
        Boolean correctValue,
        @Valid QuestionConfigInput questionConfig
    ) {}

    public record QuestionResponse(
        Long id, Long assessmentId, String questionType, String questionText,
        BigDecimal points, long displayOrder, List<Long> correctChoiceIds,
        Boolean correctValue, List<ChoiceResponse> choices,
        String questionConfig, String questionConfiguration
    ) {}

    public record ChoiceRequest(
        @NotBlank @Size(max = 65_535) String choiceText,
        @NotNull Integer displayOrder
    ) {}

    public record ChoiceResponse(Long id, Long questionId, String choiceText, int displayOrder, boolean correct) {}
}
