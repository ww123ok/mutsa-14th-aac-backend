package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.AppUser;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record MeResponse(
        Long id,
        String nickname,
        String email,
        String profileImage,
        String job,

        @JsonFormat(pattern = "HH:mm")
        LocalTime reminderTime,

        boolean aiMemoryConsent,
        boolean onboardingCompleted,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime onboardingCompletedAt,

        int credit
) {

    public static MeResponse from(AppUser user) {
        return new MeResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImage(),
                user.getJob(),
                user.getDiaryReminderTime(),
                user.isAiMemoryConsent(),
                user.isOnboardingCompleted(),
                user.getOnboardingCompletedAt(),
                user.getCredit()
        );
    }
}