package mutsa.hackathon.service;

import org.springframework.stereotype.Component;

@Component
public class FallbackWeeklyVisualPlanFactory {

    public WeeklyVisualPlan create(
            WeeklyRewardGenerationContext context
    ) {
        return create(context, null);
    }

    public WeeklyVisualPlan create(
            WeeklyRewardGenerationContext context,
            WeeklyVisualCategory excludedCategory
    ) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "주간 보상 생성 정보는 필수입니다."
            );
        }

        if (excludedCategory == WeeklyVisualCategory.GRAPHIC_POSTER) {
            return new WeeklyVisualPlan(
                    WeeklyVisualCategory.OIL_ACRYLIC,
                    "Create a square oil-and-acrylic scene built from broad layered brushwork and "
                            + "soft overlapping shapes that represent the week's accumulated rhythm "
                            + "without inventing literal events. Use the supplied weekly colors as "
                            + "the entire palette, with the most frequent tone covering the largest "
                            + "painted field, a second tone shaping the central ordinary form, and "
                            + "remaining colors appearing as restrained accents. Keep the composition "
                            + "calm and grounded, with visible canvas texture, imperfect edges, "
                            + "translucent overlaps, and gentle depth. Avoid faces, readable text, "
                            + "logos, brands, named places, dramatic symbolism, fantasy elements, or "
                            + "any unsupported object. Let the painting feel like several everyday "
                            + "moments settling into one cohesive atmosphere while preserving a clear "
                            + "central focus and enough negative space for visual breathing room."
            );
        }

        return new WeeklyVisualPlan(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                "Create a portrait flat graphic poster with one dominant asymmetric diagonal "
                        + "mass occupying the central visual field. Add three supporting cropped "
                        + "geometric planes, one narrow halftone trail, and a restrained "
                        + "misregistered print layer to express accumulated weekly records without "
                        + "showing literal diary scenes. Preserve broad pure-white negative space "
                        + "and a clear focal hierarchy. Use the strongest weekly color on the main "
                        + "mass, a second color across the supporting planes, and the remaining "
                        + "colors only as small accents. Keep the construction intentionally "
                        + "non-photographic, with no person, face, room, building, desk, paper, bus, "
                        + "landscape, readable text, logo, collage, calendar, or recognizable object. "
                        + "Finish with subtle grain and controlled screen-print texture."
        );
    }
}
