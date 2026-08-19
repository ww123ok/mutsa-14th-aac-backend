package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiaryCreateRequest(

        @NotBlank(
                message = "일기 내용은 필수입니다."
        )
        String content,

        /**
         * 오늘 일기에서 추출한 정보를
         * 앞으로의 작성 도움 질문 개인화에
         * 활용할지에 대한 사용자 선택.
         * 기존 프론트의 reflectionUsesDiaryContent는
         * 마이그레이션 기간 동안 입력 alias로만 지원.
         */
        @JsonAlias("reflectionUsesDiaryContent")
        @NotNull(
                message = "오늘 일기 내용을 앞으로의 질문에 반영할지 선택해야 합니다."
        )
        Boolean personalizationUsesDiaryContent,

        /**
         * 임시저장 기반 완료 시 선택적으로 전달.
         * 하루 경계를 넘긴 뒤에도 최초 작성 날짜를 보존하는 기준점으로 사용한다.
         */
        Long draftId

) {

        /**
         * 기존 내부 테스트 및 호출 호환용 생성자.
         */
        public DiaryCreateRequest(
                String content
        ) {
                this(
                        content,
                        true,
                        null
                );
        }

        public DiaryCreateRequest(
                String content,
                Boolean personalizationUsesDiaryContent
        ) {
                this(
                        content,
                        personalizationUsesDiaryContent,
                        null
                );
        }

        public boolean
        shouldUseDiaryContentForPersonalization() {
                return Boolean.TRUE.equals(
                        personalizationUsesDiaryContent
                );
        }
}