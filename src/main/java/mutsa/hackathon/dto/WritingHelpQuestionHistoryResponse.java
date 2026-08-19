package mutsa.hackathon.dto;

import mutsa.hackathon.domain.AiQuestion;

import java.time.LocalDate;

public record WritingHelpQuestionHistoryResponse(
        Long questionId,
        LocalDate askedDate,
        int questionOrder,
        String questionText,
        String generationSource
) {

    public static WritingHelpQuestionHistoryResponse from(
            AiQuestion question
    ) {
        return new WritingHelpQuestionHistoryResponse(
                question.getId(),
                question.getAskedDate(),
                question.getQuestionOrder(),
                question.getQuestionText(),
                question.getGenerationSource().name()
        );
    }
}
