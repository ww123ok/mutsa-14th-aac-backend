package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 OpenAI Responses API로 색 보상 생성을 검증하는
 * 수동 통합 테스트.
 * 일반 clean test에서는 실행되지 않음
 * OPENAI_LIVE_TEST=true일 때만 실제 API를 한 번 호출함.
 */
@SpringBootTest(
        properties = {
                "app.openai.reward-enabled=true"
        }
)
@EnabledIfEnvironmentVariable(
        named = "OPENAI_LIVE_TEST",
        matches = "true"
)
class OpenAiDiaryColorRewardLiveIntegrationTest {

    @Autowired
    private DiaryColorRewardGenerator
            diaryColorRewardGenerator;

    @Test
    void 실제_OpenAI가_일기에_어울리는_색_보상을_생성한다() {
        assertInstanceOf(
                OpenAiDiaryColorRewardGenerator.class,
                diaryColorRewardGenerator
        );

        DiaryColorReward reward =
                diaryColorRewardGenerator.generate(
                        """
                        오늘 팀원들과 백엔드 오류를 하나씩 해결했다.
                        처음에는 막막했지만 테스트가 모두 성공했고,
                        지금까지 구현한 기능이 실제로 연결되는 것을 보니
                        뿌듯하고 마음이 한결 편안해졌다.
                        """
                );

        assertNotNull(
                reward
        );

        assertNotNull(
                reward.colorHex()
        );

        assertTrue(
                reward.colorHex()
                        .matches(
                                "^#[0-9A-F]{6}$"
                        )
        );

        assertNotNull(
                reward.colorName()
        );

        assertTrue(
                !reward.colorName()
                        .isBlank()
        );

        assertTrue(
                reward.colorName()
                        .length()
                        <= 100
        );

        System.out.println(
                "[OpenAI 실제 색 보상] "
                        + reward.colorName()
                        + " "
                        + reward.colorHex()
        );
    }
}