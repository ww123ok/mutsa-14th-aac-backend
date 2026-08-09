package mutsa.hackathon.service;

/**
 * 일기 저장 트랜잭션이 색상 보상 생성을 요청할 때
 * 발행하는 내부 애플리케이션 이벤트
 */
public record DiaryRewardGenerationRequested(
        Long rewardId
) {

    public DiaryRewardGenerationRequested {
        if (rewardId == null) {
            throw new IllegalArgumentException(
                    "색상 보상 식별자는 필수입니다."
            );
        }
    }
}