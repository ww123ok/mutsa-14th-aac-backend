package mutsa.hackathon.service;

import java.util.Locale;

public final class WeeklyImagePromptFactory {

    private static final int MAX_PROMPT_LENGTH = 30_000;
    private static final int MAX_RETRY_PROMPT_LENGTH = 45_000;

    private static final String COMMON_PROMPT = """
            # DAYBIT FINAL WEEKLY IMAGE GENERATION PROMPT
            ## COMMON PROMPT

            Create **one polished weekly reward image** for DAYBIT.

            ### INPUT

            - Weekly Color Palette: `%s`
            - Approved Visual Motif: `%s`

            `VISUAL_MOTIF` is the approved visual direction already distilled from the week's diary records.

            **Use the visual motif as the content source for the image, and use the selected category rules as the style source.**

            Do not reinterpret the original diary records or invent a new story.

            ### HARD CONSTRAINTS

            - Create exactly one finished image.
            - Keep the image meaningfully connected to `VISUAL_MOTIF`.
            - Do not invent unsupported people, events, relationships, emotions, or narrative conclusions.
            - Do not expose recognizable **human** faces or private identifying information.
            - Do not include logos, brand marks, watermarks, signatures, or unnecessary explanatory text.
            - Do not imitate a specific artist, studio, franchise, character, poster, album cover, or game.
            - Avoid clearly malformed anatomy, meaningless pseudo-text, broken objects, or severe visual artifacts.
            - Follow any additional hard prohibition explicitly stated by the selected category.

            ### VISUAL GUIDANCE

            These directions should strongly guide the image, but minor deviations should not automatically make an otherwise strong image invalid.

            - Create one cohesive image rather than a collection of unrelated daily scenes.
            - Preserve the main visual hierarchy and recognizable ideas from `VISUAL_MOTIF`.
            - Use the weekly palette as **dominant / supporting / accent colors**. The colors do not need equal visual weight.
            - Prefer imagery grounded in diary-supported places, actions, objects, routines, structures, and visual relationships.
            - Avoid replacing specific diary-derived ideas with generic decorative imagery simply because it looks attractive.
            - Avoid excessive HDR, generic glossy AI aesthetics, synthetic-looking materials, or random visual clutter.
            - Prioritize connection to the week, strong art direction, and visual coherence over literal illustration of every detail.
            """;

    private static final String GRAPHIC_POSTER = """
            # GRAPHIC DESIGN POSTER

            Create a **portrait-oriented contemporary 2D graphic design poster**.

            The result should immediately read as an **intentionally art-directed poster**, not as a realistic scene, diary illustration, photo collage, mood board, infographic, dashboard, or software UI.

            ## HARD CONSTRAINTS

            - Use a portrait-oriented composition.
            - Keep the main visual idea meaningfully derived from `VISUAL_MOTIF`.
            - Do not turn the composition into a chart, dashboard, checklist, timeline, infographic, or software interface.
            - Do not use readable explanatory text, captions, UI labels, or fake informational copy.
            - If typography appears, use English characters only. Do not use Korean or the word `DAYBIT`.

            ## COMPOSITION

            Build the poster around **one dominant visual structure, silhouette, cluster, or typographic mass**.

            Support it with approximately 2–4 secondary elements.

            Favor:

            - strong scale contrast
            - cropping
            - overlap
            - interruption
            - asymmetry
            - compression
            - collision
            - repetition with variation
            - one visually dense region contrasted with broad negative space

            Avoid scattering many similarly sized elements evenly across the canvas.

            Avoid building a believable realistic environment or conventional photographic perspective.

            When `VISUAL_MOTIF` contains multiple concrete diary-derived visual cues, preserve more than one where practical so the poster remains specific to this week.

            Diary-derived objects and structures may be transformed through:

            - silhouette
            - flattening
            - cropping
            - repetition
            - fragmentation
            - distortion
            - masking
            - layering
            - posterization
            - duotone
            - color overlay
            - halftone
            - grain
            - dithering
            - photocopy texture
            - screen-print texture
            - ink bleed
            - print misregistration
            - rough edges
            - paper texture
            - pixelation
            - rough brush marks
            - stamps
            - torn or worn traces

            Geometric forms, grids, dots, color planes, lines, and graphic marks may be used as supporting design material, but they should not replace the diary-derived visual identity of the poster.

            Objects should function as **graphic forms rather than complete realistic illustrations**.

            ## TYPOGRAPHY

            Typography is optional.

            If used:

            - English only
            - no Korean
            - never write `DAYBIT`
            - no explanatory captions
            - no checklist text
            - no interface labels
            - no long readable sentences
            - prefer short words, fragments, letters, numbers, or partial strings

            Treat typography primarily as visual material.

            It may be cropped, overlapped, rotated, repeated, stretched, fragmented, partially obscured, or distorted.

            Avoid clean informational headline layouts that make the poster resemble an advertisement, slide, dashboard, or interface.

            ## COLOR AND MATERIALITY

            Use `[WEEKLY_COLOR_PALETTE]` as the primary color source.

            Assign dominant, supporting, and accent roles instead of giving every color equal weight.

            Prefer:

            - pure white `#FFFFFF` with broad negative space,
            - a white-based gradient,
            - or one vivid palette-grounded background.

            Avoid automatically shifting the palette toward ivory, cream, beige, warm gray, or other muted neutral tones.

            Use localized print texture rather than covering every surface with equal noise.

            Good combinations include:

            - halftone + grain
            - photocopy + misregistration
            - screen-print + ink bleed
            - paper grain + posterization

            The first impression should be:

            **strong hierarchy, a memorable silhouette, graphic tension, rhythm, negative space, and editorial density.**
            """;


    private static final String NON_HUMAN_CHARACTER = """
            # NON-HUMAN CHARACTER

            Create one polished **3D animal character**.

            Exactly one animal must be the clear visual focus of the image.

            ## HARD CONSTRAINTS

            - Create an animal character, not a human or humanoid character.
            - Do not create an object character.
            - Do not create a realistic wildlife portrait.
            - Do not introduce meaningful clothing or props that are unsupported by `VISUAL_MOTIF`.
            - Do not include recognizable human faces.
            - Keep the character clearly non-human in anatomy and overall silhouette.
            - Use a clean single-color studio background derived from the weekly palette.
            - Do not add environmental scenery, patterned backgrounds, or unrelated visual decoration.

            A visible **animal face is allowed and expected** when appropriate for the selected pose. Human-face restrictions do not apply to the animal character.

            ## CHARACTER DIRECTION

            Follow the animal, action, props, and design features specified in `VISUAL_MOTIF`.

            Use one clear main action or state.

            Favor 1–2 strong design characteristics rather than many small decorative details.

            Instead of depicting the animal realistically:

            - simplify its anatomy,
            - selectively exaggerate recognizable physical characteristics,
            - keep the silhouette compact and clearly stylized.

            Avoid normal adult human body proportions, narrow human-like waists, long athletic limbs, or fashion-model anatomy.

            The character should feel designed rather than anatomically realistic.

            Avoid making it excessively infantile, chibi-like, corporate, or like a generic promotional mascot.

            Aim for a **contemporary character-design sensibility with fashion and lifestyle appeal for people in their 20s**.

            Keep facial expression restrained.

            A neutral, calm, mildly indifferent, or naturally focused expression is acceptable.

            Do not invent a dramatic emotion that is not supported by the motif.

            When selecting the animal, consider the overall motif, including:

            - repeated actions
            - lifestyle patterns
            - posture or body rhythm
            - objects and props
            - atmosphere
            - silhouette suitability
            - compatibility with clothing or props
            - avoiding unnecessary repetition of animals used frequently in recent results

            ## FACE AND POSE

            Prefer a front-facing or clear three-quarter orientation when it supports the action.

            Keep the animal's identity and main facial features readable when practical.

            For dynamic actions, prioritize a natural pose and clear silhouette rather than forcing an unnatural camera-facing pose.

            Do not force a perfectly frontal pose when it makes the action or silhouette feel unnatural.

            ## MATERIALS

            Suitable tactile materials may include:

            - short soft fur
            - plush
            - knit
            - fabric
            - felt
            - clay
            - soft vinyl
            - rubber

            Use materials selectively according to the character design.

            Avoid glossy toy-plastic rendering and materials that resemble realistic human skin.

            ## CLOTHING AND PROPS

            Use only clothing and meaningful props that are grounded in `VISUAL_MOTIF`.

            Keep approximately 2–4 meaningful props at most.

            Do not invent accessories simply to make the character look fashionable.

            Contemporary styling is welcome when it is supported by the approved motif.

            If the motif does not specify clothing, a naturally designed unclothed animal character is preferable to invented fashion items.

            ## BACKGROUND AND COLOR

            Use a **clean single-color studio background** selected from `[WEEKLY_COLOR_PALETTE]`.

            Prefer one of the stronger, more saturated palette colors when it works visually with the character.

            Do **not** require exact pixel-level reproduction of a specific hexadecimal color.

            Distribute the weekly palette across the character, clothing, props, and background using dominant / supporting / accent roles.

            Do not add patterns, scenery, decorative symbols, or unnecessary background objects.

            Prefer a portrait-oriented composition.

            Frame the character large enough that its silhouette, material, primary action, and important diary-grounded details are immediately readable.

            The result should feel like a **designed contemporary character or premium character-fashion editorial**, not simply a cute mascot.

            ## LIGHTING

            Use soft, clean studio lighting.

            Make the character's form and important material qualities clearly visible.

            Fur, knit, fabric, clay, felt, or other tactile materials should remain distinguishable when used.

            Avoid excessive HDR, neon lighting, hard cinematic drama, or glossy toy-commercial lighting.
            """;


    private static final String OIL_ACRYLIC = """
            # OIL PAINTING
            Create **one contemporary oil painting** based on the scene specified in `VISUAL_MOTIF`.

            The image should remain recognizable as an ordinary real-life space, object arrangement, or situation, but instead of being rendered like a photograph, it must be **rebuilt through paint**.
            ## SCENE AND COMPOSITION
            - Show only one scene.
            - A close crop, top view, oblique view, or ordinary eye-level view is allowed.
            - Do not attempt to show the entire room or every event from the week.
            - Prefer painterly balance over perfect symmetry or photographically polished staging.
            - Establish one main visual area with supporting forms around it.
            ## PAINTERLY EXPRESSION
            - Keep major objects recognizable while allowing irregular contours and partially broken edges.
            - Allow some boundaries between forms to merge into surrounding paint.
            - Omit unnecessary small details.
            - Use visible directional brushwork.
            - Mix large and small brushstrokes.
            - Vary the thickness of the paint.
            - Allow localized impasto, thin paint passages, rough canvas, and uneven paint layers.
            - Distinguish materials through differences in color and brushwork rather than photographic surface rendering.
            Avoid smooth digital blending and excessively clean, uniform outlines.
            ## COLOR AND LIGHT
            - Follow the dominant / supporting / accent color relationships specified in `VISUAL_MOTIF`.
            - For painterly harmony, literal object colors may shift slightly toward the weekly palette.
            - Both soft and vivid colors are allowed.
            - Do not automatically make the result low-saturation, brown, beige, or melancholic.
            - Use one plausible real-world light source grounded in the motif.
            - Instead of creating dreamlike light bloom, show light by allowing it to change the colors of objects.
            People or animals may appear only as minor parts of the environment and must never become the central portrait subject.

            The final result should feel like a **contemporary painting of everyday life with clearly visible physical paint materiality**.
            """;

    private static final String ALBUM_COVER = """
            # ALBUM COVER
            Create **one landscape-oriented premium vinyl record mockup image**.

            The image must include:
            - one square album sleeve as the main object
            - one LP record visible by more than half behind the sleeve
            - a patterned floor and a connected patterned background
            - natural contact shadows and spatial shadows that make the objects appear to stand on the floor
            The album sleeve and LP must not look like flat images pasted onto the floor. They must appear to stand naturally as real physical objects.
            ## COVER ARTWORK
            Use only **one central visual motif specified in** **`VISUAL_MOTIF`**.

            That single motif must dominate the entire square cover.

            Do not create a montage of multiple diary events, multiple locations, or multiple images of similar importance.

            The cover artwork may use:
            - one strong object or object fragment
            - forms combining abstraction and representation
            - overlapping forms
            - cropped forms
            - bars
            - translucent strips
            - flowing bands
            - color planes
            - restrained paint marks
            Supporting layers must not cover the central motif. They should **strengthen rhythm and tension**.
            ## HUMAN-DERIVED FORMS
            Use a human-derived form only when it is supported by `VISUAL_MOTIF`.

            If used:
            - do not create a clean and recognizable portrait
            - use fragmentation, cropping, overlap, obstruction, and integration with patterns or graphic layers
            - the final form must remain clearly non-identifiable
            ## TYPOGRAPHY
            Prefer not to use text.

            If text is used:
            - keep it extremely small
            - keep it decorative
            - use English only
            - never allow it to become the main element
            Do not include track lists, SIDE A/B, DAYBIT, WEEKLY ARCHIVE, or explanatory copy.
            ## COLOR AND MATERIALITY
            - Use 1–2 colors from the weekly palette as dominant colors and use the remaining colors as supporting or accent colors.
            - Keep the cover artwork itself flat and editorial in its graphic sensibility.
            - The physical sleeve and LP should retain believable material presence.
            - Use subtle repeating patterns on both the background and floor.
            - Maintain only restrained studio depth rather than creating a large realistic space.
            The overall result should feel **editorial, graphic yet tactile, premium, contemporary, and like an independent record product**.
            """;

    private static final String PIXEL_ART = """
            # PIXEL ART
            Create a polished **pixel-art game environment from a high three-quarter top-down viewpoint**.

            The floor structure and spatial arrangement must clearly read like a small playable game map.

            Do not use:
            - street-level viewpoint
            - low cinematic camera
            - side-scrolling viewpoint
            - large horizon
            - landscape-painting-like composition
            ## ENVIRONMENT
            Build **one cohesive lived-in environment** based on `VISUAL_MOTIF`.

            Use the following elements included in the motif:
            - main zones
            - routes
            - objects
            - traces of repeated activities
            - focal area
            - time of day
            - lighting
            Diary-supported elements may be arranged into a readable tile-based world:
            - paths
            - floors
            - walls
            - buildings
            - furniture
            - plants
            - lighting
            - signs
            - machines
            - benches
            - everyday objects
            - natural elements
            Keep the environment rich and lived-in without making it so complex that the central structure disappears.

            Do not force unrelated indoor, outdoor, urban, and natural environments into one scene merely to show more diary content.
            ## CHARACTER AND HUD
            A small player character may be included when necessary.
            - The player must remain secondary to the environment.
            - Show only one simple action or state.
            - Do not enlarge the character into a portrait.
            Use a small retro-game HUD along the edge of the screen:
            - hearts
            - gauges
            - counters
            - item icons
            - small inventory-like elements
            Keep the HUD small, decorative, and orderly, and do not include long explanatory text.
            ## COLOR
            Rebuild the weekly palette into one cohesive pixel palette.
            - Dominant colors: overall environment
            - Supporting colors: structures, ground, objects
            - Accent colors: lighting, small props, HUD
            The final result should feel like **one carefully designed pixel-art world with visible traces of everyday life**, not a collage or a collection of diary elements.
            """;

    private static final String PHOTO_LANDSCAPE = """
            # PHOTO LANDSCAPE
            Create **one landscape-oriented photographic image that convincingly looks as if it were captured with a real physical camera**.

            Follow the representative environment, time of day, light, weather, spatial flow, and traces of everyday life described in `VISUAL_MOTIF`.
            ## SCENE
            Depict exactly **one plausible real-world place and one moment**.

            Do not combine multiple locations into one scene or place one object for each diary day.

            Let the influence of multiple records appear indirectly through:
            - spatial details
            - light
            - weather
            - traces of objects
            - movement
            - density
            - color
            - ordinary traces of everyday life
            The main subject of the image should be **space, light, perspective, atmosphere, and traces of everyday activity**.
            ## COMPOSITION
            - Create clear spatial depth and visual flow.
            - Use roads, sidewalks, stairs, railings, building lines, paths, or motif-grounded structures to create natural perspective.
            - Choose a wide, normal, or telephoto lens feeling according to the approved environment.
            - Do not automatically turn every result into a wide-angle sunset landscape.
            - Use natural layering, negative space, perspective, and atmospheric perspective.
            ## PHOTOGRAPHIC REALISM
            Use:
            - realistic exposure
            - believable scale relationships
            - natural perspective
            - plausible color temperature
            - restrained color grading
            - small lived-in imperfections
            - framing that feels casually discovered and photographed
            Aim for an impression closer to documentary photography, street photography, or observational photography than commercial advertising.

            When necessary, people may appear only as **small, distant background figures whose faces cannot be identified**.

            Avoid:
            - beautiful scenery unrelated to the records
            - generic sunsets
            - generic oceans
            - generic forests
            - generic night cities
            - impossible architecture
            - excessive neon
            - environments that look like 3D renders
            - polished real-estate-advertising imagery
            - scenes that look artificially perfect and obviously AI-generated
            First respect the actual environment described in `VISUAL_MOTIF`.

            Then naturally integrate the weekly palette into light, surfaces, reflections, sky, buildings, vegetation, shadows, and plausible real-world objects.

            The final result should feel like **a photograph that could genuinely have been encountered and captured during ordinary everyday life**.
            """;

    private static final String FIRST_PERSON_ANIME = """
            # FIRST-PERSON ANIME (LEGACY ONLY)
            This category is retained only for backward compatibility and must not be selected
            for a newly generated weekly visual plan. Create one original portrait-oriented,
            first-person everyday scene without a visible face, named-studio imitation,
            unsupported fantasy, or multiple-day collage.
            """;

    private WeeklyImagePromptFactory() {
    }

    public static String buildPrompt(
            WeeklyVisualPlan visualPlan,
            String palette
    ) {
        if (visualPlan == null) {
            throw new IllegalArgumentException(
                    "주간 이미지 분석 결과는 필수입니다."
            );
        }
        if (palette == null || palette.isBlank()) {
            throw new IllegalArgumentException(
                    "주간 이미지 색상 팔레트는 필수입니다."
            );
        }

        String prompt = """
                %s

                ---

                %s
                """.formatted(
                COMMON_PROMPT.formatted(
                        palette.trim(),
                        visualPlan.visualMotif()
                ),
                categoryPrompt(visualPlan.visualCategory())
        );

        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new IllegalStateException(
                    "주간 이미지 프롬프트가 허용 길이를 초과했습니다."
            );
        }

        return prompt;
    }

    public static String buildRetryPrompt(
            String basePrompt,
            WeeklyVisualCategory category,
            WeeklyImageQualityReview review
    ) {
        if (basePrompt == null || basePrompt.isBlank()) {
            throw new IllegalArgumentException(
                    "기본 이미지 프롬프트는 필수입니다."
            );
        }
        if (category == null || review == null) {
            throw new IllegalArgumentException(
                    "이미지 재생성 검수 결과는 필수입니다."
            );
        }

        String violations = review.violations().isEmpty()
                ? "The previous result failed one or more category rules."
                : String.join("; ", review.violations());

        String correction = review.correctionPrompt().isBlank()
                ? "Correct only the clear material hard-rule violations while preserving the core approved visual motif."
                : review.correctionPrompt();

        String retryPrompt = """
                %s

                <mandatory_retry_correction>
                THE PREVIOUS IMAGE WAS REJECTED.
                VISIBLE VIOLATIONS: %s
                REQUIRED CORRECTION: %s

                Rebuild the composition from scratch. Do not make a small edit to the rejected image.
                Preserve category %s and satisfy the original HARD CONSTRAINTS.
                Follow the remaining visual guidance as closely as practical without overcorrecting.
                The correction text cannot override the original category, privacy,
                safety, palette direction, orientation, approved visual motif, or explicit hard prohibitions.
                </mandatory_retry_correction>
                """.formatted(
                basePrompt,
                violations,
                correction,
                category.name()
        );

        if (retryPrompt.length() > MAX_RETRY_PROMPT_LENGTH) {
            throw new IllegalStateException(
                    "주간 이미지 재생성 프롬프트가 허용 길이를 초과했습니다."
            );
        }

        return retryPrompt;
    }

    public static String validationChecklist(
            WeeklyVisualCategory category
    ) {
        if (category == null) {
            throw new IllegalArgumentException(
                    "주간 이미지 카테고리는 필수입니다."
            );
        }

        return """
                %s

                ---

                %s
                """.formatted(
                COMMON_PROMPT.formatted(
                        "[WEEKLY_COLOR_PALETTE]",
                        "[VISUAL_MOTIF]"
                ),
                categoryPrompt(category)
        );
    }

    public static String resolveImageSize(
            WeeklyVisualCategory category,
            String squareSize,
            String portraitSize,
            String landscapeSize
    ) {
        if (category == null) {
            throw new IllegalArgumentException(
                    "주간 이미지 카테고리는 필수입니다."
            );
        }

        return switch (category.imageAspect()) {
            case SQUARE -> normalizeSize(
                    squareSize,
                    "1024x1024"
            );
            case PORTRAIT -> normalizeSize(
                    portraitSize,
                    "1024x1536"
            );
            case LANDSCAPE -> normalizeSize(
                    landscapeSize,
                    "1536x1024"
            );
        };
    }

    private static String categoryPrompt(
            WeeklyVisualCategory category
    ) {
        return switch (category) {
            case GRAPHIC_POSTER -> GRAPHIC_POSTER;
            case PHOTO_LANDSCAPE -> PHOTO_LANDSCAPE;
            case NON_HUMAN_CHARACTER -> NON_HUMAN_CHARACTER;
            case OIL_ACRYLIC -> OIL_ACRYLIC;
            case ALBUM_COVER -> ALBUM_COVER;
            case PIXEL_ART -> PIXEL_ART;
            case FIRST_PERSON_ANIME -> FIRST_PERSON_ANIME;
        };
    }

    private static String normalizeSize(
            String value,
            String defaultValue
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
