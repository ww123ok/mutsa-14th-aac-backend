package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyImagePromptFactoryTest {

    @Test
    void 실사풍경은_주간요약과_팔레트를_포함한_가로프롬프트를_만든다() {
        WeeklyRewardInsight insight = insight(
                WeeklyVisualCategory.PHOTO_LANDSCAPE,
                "Create one university-area street that connects repeated classes, errands, and walks."
        );

        String prompt = WeeklyImagePromptFactory.buildPrompt(
                insight,
                "#D6A45C, #6A8FB3, #C9878A"
        );

        assertTrue(prompt.contains("SELECTED CATEGORY: PHOTO_LANDSCAPE"));
        assertTrue(prompt.contains("이번 주에는 학교 주변 이동과 산책이 반복됐습니다."));
        assertTrue(prompt.contains("#D6A45C, #6A8FB3, #C9878A"));
        assertTrue(prompt.contains("real photograph captured"));
        assertTrue(prompt.contains("whole week, not one day"));
        assertFalse(prompt.contains("Studio Ghibli imitation"));

        assertEquals(
                "1536x1024",
                WeeklyImagePromptFactory.resolveImageSize(
                        insight.visualCategory(),
                        "1024x1024",
                        "1024x1536",
                        "1536x1024"
                )
        );
    }

    @Test
    void 카테고리에_따라_세로와_정방형_크기를_선택한다() {
        assertEquals(
                "1024x1536",
                WeeklyImagePromptFactory.resolveImageSize(
                        WeeklyVisualCategory.GRAPHIC_POSTER,
                        "1024x1024",
                        "1024x1536",
                        "1536x1024"
                )
        );

        assertEquals(
                "1024x1024",
                WeeklyImagePromptFactory.resolveImageSize(
                        WeeklyVisualCategory.NON_HUMAN_CHARACTER,
                        "1024x1024",
                        "1024x1536",
                        "1536x1024"
                )
        );
    }

    private WeeklyRewardInsight insight(
            WeeklyVisualCategory category,
            String motif
    ) {
        return new WeeklyRewardInsight(
                "학교 주변에서 이어진 한 주",
                "이번 주에는 학교 주변 이동과 산책이 반복됐습니다.",
                List.of("이동", "산책", "저녁"),
                category,
                motif
        );
    }
}
