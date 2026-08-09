package mutsa.hackathon.dto;

public record WritingHelpStatusResponse(
        int dailyLimit,
        int usedCount,
        int remainingCount,
        boolean available
) {

    public static WritingHelpStatusResponse of(
            int dailyLimit,
            long usedCount
    ) {
        if (dailyLimit < 1) {
            throw new IllegalArgumentException(
                    "작성 도움 질문 일일 제한은 1 이상이어야 합니다."
            );
        }

        if (usedCount < 0) {
            throw new IllegalArgumentException(
                    "작성 도움 질문 사용 횟수는 0 이상이어야 합니다."
            );
        }

        int normalizedUsedCount =
                Math.toIntExact(usedCount);

        int remainingCount =
                Math.max(
                        0,
                        dailyLimit - normalizedUsedCount
                );

        return new WritingHelpStatusResponse(
                dailyLimit,
                normalizedUsedCount,
                remainingCount,
                remainingCount > 0
        );
    }
}