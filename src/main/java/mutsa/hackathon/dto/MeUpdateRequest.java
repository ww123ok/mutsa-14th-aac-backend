package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MeUpdateRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(
                min = 2,
                max = 8,
                message = "닉네임은 2자 이상 8자 이하로 입력해야 합니다."
        )
        String nickname,

        @NotNull(message = "일기 알림 시간은 필수입니다.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime reminderTime,

        @NotNull(message = "하루 전환 시간은 필수입니다.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime dayStartTime,

        @NotNull(message = "AI 기억 활용 동의 여부는 필수입니다.")
        Boolean aiMemoryConsent

) {

        /** 기존 내부 호출과의 호환용 생성자입니다. 직업 값은 더 이상 저장하지 않습니다. */
        public MeUpdateRequest(
                String nickname,
                String ignoredJob,
                LocalTime reminderTime,
                LocalTime dayStartTime,
                Boolean aiMemoryConsent
        ) {
                this(
                        nickname,
                        reminderTime,
                        dayStartTime,
                        aiMemoryConsent
                );
        }

        /** 기존 내부 호출과의 호환용 생성자입니다. 하루 전환 시간은 자정으로 처리합니다. */
        public MeUpdateRequest(
                String nickname,
                String ignoredJob,
                LocalTime reminderTime,
                Boolean aiMemoryConsent
        ) {
                this(nickname, reminderTime, LocalTime.MIDNIGHT, aiMemoryConsent);
        }
}
