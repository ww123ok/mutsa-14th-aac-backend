package mutsa.hackathon.service;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OpenAI 색 보상 기능이 비활성화되어 있을 때
 * 등록되는 임시 생성기.
 * 실제 생성 대신 예외를 발생시켜 보상을 FAILED로 변경하도록 함.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.openai",
        name = "reward-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class FallbackDiaryColorRewardGenerator
        implements DiaryColorRewardGenerator {

    @Override
    public DiaryColorReward generate(
            String diaryContent
    ) {
        throw new IllegalStateException(
                "OpenAI 색 보상 생성 기능이 비활성화되어 있습니다."
        );
    }
}