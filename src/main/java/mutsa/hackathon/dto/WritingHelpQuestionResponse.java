package mutsa.hackathon.dto;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.service.WritingHelpQuestionContextType;

import java.time.LocalDate;

public record WritingHelpQuestionResponse(
        Long questionId,
        LocalDate askedDate,
        int questionOrder,
        int remainingCount,
        String questionText,
        String generationSource,
        String contextType
) {

    public static WritingHelpQuestionResponse from(
            AiQuestion question,
            int dailyLimit,
            WritingHelpQuestionContextType contextType
    ) {
        return new WritingHelpQuestionResponse(
                question.getId(),
                question.getAskedDate(),
                question.getQuestionOrder(),
                Math.max(
                        0,
                        dailyLimit
                                - question.getQuestionOrder()
                ),
                question.getQuestionText(),
                question.getGenerationSource().name(),
                contextType.name()
        );
    }
}