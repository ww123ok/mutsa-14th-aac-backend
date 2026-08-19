package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiaryDraftUpsertRequest(

        @NotBlank(
                message = "임시 저장할 일기 내용은 필수입니다."
        )
        String content,

        @JsonAlias("reflectionUsesDiaryContent")
        @NotNull(
                message = "일기 내용을 앞으로의 질문에 반영할지 선택해야 합니다."
        )
        Boolean personalizationUsesDiaryContent
) {
    public boolean
    shouldUseDiaryContentForPersonalization() {
        return Boolean.TRUE.equals(
                personalizationUsesDiaryContent
        );
    }
}
