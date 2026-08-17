package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyImagePromptFactoryTest {

    @Test
    void 실사풍경은_일기요약을_제외하고_브리프와_팔레트로_가로프롬프트를_만든다() {
        WeeklyVisualPlan visualPlan = visualPlan(
                WeeklyVisualCategory.PHOTO_LANDSCAPE,
                "Create one university-area street that connects repeated classes, errands, and walks."
        );

        String prompt = WeeklyImagePromptFactory.buildPrompt(
                visualPlan,
                "#D6A45C, #6A8FB3, #C9878A"
        );

        assertTrue(prompt.contains("SELECTED CATEGORY: PHOTO_LANDSCAPE"));
        assertFalse(prompt.contains("이번 주에는 학교 주변 이동과 산책이 반복됐습니다."));
        assertFalse(prompt.contains("WEEKLY CONTEXT KEYWORDS"));
        assertTrue(prompt.contains("#D6A45C, #6A8FB3, #C9878A"));
        assertTrue(prompt.contains("APPROVED ART-DIRECTION BRIEF"));
        assertTrue(prompt.contains("real photograph captured"));
        assertTrue(prompt.contains("whole week, not one day"));
        assertFalse(prompt.contains("Studio Ghibli imitation"));

        assertEquals(
                "1536x1024",
                WeeklyImagePromptFactory.resolveImageSize(
                        visualPlan.visualCategory(),
                        "1024x1024",
                        "1024x1536",
                        "1536x1024"
                )
        );
    }

    @Test
    void 포스터와_캐릭터는_세로_픽셀아트는_정방형_크기를_선택한다() {
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
                "1024x1536",
                WeeklyImagePromptFactory.resolveImageSize(
                        WeeklyVisualCategory.NON_HUMAN_CHARACTER,
                        "1024x1024",
                        "1024x1536",
                        "1536x1024"
                )
        );

        assertEquals(
                "1024x1024",
                WeeklyImagePromptFactory.resolveImageSize(
                        WeeklyVisualCategory.PIXEL_ART,
                        "1024x1024",
                        "1024x1536",
                        "1536x1024"
                )
        );
    }

    @Test
    void 그래픽포스터는_콜라주와_현실장면을_금지하는_하드규칙을_포함한다() {
        WeeklyVisualPlan visualPlan = visualPlan(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                "Build one dominant diagonal silhouette, three supporting planes, "
                        + "broad white negative space, halftone texture, and small color accents."
        );

        String prompt = WeeklyImagePromptFactory.buildPrompt(
                visualPlan,
                "#7D2B22, #9463B7, #7B5948, #8A20E8"
        );

        assertTrue(prompt.contains("DAYBIT WEEKLY IMAGE PROMPT VERSION: V3"));
        assertTrue(prompt.contains("portrait-oriented contemporary 2D graphic design poster"));
        assertTrue(prompt.contains("Never show several recognizable places"));
        assertTrue(prompt.contains("One unmistakable dominant silhouette"));
        assertTrue(prompt.contains("pure #FFFFFF"));
        assertTrue(prompt.contains("no Korean, DAYBIT, clean headline"));
        assertTrue(prompt.length() < 30_000);
    }

    @Test
    void 검수실패_재생성프롬프트는_위반사항과_카테고리를_고정한다() {
        WeeklyVisualPlan visualPlan = visualPlan(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                "Build one dominant flat silhouette with three supporting forms."
        );
        String basePrompt = WeeklyImagePromptFactory.buildPrompt(
                visualPlan,
                "#7D2B22, #9463B7"
        );

        String retryPrompt = WeeklyImagePromptFactory.buildRetryPrompt(
                basePrompt,
                WeeklyVisualCategory.GRAPHIC_POSTER,
                new WeeklyImageQualityReview(
                        true,
                        false,
                        List.of("The image is a recognizable photo collage."),
                        "Remove all photographic scenes and rebuild them as flat graphic planes."
                )
        );

        assertTrue(retryPrompt.contains("THE PREVIOUS IMAGE WAS REJECTED"));
        assertTrue(retryPrompt.contains("recognizable photo collage"));
        assertTrue(retryPrompt.contains("Preserve category GRAPHIC_POSTER"));
        assertTrue(retryPrompt.length() < 32_000);
    }

    private WeeklyVisualPlan visualPlan(
            WeeklyVisualCategory category,
            String motif
    ) {
        String expandedMotif = motif + " " + """
                Maintain one integrated composition with a clear focal hierarchy and deliberate
                negative space. Translate only supported weekly context into form, light, texture,
                object traces, and color distribution. Use the first palette color as the primary
                field, the second as support, and remaining colors only as restrained accents.
                Avoid faces, private identifiers, brands, logos, copyrighted characters, daily
                panels, unsupported symbols, dramatic events, emotional invention, and explanatory
                text. Keep every element grounded in the approved category and finish the image as
                one contemporary, cohesive, shareable mobile archive reward.
                """;

        return new WeeklyVisualPlan(
                category,
                expandedMotif
        );
    }
}
