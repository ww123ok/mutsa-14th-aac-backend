package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiWeeklyImageQualityValidatorTest {

    @Test
    void 검수프롬프트는_하드제약과_소프트가이드를_분리하고_동물얼굴을_허용한다()
            throws Exception {
        Field field = OpenAiWeeklyImageQualityValidator.class
                .getDeclaredField("INSTRUCTIONS");
        field.setAccessible(true);
        String instructions = (String) field.get(null);

        assertTrue(instructions.contains(
                "explicit HARD CONSTRAINTS and clear prohibitions"
        ));
        assertTrue(instructions.contains(
                "Treat those as strong creative direction, not automatic failure conditions."
        ));
        assertTrue(instructions.contains(
                "a clearly visible ANIMAL face is allowed and often desirable"
        ));
        assertTrue(instructions.contains(
                "not an exact pixel-level hexadecimal match"
        ));
        assertTrue(instructions.contains(
                "one secondary motif cue is absent"
        ));
        assertFalse(instructions.contains(
                "a visible face, UI, explanatory text"
        ));
    }

    @Test
    void 검수입력은_생성프롬프트전체를_하드체크리스트로_취급하지_않는다() {
        String input = OpenAiWeeklyImageQualityValidator.buildReviewInput(
                WeeklyVisualCategory.NON_HUMAN_CHARACTER,
                "1024x1536",
                "## HARD CONSTRAINTS\n- No human.\n## CHARACTER DIRECTION\nPrefer a compact silhouette."
        );

        assertTrue(input.contains("SELECTED CATEGORY: NON_HUMAN_CHARACTER"));
        assertTrue(input.contains("EXPECTED ORIENTATION: PORTRAIT"));
        assertTrue(input.contains("GENERATION BRIEF:"));
        assertTrue(input.contains(
                "Extract only explicit HARD CONSTRAINTS and clear prohibitions"
        ));
        assertFalse(input.contains("HARD CHECKLIST:"));
    }
}
