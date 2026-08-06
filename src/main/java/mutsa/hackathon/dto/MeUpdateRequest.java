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

        @NotNull(message = "AI 기억 활용 동의 여부는 필수입니다.")
        Boolean aiMemoryConsent

) {
}