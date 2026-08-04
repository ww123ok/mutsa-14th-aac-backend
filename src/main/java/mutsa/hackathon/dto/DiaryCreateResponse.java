package mutsa.hackathon.dto;

import mutsa.hackathon.domain.Diary;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryCreateResponse(
        Long diaryId,
        LocalDate recordedDate,
        String content,
        LocalDateTime createdAt
) {
    public static DiaryCreateResponse from(Diary diary) {
        return new DiaryCreateResponse(
                diary.getId(),
                diary.getRecordedDate(),
                diary.getContent(),
                diary.getCreatedAt() // BaseEntity에서 제공하는 작성 일시
        );
    }
}
