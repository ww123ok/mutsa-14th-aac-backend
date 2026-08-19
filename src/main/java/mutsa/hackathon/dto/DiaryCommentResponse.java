package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.DiaryComment;

import java.time.LocalDateTime;

public record DiaryCommentResponse(
        Long commentId,
        Long diaryId,
        String content,

        @JsonFormat(
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        LocalDateTime createdAt
) {
    public static DiaryCommentResponse from(
            DiaryComment comment
    ) {
        return new DiaryCommentResponse(
                comment.getId(),
                comment.getDiary().getId(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
