package mutsa.hackathon.service;

import java.util.List;

final class WeeklyVisualCategorySelectionPolicy {

    static final List<WeeklyVisualCategory> SELECTABLE_CATEGORIES = List.of(
            WeeklyVisualCategory.NON_HUMAN_CHARACTER,
            WeeklyVisualCategory.GRAPHIC_POSTER,
            WeeklyVisualCategory.OIL_ACRYLIC,
            WeeklyVisualCategory.ALBUM_COVER
    );

    static final String EXACT_SELECTION_RULES = """
            ## Weekly Image Category Selection Criteria

            First, determine what the central characteristic of this week's records is.

            ### 1. Pattern-Centered

            Use this group when repeated actions, objects, lifestyle patterns, or structures, relationships, and changes across different days form the main characteristic of the week.

            #### Character Image

            - Select this category when the week's distinctive character remains even if only one action, object, or lifestyle pattern is extracted.
            - It is suitable when the same element repeats and **that repetition itself is enough to explain the week**.
            - This includes cases where the location or situation changes, but one central action or subject continues to recur.

            Examples: repeatedly working on assignments, exercising often, drinking coffee every day, or a specific object appearing repeatedly.

            #### Graphic Design Poster Image

            - Select this category when the week's distinctive character disappears if only one action or object is extracted.
            - Even when elements repeat, this category is suitable when **the week's identity can only be preserved by also showing sequence, comparison, increase or decrease, transition, or relationships with other elements**.
            - Select this when the structure among multiple elements matters more than any single element itself.

            Examples: schedules gradually piling up, two different lifestyle states alternating, moving through several places in a consistent sequence, or the amount or manner of the same action changing across different days.

            ---

            ### 2. Atmosphere-Centered

            Use this group when the overall atmosphere and emotional impression repeated throughout the week are more central than specific actions, objects, or spaces.

            #### Oil Painting Image

            - Among atmosphere-centered records, select this category when **the week's colors are generally medium- or low-saturation**.
            - Use it to express a week in which multiple emotions or scenes overlap softly.

            #### Album Cover Image

            - Among atmosphere-centered records, select this category when **the week's colors are generally medium- or high-saturation**.
            - Compress the overall atmosphere of the week into one strong central visual.

            ---

            ## Decision Principles

            - First select one central group among `Pattern / Atmosphere`, then choose the detailed category within that group.
            - Do not select a category simply because a certain element appears in the records.
            - When multiple conditions apply at the same time, prioritize **the axis whose removal would cause the greatest loss of the week's distinctive character**.
            - Prioritize the structure of the user's actual records and the week's colors over the attractiveness or stylistic preference of the image category.
            """;

    private WeeklyVisualCategorySelectionPolicy() {
    }
}
