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
    void preserves_the_top_level_groups_and_removal_test() {
        String rules = WeeklyVisualCategorySelectionPolicy.EXACT_SELECTION_RULES;

        assertTrue(rules.contains("First, determine what is central to this week's records."));
        assertTrue(rules.contains("### 1. Pattern-centered"));
        assertTrue(rules.contains("### 2. Mood-centered"));
        assertTrue(rules.contains("### 3. Space-centered"));
        assertTrue(rules.contains(
                "First select one central group from `pattern / mood / space` for this week's records, "
                        + "and then select a detailed category within that group."
        ));
        assertTrue(rules.contains(
                "the axis whose removal would cause the greatest loss of the week's defining character"
        ));
        assertTrue(rules.contains(
                "Prioritize the actual structure of the user's records and this week's colors "
                        + "over the beauty or stylistic appeal of an image category."
        ));
    }

    @Test
    void preserves_the_detailed_category_selection_criteria() {
        String rules = WeeklyVisualCategorySelectionPolicy.EXACT_SELECTION_RULES;

        assertTrue(rules.contains(
                "the repetition itself can explain the week"
        ));
        assertTrue(rules.contains(
                "the sequence, comparison, increase or decrease, transition, or relationship with other elements "
                        + "must also be shown for the defining feature of the week to remain intact"
        ));
        assertTrue(rules.contains(
                "this week's colors are generally medium-saturation or low-saturation"
        ));
        assertTrue(rules.contains(
                "this week's colors are generally medium-saturation or high-saturation"
        ));
        assertTrue(rules.contains("the experience of using a space"));
        assertTrue(rules.contains("the experience of viewing a space"));
    }
}
