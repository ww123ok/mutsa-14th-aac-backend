package mutsa.hackathon.service;

import java.util.List;

public record WritingHelpPrompt(
        String nickname,
        String job,
        String memoryProfile,
        int questionOrder,
        List<String> previousQuestions,
        List<String> earlierQuestions
) {
    public WritingHelpPrompt {
        if (questionOrder < 1 || questionOrder > 3) {
            throw new IllegalArgumentException(
                    "작성 도움 질문 순서는 1부터 3까지여야 합니다."
            );
        }

        previousQuestions =
                previousQuestions == null
                        ? List.of()
                        : List.copyOf(
                        previousQuestions
                );

        earlierQuestions =
                earlierQuestions == null
                        ? List.of()
                        : List.copyOf(
                        earlierQuestions
                );
    }
}
