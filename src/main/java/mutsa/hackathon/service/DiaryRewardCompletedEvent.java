package mutsa.hackathon.service;

import java.time.LocalDate;

/**
 * 일간 색 보상이 최종 완료된 뒤 주간 보상 지연 생성을 재검사하기 위한 이벤트.
 */
public record DiaryRewardCompletedEvent(
        Long userId,
        Long diaryId,
        Long rewardId,
        LocalDate recordedDate
) {
}
