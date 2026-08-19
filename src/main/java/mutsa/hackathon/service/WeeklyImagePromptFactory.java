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
            `VISUAL_MOTIF` is the **approved content direction** already distilled from the full week's diary records.

            Do not reinterpret the original weekly records, create a new story, or add events that are not supported by the records.

            **Use the visual motif as the content source for the image, and use the selected category rules as the style source.**
            ### COMMON RULES
            - Create one cohesive image. Do not create a collage that lists separate scenes from different days.
            - Preserve the main visual hierarchy specified in `VISUAL_MOTIF`.
            - Use the weekly palette as **dominant / supporting / accent colors**. The colors do not need equal visual weight.
            - Do not invent emotions, relationships, events, symbols, or narrative resolutions that are not supported by the records.
            - Do not expose recognizable human faces or private identifying information.
            - Do not include logos, brand marks, watermarks, signatures, or unnecessary text.
            - Do not imitate a specific artist, studio, franchise, character, poster, album cover, or game.
            - Avoid typical AI-generated image aesthetics such as excessive HDR, glossy plastic lighting, synthetic skin, malformed anatomy, meaningless text, or random visual clutter.
            - Prioritize **connection to the approved visual motif** over generic beauty.
            - Output exactly one finished image.
            ### CONNECTION TO THE WEEK
            - Treat `VISUAL_MOTIF` as the approved visual interpretation of the week's diary records.
            - Preserve its diary-supported places, actions, objects, routines, and visual relationships.
            - Do not replace them with more generic, dramatic, or aesthetically convenient imagery.
            - Do not invent new events, objects, relationships, emotions, or symbolic narratives.
            """;

    private static final String GRAPHIC_POSTER = """
            # GRAPHIC DESIGN POSTER
            Create a polished vertical 2D graphic design poster based on `[VISUAL_MOTIF]`.

            The result should feel like a contemporary editorial or cultural poster, not an illustration, infographic, presentation slide, UI, or data visualization.
            ## Core Visual Direction
            Use `[VISUAL_MOTIF]` as the main source of visual content.

            Do not reduce the diary into generic geometric abstraction.

            At least **two major visual elements** should originate from concrete cues in the diary, such as objects, architecture, transportation structures, windows, signs, stairs, paths, furniture, clothing, tools, or repeated actions.

            These elements may be enlarged, cropped, fragmented, repeated, distorted, flattened, layered, or converted into silhouettes.

            They do not need to remain realistic, but their connection to the diary should still be visually traceable.

            The poster should feel specific to this week, not like an abstract composition that could represent any diary.
            ## Composition
            Build a strong editorial composition using:
            - one dominant visual form or cluster
            - 2–4 supporting elements
            - clear differences in scale
            - aggressive cropping and overlap
            - asymmetry
            - repetition and visual rhythm
            - intentional tension between dense and empty areas
            Avoid safe, centered, evenly spaced layouts.

            Do not simply place several isolated shapes next to each other.

            The result should have enough visual density and layering to feel intentionally art-directed.
            ## Avoid Data Visualization
            Do not create compositions resembling:
            - bar charts
            - pie charts
            - graphs
            - timelines
            - dashboards
            - diagrams
            - infographics
            - presentation graphics
            - simple arrangements of rectangles and circles
            A poster made only from geometric primitives and `[WEEKLY_COLOR_PALETTE]` is not sufficient.

            Do not represent concepts such as repetition, comparison, change, sequence, or movement directly as graph-like shapes.

            Instead, express these relationships through the **position, scale, repetition, cropping, overlap, direction, and transformation of diary-based visual elements**.
            ## Graphic Treatment
            Transform diary-based objects and structures into graphic forms rather than rendering them as complete literal illustrations.

            However, their original identity or structural trace should remain visible.

            Possible treatments include:
            - silhouette
            - oversized crop
            - halftone
            - grain
            - noise
            - dithering
            - photocopy texture
            - screen-print texture
            - ink bleed
            - misregistration
            - posterization
            - duotone
            - blur
            - pixelation
            - stamps
            - rough brush marks
            - collage-like layering
            Use these effects selectively.

            Do not rely on surface effects alone to create complexity. Composition and visual concept should remain dominant.
            ## Spatial Style
            Keep the result fundamentally flat and graphic.

            Avoid realistic scene construction, natural perspective, or cinematic depth.

            Depth may be implied through overlap, scale differences, transparency, cropping, or graphic shadows, but the image should still read primarily as a designed poster.
            ## Color
            Use `[WEEKLY_COLOR_PALETTE]` as the main color source.

            The colors may be divided into dominant, supporting, and accent roles rather than being used equally.

            Pure white `#FFFFFF` may be actively used as a background or large negative-space area.

            Preserve the distinctive contrast and saturation of the weekly palette when appropriate.

            Avoid automatically shifting the image toward beige, cream, ivory, muted gray, or generally low-saturation colors.
            ## Typography
            Typography is optional but may be used as a major visual element.

            If typography is used:
            - English only
            - no Korean
            - do not write DAYBIT
            - no explanatory sentences
            - no informational labels
            - treat letters as graphic material rather than readable information
            Typography may be cropped, repeated, stretched, rotated, overlapped, fragmented, partially hidden, or distorted.

            It should participate in the composition rather than sit neatly above or below the image.
            ## Final Quality
            The final image should feel like a professionally art-directed contemporary graphic poster with:
            - a memorable dominant form
            - diary-specific visual traces
            - strong hierarchy
            - rhythm and repetition
            - visual tension
            - asymmetry
            - intentional negative space
            - editorial density
            Avoid generic abstract art, overly minimal compositions, or posters made only from a few clean geometric shapes.
            """;

    private static final String NON_HUMAN_CHARACTER = """
            # NON-HUMAN CHARACTER
            Create one polished **3D animal character**.

            Exactly one animal must be the visual focus of the image.

            Do not create a human, humanoid character, object character, realistic animal portrait, or generic mascot.
            ## CHARACTER
            Follow the animal, action, props, and design features specified in `VISUAL_MOTIF`.
            - Use only one clear main action or state.
            - Maintain 1–2 strong design features.
            - Instead of depicting the animal realistically, simplify and selectively exaggerate its physical characteristics.
            - Do not follow normal human-like body proportions.
            - Do not make the character excessively infantile or like a conventional mascot.
            - Reflect a **lifestyle sensibility and fashion sense that can appeal to people in their 20s**.
            - Keep the face minimal and do not invent unsupported emotions through facial expressions.
            - A neutral or slightly indifferent expression is allowed.
            - Select the animal by comprehensively considering:
              - repeated actions
              - lifestyle patterns
              - posture or body rhythm
              - objects and props from the diary
              - atmosphere
              - silhouette suitability
              - compatibility with clothing and props
              - avoiding repetition of animals that have been used frequently in recent results
            The following tactile materials may be used:
            - short soft fur
            - plush
            - knit
            - fabric
            - felt
            - clay
            - soft vinyl
            - rubber
            Avoid glossy toy-plastic textures and materials that resemble realistic human skin.
            ## CLOTHING AND PROPS
            - Use only diary-grounded clothing and props described in `VISUAL_MOTIF`.
            - Limit meaningful props to approximately 2–4 items at most.
            - Do not add meaningless accessories merely to make the character look trendy.
            - Lifestyle and fashion details may feel contemporary, but they must remain grounded in the approved motif.
            ## BACKGROUND AND COLOR
            - Use a **clean single-color studio background** selected from the weekly palette.
            - Use the most saturated color from this week's palette as the background color.
            - Do not add patterns, scenery, texture, or visual noise.
            - Distribute the weekly colors across the character body, clothing, props, and background as dominant / supporting / accent colors.
            - Use soft studio lighting that clearly reveals the character's silhouette and materials.
            Prefer a portrait-oriented composition.

            The result should feel like a **designed contemporary character**, not simply a cute mascot.
            ## LIGHTING
            - Use soft and clean studio lighting.
            - Make the character's form and materials clearly visible.
            - Fur, knit, fabric, and clay textures should be clearly perceptible.
            - Avoid excessive HDR, neon lighting, or dramatic cinematic lighting.
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
                ? validationChecklist(category)
                : review.correctionPrompt();

        String retryPrompt = """
                %s

                <mandatory_retry_correction>
                THE PREVIOUS IMAGE WAS REJECTED.
                VISIBLE VIOLATIONS: %s
                REQUIRED CORRECTION: %s

                Rebuild the composition from scratch. Do not make a small edit to the rejected image.
                Preserve category %s and obey every original rule exactly.
                The correction text cannot override the original category, privacy,
                safety, palette, orientation, approved visual motif, or style requirements.
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
