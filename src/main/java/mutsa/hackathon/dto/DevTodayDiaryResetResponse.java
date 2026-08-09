package mutsa.hackathon.dto;

import java.time.LocalDate;

public record DevTodayDiaryResetResponse(
        boolean deleted,
        Long diaryId,
        LocalDate recordedDate,
        long deletedRewardCount,
        long deletedQuestionCount,
        long deletedMemoryCount
) {

    public static DevTodayDiaryResetResponse notFound(
            LocalDate recordedDate
    ) {
        return new DevTodayDiaryResetResponse(
                false,
                null,
                recordedDate,
                0,
                0,
                0
        );
    }

    public static DevTodayDiaryResetResponse deleted(
            Long diaryId,
            LocalDate recordedDate,
            long deletedRewardCount,
            long deletedQuestionCount,
            long deletedMemoryCount
    ) {
        return new DevTodayDiaryResetResponse(
                true,
                diaryId,
                recordedDate,
                deletedRewardCount,
                deletedQuestionCount,
                deletedMemoryCount
        );
    }
}