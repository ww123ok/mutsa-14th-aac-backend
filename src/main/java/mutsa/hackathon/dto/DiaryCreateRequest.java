package mutsa.hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiaryCreateRequest(

        @NotBlank(
                message = "일기 내용은 필수입니다."
        )
        String content,

        @NotNull(
                message = "성찰 질문에 일기 내용을 반영할지 선택해야 합니다."
        )
        Boolean reflectionUsesDiaryContent

) {

        /**
         * 기존 백엔드 테스트 코드와 내부 호출의 호환성을 유지
         * HTTP 요청에서는 reflectionUsesDiaryContent를
         * 명시적으로 전달해야 함
         */
        public DiaryCreateRequest(String content) {
                this(content, true);
        }

        public boolean shouldUseDiaryContent() {
                return Boolean.TRUE.equals(
                        reflectionUsesDiaryContent
                );
        }
}