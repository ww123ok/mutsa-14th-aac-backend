package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.DiaryDraft;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryDraftResponse(
        Long draftId,
        LocalDate recordedDate,
        String content,
        boolean personalizationUsesDiaryContent,

        @JsonFormat(
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        LocalDateTime editingActiveUntil,

        @JsonFormat(
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        LocalDateTime updatedAt
) {
    public static DiaryDraftResponse from(
            DiaryDraft draft
    ) {
        return new DiaryDraftResponse(
                draft.getId(),
                draft.getRecordedDate(),
                draft.getContent(),
                draft.shouldUseDiaryContentForPersonalization(),
                draft.getEditingActiveUntil(),
                draft.getUpdatedAt()
        );
    }
}
