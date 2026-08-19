package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyVisualCategorySelectionPolicyTest {

    @Test
    void newly_generated_weekly_images_select_only_the_six_specified_categories() {
        assertEquals(
                List.of(
                        WeeklyVisualCategory.NON_HUMAN_CHARACTER,
                        WeeklyVisualCategory.GRAPHIC_POSTER,
                        WeeklyVisualCategory.OIL_ACRYLIC,
                        WeeklyVisualCategory.ALBUM_COVER,
                        WeeklyVisualCategory.PIXEL_ART,
                        WeeklyVisualCategory.PHOTO_LANDSCAPE
                ),
                WeeklyVisualCategorySelectionPolicy.SELECTABLE_CATEGORIES
        );

        assertFalse(
                WeeklyVisualCategorySelectionPolicy.SELECTABLE_CATEGORIES
                        .contains(WeeklyVisualCategory.FIRST_PERSON_ANIME)
        );
    }

    @Test
    void preserves_the_revised_category_selection_criteria() {
        String rules = WeeklyVisualCategorySelectionPolicy.EXACT_SELECTION_RULES;

        assertTrue(rules.contains(
                "# Revised Weekly Image Category Selection Criteria"
        ));
        assertTrue(rules.contains(
                "Consider this category **first when a repeated lifestyle pattern is clearly present**."
        ));
        assertTrue(rules.contains(
                "A generally busy, varied, or eventful week should **not automatically become a graphic poster**."
        ));
        assertTrue(rules.contains(
                "Consider this category **first when the user repeatedly occupies, navigates, or uses spaces**."
        ));
        assertTrue(rules.contains(
                "Choose `PHOTO_LANDSCAPE` when **how the environment looked matters more than what the user did there**."
        ));
        assertTrue(rules.contains("# Diversity Tie-Breaker"));
        assertTrue(rules.contains(
                "Do not force an unsuitable category solely to balance frequency."
        ));
    }

    @Test
    void preserves_the_compact_category_selection_prompt() {
        String rules = WeeklyVisualCategorySelectionPolicy.COMPACT_SELECTION_RULES;

        assertTrue(rules.contains(
                "Weekly Image Category Selection — Compact Version"
        ));
        assertTrue(rules.contains(
                "one recurring action/routine/state explains the week"
        ));
        assertTrue(rules.contains(
                "what happened within spaces and how they were used/connected matters most"
        ));
        assertTrue(rules.contains(
                "If several categories are similarly suitable, prefer the category used less often in recent history."
        ));
    }

    @Test
    void 직전주_카테고리는_이번주_후보에서_제외한다() {
        List<WeeklyVisualCategory> candidates =
                WeeklyVisualCategorySelectionPolicy
                        .selectableCategoriesExcluding(
                                WeeklyVisualCategory.PIXEL_ART
                        );

        assertEquals(5, candidates.size());
        assertFalse(candidates.contains(WeeklyVisualCategory.PIXEL_ART));
        assertTrue(candidates.contains(WeeklyVisualCategory.PHOTO_LANDSCAPE));
        assertTrue(candidates.contains(WeeklyVisualCategory.GRAPHIC_POSTER));
        assertEquals(
                WeeklyVisualCategorySelectionPolicy.SELECTABLE_CATEGORIES,
                WeeklyVisualCategorySelectionPolicy
                        .selectableCategoriesExcluding(null)
        );
    }
}
