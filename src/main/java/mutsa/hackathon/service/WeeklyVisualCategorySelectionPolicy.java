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
            # Revised Weekly Image Category Selection Criteria
            First determine which of the following best preserves the distinctive character of the week:
            - `Pattern-Centered`
            - `Atmosphere-Centered`
            - `Space-Centered`
            Do not choose a category simply because it can visually accommodate the records.

            When several groups are plausible, prioritize the one whose removal would cause the greatest loss of week-specific information.
            ---
            ## 1. Pattern-Centered
            Use this group when repeated actions, objects, routines, states, or changes in behavior form an important structure across the week.
            ### NON\\_HUMAN\\_CHARACTER
            Consider this category **first when a repeated lifestyle pattern is clearly present**.

            Select it when the week can be meaningfully compressed into:

            **one animal + one dominant action, routine, posture, object, or recurring state**

            The repeated element does not need to be exactly identical across records.

            Treat closely related behaviors as one broader recurring pattern when they share the same visual rhythm or physical character.

            For example:
            - repeatedly sitting and working, studying, drawing, or using a laptop
            - repeatedly carrying, preparing, packing, or organizing things
            - repeatedly eating, drinking, exercising, commuting, waiting, or resting
            - repeatedly interacting with the same type of object
            - similar body rhythms appearing across different places
            Different locations or situations do not prevent character selection if the same core behavioral pattern continues.

            Select `NON_HUMAN_CHARACTER` when **the repetition itself is enough to make the week recognizable**, even if surrounding details change.

            Do not require one exact action to appear identically several times.

            Do not reject this category merely because several secondary events also occurred.
            ### GRAPHIC\\_POSTER
            Select this category only when reducing the week to one recurring action, object, or routine would remove an essential part of its identity.

            The week's distinctive structure should depend on relationships such as:
            - sequence
            - comparison
            - alternation
            - accumulation
            - increase or decrease
            - interruption
            - transition
            - collision between recurring elements
            Multiple different activities alone are not enough.

            A generally busy, varied, or eventful week should **not automatically become a graphic poster**.

            Choose `GRAPHIC_POSTER` only when the relationship between multiple elements is more important than any one repeated element.

            Rule:

            **one recurring behavioral pattern can represent the week → NON_HUMAN_CHARACTER**

            **the relationship or change between multiple recurring elements is essential → GRAPHIC_POSTER**
            ---
            ## 2. Atmosphere-Centered
            Use this group only when concrete repeated actions and spatial structures are relatively weak, and the week's identity is carried more strongly by accumulated atmosphere, visual tone, light, or color.

            Do not select an atmosphere-centered category simply because the records contain emotions or have an overall mood.

            If a clear behavioral pattern or lived-in spatial structure exists, consider `NON_HUMAN_CHARACTER`, `PIXEL_ART`, or another more concrete category first.
            ### OIL\\_ACRYLIC
            Select this category when the week can be compressed into one ordinary, diary-supported scene or arrangement of everyday traces.

            Suitable evidence includes:
            - table or desk surfaces
            - food or dishes
            - room fragments
            - windows
            - bags, clothes, papers, cups, devices, or other accumulated objects
            - several ordinary traces that can plausibly coexist in one scene
            Prefer it when the palette is generally low- to medium-saturation, or when colors benefit from blending, soft transitions, and material texture.

            The week does not need to be emotionally heavy or quiet.
            ### ALBUM\\_COVER
            Use this category more selectively.

            Select it when:
            - no recurring action is strong enough for `NON_HUMAN_CHARACTER`,
            - no lived-in spatial structure is strong enough for `PIXEL_ART`,
            - no visually observed environment strongly supports `PHOTO_LANDSCAPE`,
            - and the week is still best represented by one central diary-supported motif combined with overall rhythm, atmosphere, and color relationships.
            The presence of varied events or a general weekly mood is not enough.

            Prefer `ALBUM_COVER` when the palette benefits from separation, contrast, layering, or graphic organization rather than blending.

            Treat this category as an atmosphere-driven option, **not as a default fallback for difficult-to-classify weeks**.
            ---
            ## 3. Space-Centered
            Use this group when places, spatial routines, routes, zones, or interaction with environments are essential to the week's identity.

            The same exact location does not need to repeat.

            Several related everyday spaces can be compressed into one representative environment when their usage forms a coherent weekly pattern.
            ### PIXEL\\_ART
            Consider this category **first when the user repeatedly occupies, navigates, or uses spaces**.

            Select it when the week contains enough spatial and behavioral information to build a coherent lived-in environment or game map.

            The records do not need to describe the same room or place repeatedly.

            Treat related spaces as one spatial system when they form a recurring everyday route or activity structure.

            Examples include:
            - home, school, cafe, studio, gym, station, convenience store, library, or workplace appearing as activity zones
            - moving between several recurring everyday spaces
            - repeatedly sitting, working, eating, waiting, exercising, preparing, or resting in different zones
            - objects that indicate how spaces were used, such as desks, chairs, bags, laptops, cups, lockers, exercise equipment, food, or transit elements
            - repeated routes or transitions between indoor spaces
            Choose `PIXEL_ART` when **what the user did within spaces and how those spaces connected matters more than the appearance of one particular place**.

            A single repeated location is not required.

            A week with several ordinary activity spaces can still become one coherent game-like environment.

            Do not reject `PIXEL_ART` merely because the records involve multiple locations.
            ### PHOTO\\_LANDSCAPE
            Select this category when spatial experience is primarily visual rather than activity-based.

            Suitable evidence includes repeated attention to:
            - streets
            - exterior routes
            - weather
            - daylight or night lighting
            - reflections
            - architecture
            - sky
            - scenery
            - natural surroundings
            - visually memorable views encountered while moving
            The exact same location does not need to recur, but several records should support a coherent type of observed environment.

            Choose `PHOTO_LANDSCAPE` when **how the environment looked matters more than what the user did there**.

            Rule:

            **space mainly occupied, navigated, and used → PIXEL_ART**

            **space mainly seen, passed through, or visually remembered → PHOTO_LANDSCAPE**
            ---
            # Decision Order
            Use the following order when analyzing the week.
            ### Step 1 — Find the strongest recurring structure
            Before considering atmosphere, check whether the week contains:
            1. a recurring behavioral pattern suitable for `NON_HUMAN_CHARACTER`
            2. a recurring lived-in spatial structure suitable for `PIXEL_ART`
            3. a recurring observed environment suitable for `PHOTO_LANDSCAPE`
            Do not require exact repetition. Related actions or spaces may form one broader pattern when supported by at least two records.
            ### Step 2 — Check whether relationships are essential
            If a recurring pattern exists but cannot represent the week without also showing meaningful comparison, sequence, transition, accumulation, or alternation, consider `GRAPHIC_POSTER`.

            Do not select `GRAPHIC_POSTER` simply because several different events occurred.
            ### Step 3 — Use atmosphere-centered categories when concrete structures are weaker
            If no action, spatial usage pattern, observed environment, or multi-element relationship clearly preserves the week, evaluate:
            - `OIL_ACRYLIC`
            - `ALBUM_COVER`
            Atmosphere should therefore be a genuine central characteristic, not a fallback caused by uncertainty.
            ---
            # Diversity Tie-Breaker
            First identify every category that is genuinely suitable.

            If one category is clearly the strongest fit, select it regardless of recent history.

            If two or more categories are similarly suitable, prefer the category that has appeared less often in recent category history.

            A slightly less suitable category may be selected for diversity only when the difference in fit is small.

            Do not force an unsuitable category solely to balance frequency.

            Avoid repeatedly selecting `GRAPHIC_POSTER` or `ALBUM_COVER` as general-purpose fallback categories.

            When suitability is comparable, give additional preference to an underrepresented category such as `NON_HUMAN_CHARACTER`, `PIXEL_ART`, `PHOTO_LANDSCAPE`, or `OIL_ACRYLIC`.
            """;

    static final String COMPACT_SELECTION_RULES = """
            Weekly Image Category Selection — Compact Version

            First determine the week's strongest central structure.

            Consider the whole week, and treat something as repeated only when supported by at least two records.

            Choose the axis whose removal would cause the greatest loss of the week's distinctive character:

            - `Pattern-Centered`
            - `Space-Centered`
            - `Atmosphere-Centered`

            Do not choose a category simply because it can accommodate varied diary content or produce an attractive image.
            ## 1. Pattern-Centered
            ### NON\\_HUMAN\\_CHARACTER
            Prefer when one recurring behavioral pattern can still represent the week after other details are removed.

            The repetition does not need to be exactly identical. Closely related actions may be grouped when they share the same visual rhythm, posture, object use, or lifestyle pattern.

            Examples include repeated working/studying, eating/drinking, exercising, commuting, waiting, preparing, organizing, or resting across different situations.

            Rule:

            **one recurring action/routine/state explains the week →** **`NON_HUMAN_CHARACTER`**
            ### GRAPHIC\\_POSTER
            Choose only when one repeated element is insufficient and the week's identity depends on relationships among multiple elements, such as:

            - sequence
            - comparison
            - alternation
            - accumulation
            - increase/decrease
            - transition
            - interruption

            Do not select it merely because the week contains many different activities.

            Rule:

            **relationships or changes within repetition are essential →** **`GRAPHIC_POSTER`**
            ## 2. Space-Centered
            ### PIXEL\\_ART
            Prefer when the user repeatedly **occupies, navigates, or uses spaces**.

            The same place does not need to repeat. Related everyday spaces may form one coherent lived-in environment when their routes, zones, objects, and activities create a recurring structure.

            Examples include home, school, cafe, gym, studio, station, store, or similar activity spaces connected by everyday movement.

            Rule:

            **what happened within spaces and how they were used/connected matters most →** **`PIXEL_ART`**
            ### PHOTO\\_LANDSCAPE
            Choose when spaces are mainly **seen, passed through, or visually remembered** rather than actively used.

            Prioritize recurring visual evidence such as streets, scenery, weather, daylight, reflections, architecture, sky, exterior routes, or natural surroundings.

            Rule:

            **how the environment looked matters more than what the user did there →** **`PHOTO_LANDSCAPE`**
            ## 3. Atmosphere-Centered
            Use this group only when concrete behavioral and spatial structures are weaker.

            Do not use atmosphere categories as fallback choices for difficult-to-classify weeks.
            ### OIL\\_ACRYLIC
            Choose when the week can be compressed into one ordinary supported scene or arrangement of everyday traces, especially when the palette is generally low- to medium-saturation and benefits from blending or soft transitions.
            ### ALBUM\\_COVER
            Choose selectively when no strong recurring action, spatial structure, or observed environment dominates, and the week is best represented by one central diary-supported motif plus overall atmosphere, rhythm, and color relationships.

            Prefer when colors benefit from separation, contrast, or layering.

            Do not select `ALBUM_COVER` merely because the week has a general mood or varied events.
            # Decision Order
            Evaluate in this order:

            1. recurring behavioral pattern → consider `NON_HUMAN_CHARACTER`
            2. recurring lived-in spatial structure → consider `PIXEL_ART`
            3. recurring visually observed environment → consider `PHOTO_LANDSCAPE`
            4. meaningful relationships or changes among multiple repeated elements → consider `GRAPHIC_POSTER`
            5. if these concrete structures are weak → consider `OIL_ACRYLIC` or `ALBUM_COVER`

            If one category clearly fits best, select it.

            If several categories are similarly suitable, prefer the category used less often in recent history.

            Do not force an unsuitable category for diversity, but avoid repeatedly using `GRAPHIC_POSTER` or `ALBUM_COVER` as general-purpose fallbacks.
            """;

    static List<WeeklyVisualCategory> selectableCategoriesExcluding(
            WeeklyVisualCategory excludedCategory
    ) {
        if (
                excludedCategory == null
                        || !SELECTABLE_CATEGORIES.contains(excludedCategory)
        ) {
            return SELECTABLE_CATEGORIES;
        }

        return SELECTABLE_CATEGORIES.stream()
                .filter(category -> category != excludedCategory)
                .toList();
    }

    private WeeklyVisualCategorySelectionPolicy() {
    }
}
