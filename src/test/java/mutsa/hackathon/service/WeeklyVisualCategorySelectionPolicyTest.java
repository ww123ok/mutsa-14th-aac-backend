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

        assertTrue(rules.contains(
                "First, determine what the central characteristic of this week's records is."
        ));
        assertTrue(rules.contains("### 1. Pattern-Centered"));
        assertTrue(rules.contains("### 2. Atmosphere-Centered"));
        assertTrue(rules.contains("### 3. Space-Centered"));
        assertTrue(rules.contains(
                "First select one central group among `Pattern / Atmosphere / Space`, "
                        + "then choose the detailed category within that group."
        ));
        assertTrue(rules.contains(
                "the axis whose removal would cause the greatest loss of the week's distinctive character"
        ));
        assertTrue(rules.contains(
                "Prioritize the structure of the user's actual records and the week's colors "
                        + "over the attractiveness or stylistic preference of the image category."
        ));
    }

    @Test
    void preserves_the_detailed_category_selection_criteria() {
        String rules = WeeklyVisualCategorySelectionPolicy.EXACT_SELECTION_RULES;

        assertTrue(rules.contains(
                "that repetition itself is enough to explain the week"
        ));
        assertTrue(rules.contains(
                "the week's identity can only be preserved by also showing sequence, comparison, "
                        + "increase or decrease, transition, or relationships with other elements"
        ));
        assertTrue(rules.contains(
                "the week's colors are generally medium- or low-saturation"
        ));
        assertTrue(rules.contains(
                "the week's colors are generally medium- or high-saturation"
        ));
        assertTrue(rules.contains("the experience of using a space"));
        assertTrue(rules.contains("the experience of seeing a space"));
    }
}
