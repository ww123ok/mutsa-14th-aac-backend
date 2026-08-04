package mutsa.hackathon.dto;

import mutsa.hackathon.domain.Diary;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryResponse(
        Long diaryId,
        LocalDate recordedDate,
        String content,
        LocalDateTime createdAt
) {
    public static DiaryResponse from(Diary diary) {
        return new DiaryResponse(
                diary.getId(),
                diary.getRecordedDate(),
                diary.getContent(),
                diary.getCreatedAt() // BaseEntity에서 제공하는 작성 일시
        );
    }
}
