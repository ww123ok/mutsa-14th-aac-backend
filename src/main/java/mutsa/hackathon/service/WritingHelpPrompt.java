package mutsa.hackathon.service;

import java.util.List;

public record WritingHelpPrompt(
        WritingHelpQuestionContextType contextType,
        String currentContent,
        List<WritingHelpRecentDiary> recentDiaries,
        int questionOrder,
        List<String> previousQuestions
) {
    public WritingHelpPrompt {
        if (contextType == null) {
            throw new IllegalArgumentException(
                    "작성 도움 질문 문맥 유형은 필수입니다."
            );
        }

        if (questionOrder < 1 || questionOrder > 3) {
            throw new IllegalArgumentException(
                    "작성 도움 질문 순서는 1부터 3까지여야 합니다."
            );
        }

        currentContent =
                currentContent == null
                        ? null
                        : currentContent.trim();

        recentDiaries =
                recentDiaries == null
                        ? List.of()
                        : List.copyOf(recentDiaries);

        previousQuestions =
                previousQuestions == null
                        ? List.of()
                        : List.copyOf(previousQuestions);

        if (
                contextType
                        == WritingHelpQuestionContextType.CURRENT_DRAFT
                        && (
                        currentContent == null
                                || currentContent.isBlank()
                )
        ) {
            throw new IllegalArgumentException(
                    "작성 중 일기 기반 질문에는 현재 일기 내용이 필요합니다."
            );
        }

        if (
                contextType
                        == WritingHelpQuestionContextType.RECENT_CONTEXT
                        && recentDiaries.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "최근 맥락 기반 질문에는 최근 일기가 필요합니다."
            );
        }
    }
}
