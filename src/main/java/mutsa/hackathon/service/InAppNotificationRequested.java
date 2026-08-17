package mutsa.hackathon.service;

import mutsa.hackathon.domain.NotificationType;

import java.time.LocalDate;

public record InAppNotificationRequested(
        Long userId,
        NotificationType type,
        String message,
        Long referenceId,
        String dedupKey
) {

    public static InAppNotificationRequested diaryReminder(
            Long userId,
            LocalDate logicalDate
    ) {
        return new InAppNotificationRequested(
                userId,
                NotificationType.DIARY_REMINDER,
                "작성을 완료해 오늘의 색을 받아보세요 :)",
                null,
                "DIARY_REMINDER:" + userId + ":" + logicalDate
        );
    }

    public static InAppNotificationRequested
    experienceFragmentArrived(
            Long userId,
            Long arrivalId
    ) {
        return new InAppNotificationRequested(
                userId,
                NotificationType.EXPERIENCE_FRAGMENT_ARRIVED,
                "나에게 전달된 새로운 경험조각이 있어요.",
                arrivalId,
                "EXPERIENCE_FRAGMENT_ARRIVED:" + arrivalId
        );
    }

    public static InAppNotificationRequested
    experienceFragmentFeedback(
            Long senderId,
            Long deliveryId,
            Long shareId
    ) {
        return new InAppNotificationRequested(
                senderId,
                NotificationType.EXPERIENCE_FRAGMENT_FEEDBACK,
                "내가 보낸 경험조각에 새로운 반응이 도착했어요.",
                shareId,
                "EXPERIENCE_FRAGMENT_FEEDBACK:" + deliveryId
        );
    }

    public static InAppNotificationRequested
    weeklyRewardCompleted(
            Long userId,
            Long rewardId
    ) {
        return new InAppNotificationRequested(
                userId,
                NotificationType.WEEKLY_REWARD_COMPLETED,
                "이번 주의 기록이 한 장의 이미지로 완성됐어요.",
                rewardId,
                "WEEKLY_REWARD_COMPLETED:" + rewardId
        );
    }
}
