package mutsa.hackathon.service;

import java.util.List;

final class WeeklyVisualCategorySelectionPolicy {

    static final List<WeeklyVisualCategory> SELECTABLE_CATEGORIES = List.of(
            WeeklyVisualCategory.NON_HUMAN_CHARACTER,
            WeeklyVisualCategory.GRAPHIC_POSTER,
            WeeklyVisualCategory.OIL_ACRYLIC,
            WeeklyVisualCategory.ALBUM_COVER,
            WeeklyVisualCategory.PIXEL_ART,
            WeeklyVisualCategory.PHOTO_LANDSCAPE
    );

    static final String EXACT_SELECTION_RULES = """
            Weekly Image Category Selection Criteria

            First, determine what is central to this week's records.

            ### 1. Pattern-centered

            This applies when behaviors, objects, or lifestyle patterns repeat across multiple dates,
            or when structures, relationships, or changes between dates form the defining feature of the week.

            #### Character image

            - Select this when the defining feature of the week remains intact even if only one behavior, object, or lifestyle pattern is extracted.
            - This is appropriate when the same element repeats and **the repetition itself can explain the week**.
            - This includes cases where one central behavior or subject continues even when the place or situation changes.

            Examples: continuously working on assignments, exercising frequently, drinking coffee every day, or repeatedly encountering a particular object.

            #### Graphic design poster image

            - Select this when the defining feature of the week disappears if only one behavior or object is extracted.
            - Even when a repeated element exists, this is appropriate when **the sequence, comparison, increase or decrease, transition, or relationship with other elements must also be shown for the defining feature of the week to remain intact**.
            - Select this when the structure among multiple elements is more important than any one element itself.

            Examples: schedules gradually accumulating, two states of daily life alternating, moving through several places in a consistent order, or the amount or manner of the same behavior changing from date to date.

            ---

            ### 2. Mood-centered

            This applies when the recurring mood and emotional impression across the whole week are more central than specific behaviors, objects, or spaces.

            #### Oil-painting-style image

            - Among mood-centered records, select this when **this week's colors are generally medium-saturation or low-saturation**.
            - It represents a week in which multiple emotions or scenes softly overlap.

            #### Album cover image

            - Among mood-centered records, select this when **this week's colors are generally medium-saturation or high-saturation**.
            - It compresses the mood of the week into one strong main visual.

            ---

            ### 3. Space-centered

            This applies when experiences in a particular place or space form the defining feature of the week.

            #### Pixel-art / game-scene image

            - Select this when **the experience of using a space** is central.
            - This is appropriate when not only the place itself, but also what the user did there, which objects the user used, and what kind of life or routine took place there are important.

            Examples: working and resting in a room, repeatedly studying at a cafe, or moving through school spaces while performing various activities.

            #### Photorealistic landscape / space image

            - Select this when **the experience of viewing a space** is central.
            - This is appropriate when the space itself—such as the appearance of the place, scenery, light, weather, buildings, streets, or natural environment—is more central to the records than the user's actions.

            Examples: a rainy street, a city at dawn, a landscape at sunset, the sea, or the appearance of a particular place.

            ---

            ## Decision Principles

            - First select one central group from `pattern / mood / space` for this week's records, and then select a detailed category within that group.
            - Do not select a category simply because a particular element appears.
            - When multiple conditions appear at the same time, base the decision on **the axis whose removal would cause the greatest loss of the week's defining character**.
            - Prioritize the actual structure of the user's records and this week's colors over the beauty or stylistic appeal of an image category.
            """;

    private WeeklyVisualCategorySelectionPolicy() {
    }
}
