package mutsa.hackathon.dto;

import mutsa.hackathon.domain.AiQuestion;

import java.time.LocalDate;

public record WritingHelpQuestionResponse(
        Long questionId,
        LocalDate askedDate,
        int questionOrder,
        int remainingCount,
        String questionText,
        String generationSource
) {

    public static WritingHelpQuestionResponse from(
            AiQuestion question,
            int dailyLimit
    ) {
        return new WritingHelpQuestionResponse(
                question.getId(),
                question.getAskedDate(),
                question.getQuestionOrder(),
                Math.max(0, dailyLimit - question.getQuestionOrder()),
                question.getQuestionText(),
                question.getGenerationSource().name()
        );
    }
}
