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

        assertTrue(prompt.contains("# DAYBIT FINAL WEEKLY IMAGE GENERATION PROMPT"));
        assertTrue(prompt.contains("# PHOTO LANDSCAPE"));
        assertTrue(prompt.contains(
                "Use the visual motif as the content source for the image, "
                        + "and use the selected category rules as the style source."
        ));
        assertFalse(prompt.contains("이번 주에는 학교 주변 이동과 산책이 반복됐습니다."));
        assertFalse(prompt.contains("WEEKLY CONTEXT KEYWORDS"));
        assertTrue(prompt.contains("#D6A45C, #6A8FB3, #C9878A"));
        assertTrue(prompt.contains("Approved Visual Motif"));
        assertTrue(prompt.contains("captured with a real physical camera"));
        assertTrue(prompt.contains("one plausible real-world place and one moment"));
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

        assertEquals(
                "1024x1024",
                WeeklyImagePromptFactory.resolveImageSize(
                        WeeklyVisualCategory.OIL_ACRYLIC,
                        "1024x1024",
                        "1024x1536",
                        "1536x1024"
                )
        );

        assertEquals(
                "1536x1024",
                WeeklyImagePromptFactory.resolveImageSize(
                        WeeklyVisualCategory.ALBUM_COVER,
                        "1024x1024",
                        "1024x1536",
                        "1536x1024"
                )
        );
    }

    @Test
    void 그래픽포스터는_핵심하드제약과_유연한_에디토리얼가이드를_포함한다() {
        WeeklyVisualPlan visualPlan = visualPlan(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                "Build one dominant diagonal silhouette, three supporting planes, "
                        + "broad white negative space, halftone texture, and small color accents."
        );

        String prompt = WeeklyImagePromptFactory.buildPrompt(
                visualPlan,
                "#7D2B22, #9463B7, #7B5948, #8A20E8"
        );

        assertTrue(prompt.contains("# DAYBIT FINAL WEEKLY IMAGE GENERATION PROMPT"));
        assertTrue(prompt.contains(
                "Create a **portrait-oriented contemporary 2D graphic design poster**."
        ));
        assertTrue(prompt.contains("## HARD CONSTRAINTS"));
        assertTrue(prompt.contains(
                "Do not turn the composition into a chart, dashboard, checklist, timeline, infographic, or software interface."
        ));
        assertTrue(prompt.contains(
                "When `VISUAL_MOTIF` contains multiple concrete diary-derived visual cues, preserve more than one where practical"
        ));
        assertTrue(prompt.contains("halftone + grain"));
        assertTrue(prompt.contains("pure white `#FFFFFF` with broad negative space"));
        assertTrue(prompt.contains("never write `DAYBIT`"));
        assertTrue(prompt.contains("no Korean"));
        assertFalse(prompt.contains("If any requirement is missing, redesign"));
        assertTrue(prompt.length() < 30_000);
    }

    @Test
    void 비인간캐릭터는_동물얼굴을_허용하고_핵심제약과_유연한_스타일가이드를_분리한다() {
        WeeklyVisualPlan visualPlan = visualPlan(
                WeeklyVisualCategory.NON_HUMAN_CHARACTER,
                "Create one compact animal character wearing diary-grounded clothing and holding a diary-grounded prop."
        );

        String prompt = WeeklyImagePromptFactory.buildPrompt(
                visualPlan,
                "#7D2B22, #9463B7, #8A20E8"
        );

        assertTrue(prompt.contains("Exactly one animal must be the clear visual focus of the image."));
        assertTrue(prompt.contains("Create an animal character, not a human or humanoid character."));
        assertTrue(prompt.contains(
                "A visible **animal face is allowed and expected** when appropriate for the selected pose."
        ));
        assertTrue(prompt.contains("Prefer a front-facing or clear three-quarter orientation"));
        assertTrue(prompt.contains("clean single-color studio background"));
        assertTrue(prompt.contains(
                "Do **not** require exact pixel-level reproduction of a specific hexadecimal color."
        ));
        assertTrue(prompt.contains(
                "**designed contemporary character or premium character-fashion editorial**"
        ));
        assertFalse(prompt.contains("face must always be clearly visible"));
        assertFalse(prompt.contains("Use the most saturated color from this week's palette as the background color."));
        assertTrue(prompt.length() < 30_000);
    }

    @Test
    void 모든_신규_카테고리에_최신_스타일_원문을_포함한다() {
        assertCategoryPromptContains(
                WeeklyVisualCategory.NON_HUMAN_CHARACTER,
                "Exactly one animal must be the clear visual focus of the image.",
                "A visible **animal face is allowed and expected**"
        );
        assertCategoryPromptContains(
                WeeklyVisualCategory.OIL_ACRYLIC,
                "one contemporary oil painting",
                "clearly visible physical paint materiality"
        );
        assertCategoryPromptContains(
                WeeklyVisualCategory.ALBUM_COVER,
                "one landscape-oriented premium vinyl record mockup image",
                "one LP record visible by more than half behind the sleeve"
        );
        assertCategoryPromptContains(
                WeeklyVisualCategory.PIXEL_ART,
                "pixel-art game environment from a high three-quarter top-down viewpoint",
                "one carefully designed pixel-art world"
        );
        assertCategoryPromptContains(
                WeeklyVisualCategory.PHOTO_LANDSCAPE,
                "captured with a real physical camera",
                "documentary photography, street photography, or observational photography"
        );
        assertCategoryPromptContains(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                "one dominant visual structure, silhouette, cluster, or typographic mass",
                "localized print texture"
        );
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

    private void assertCategoryPromptContains(
            WeeklyVisualCategory category,
            String firstRule,
            String secondRule
    ) {
        String prompt = WeeklyImagePromptFactory.buildPrompt(
                visualPlan(category, "Build one diary-grounded visual direction."),
                "#667788, #AABBCC, #DDEEFF"
        );

        assertTrue(prompt.contains(firstRule));
        assertTrue(prompt.contains(secondRule));
    }
}
