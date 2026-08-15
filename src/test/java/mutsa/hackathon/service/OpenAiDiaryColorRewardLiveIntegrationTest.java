package mutsa.hackathon.service;

import mutsa.hackathon.domain.DiaryRewardPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 OpenAI Responses API로 색 보상 생성을 검증하는
 * 수동 통합 테스트.
 * 일반 clean test에서는 실행되지 않고
 * OPENAI_LIVE_TEST=true일 때만 실제 API를 호출.
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
    void 실제_OpenAI가_예약색을_피하고_일기기반_키워드와_HEX를_생성한다() {
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

        assertFalse(
                DiaryRewardPolicy
                        .isReservedColor(
                                reward.colorHex()
                        )
        );

        assertNotNull(
                reward.keywords()
        );

        assertTrue(
                reward.keywords().size() >= 1
                        && reward.keywords().size() <= 3
        );

        assertTrue(
                reward.keywords()
                        .stream()
                        .allMatch(keyword ->
                                keyword != null
                                        && !keyword.isBlank()
                                        && keyword.length() <= 20
                                        && !keyword.startsWith("#")
                                        && !keyword.matches(".*\\s+.*")
                        )
        );

        List<String> concreteTopicTerms =
                List.of(
                        "팀원",
                        "백엔드",
                        "오류",
                        "테스트",
                        "기능",
                        "프로젝트",
                        "학교",
                        "회의",
                        "과제",
                        "시험"
                );

        assertTrue(
                reward.keywords()
                        .stream()
                        .noneMatch(keyword ->
                                concreteTopicTerms
                                        .stream()
                                        .anyMatch(keyword::contains)
                        ),
                "키워드는 구체적인 사건/주제어보다 감정·감각·분위기를 표현해야 합니다: "
                        + reward.keywords()
        );

        assertNotNull(
                reward.commentSummary()
        );

        assertTrue(
                reward.commentSummary()
                        .endsWith(".")
        );

        assertFalse(
                reward.commentSummary()
                        .contains("적어주셨어요")
        );

        System.out.println(
                "[OpenAI 실제 색 보상] "
                        + reward.colorHex()
                        + " "
                        + reward.keywords()
                        + " / "
                        + reward.commentSummary()
        );
    }
}