package mutsa.hackathon.service;

import org.springframework.stereotype.Component;

@Component
public class FallbackWeeklyVisualPlanFactory {

    public WeeklyVisualPlan create(
            WeeklyRewardGenerationContext context
    ) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "주간 보상 생성 정보는 필수입니다."
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
