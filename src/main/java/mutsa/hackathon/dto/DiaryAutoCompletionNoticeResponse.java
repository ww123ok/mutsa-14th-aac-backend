package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.DiaryAutoCompletionNotice;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryAutoCompletionNoticeResponse(
        Long noticeId,
        Long diaryId,
        LocalDate recordedDate,

        @JsonFormat(
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        LocalDateTime autoCompletedAt,

        boolean viewed,

        @JsonFormat(
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        LocalDateTime viewedAt
) {
    public static DiaryAutoCompletionNoticeResponse from(
            DiaryAutoCompletionNotice notice
    ) {
        return new DiaryAutoCompletionNoticeResponse(
                notice.getId(),
                notice.getDiaryId(),
                notice.getRecordedDate(),
                notice.getAutoCompletedAt(),
                notice.isViewed(),
                notice.getViewedAt()
        );
    }
}
