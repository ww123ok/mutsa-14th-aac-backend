package mutsa.hackathon.service;

import java.time.LocalDate;

public record WritingHelpRecentDiary(
        LocalDate recordedDate,
        String content
) {
    public WritingHelpRecentDiary {
        if (recordedDate == null) {
            throw new IllegalArgumentException(
                    "최근 일기 날짜는 필수입니다."
            );
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "최근 일기 내용은 필수입니다."
            );
        }

        content = content.trim();
    }
}
