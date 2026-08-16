package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record MeUpdateRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(
                min = 2,
                max = 8,
                message = "닉네임은 2자 이상 8자 이하로 입력해야 합니다."
        )
        String nickname,

        @NotBlank(message = "현재 하는 일은 필수입니다.")
        @Size(
                max = 30,
                message = "현재 하는 일은 30자 이하로 입력해야 합니다."
        )
        String job,

        @NotNull(message = "일기 알림 시간은 필수입니다.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime reminderTime,

        /*
         * 기존 프론트 요청과의 하위 호환을 위해 optional.
         * 생략하면 현재 사용자의 설정을 유지하며,
         * 신규 사용자의 기본값은 00:00.
         */
        @JsonFormat(pattern = "HH:mm")
        LocalTime dayStartTime,

        @NotNull(message = "AI 기억 활용 동의 여부는 필수입니다.")
        Boolean aiMemoryConsent

) {

        /**
         * 기존 테스트/내부 호출의 4개 인자 생성자 호환.
         * dayStartTime을 생략한 PATCH 요청과 동일하게 처리.
         */
        public MeUpdateRequest(
                String nickname,
                String job,
                LocalTime reminderTime,
                Boolean aiMemoryConsent
        ) {
                this(
                        nickname,
                        job,
                        reminderTime,
                        null,
                        aiMemoryConsent
                );
        }
}
