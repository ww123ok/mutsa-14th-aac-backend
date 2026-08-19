package mutsa.hackathon.service;

import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class FallbackWeeklyRewardResultTextFactory {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일");

    public WeeklyRewardResultText create(
            WeeklyRewardGenerationContext context,
            WeeklyVisualPlan visualPlan
    ) {
        if (context == null || visualPlan == null) {
            throw new IllegalArgumentException(
                    "주간 보상 생성 정보는 필수입니다."
            );
        }

        LinkedHashSet<String> keywords =
                new LinkedHashSet<>();

        context.days()
                .stream()
                .flatMap(day -> day.keywords().stream())
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.replace("#", "")
                        .trim()
                        .replaceAll("\\s+", " "))
                .filter(value ->
                        !WeeklyRewardResultText.isReservedCategoryKeyword(value))
                .filter(value -> value.matches(".*[가-힣].*"))
                .forEach(value -> {
                    if (keywords.size() < 5) {
                        keywords.add(
                                value.length() <= 30
                                        ? value
                                        : value.substring(0, 30)
                        );
                    }
                });

        for (String fallbackKeyword : List.of(
                "주간 기록",
                "일상의 흐름",
                "색의 조합"
        )) {
            if (keywords.size() >= 3) {
                break;
            }
            keywords.add(fallbackKeyword);
        }

        String title = "%s부터 이어진 한 주".formatted(
                context.weekStartDate().format(DATE_FORMAT)
        );

        String reflectedContext = keywords.stream()
                .limit(2)
                .reduce((left, right) -> left + "과 " + right)
                .orElse("주간 기록");

        String summary = (
                "이번 주 기록에는 %s와 관련된 내용이 담겼습니다. "
                        + "이 내용과 각 날의 색을 바탕으로 %s 형식의 주간 이미지가 구성되었습니다."
        ).formatted(
                reflectedContext,
                categoryName(visualPlan.visualCategory())
        );

        return new WeeklyRewardResultText(
                title,
                summary,
                categoryKeyword(visualPlan.visualCategory()),
                List.copyOf(keywords)
        );
    }

    private String categoryKeyword(
            WeeklyVisualCategory category
    ) {
        return switch (category) {
            case GRAPHIC_POSTER -> "그래픽 포스터";
            case NON_HUMAN_CHARACTER -> "3D캐릭터";
            case OIL_ACRYLIC -> "유화";
            case ALBUM_COVER -> "LP커버";
            case PIXEL_ART -> "픽셀아트";
            case PHOTO_LANDSCAPE -> "실사 풍경";
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 주간 이미지 카테고리입니다."
            );
        };
    }

    private String categoryName(
            WeeklyVisualCategory category
    ) {
        return switch (category) {
            case NON_HUMAN_CHARACTER -> "비인간 동물 캐릭터";
            case GRAPHIC_POSTER -> "그래픽 디자인 포스터";
            case OIL_ACRYLIC -> "유화·아크릴 회화";
            case ALBUM_COVER -> "앨범 커버";
            case PIXEL_ART -> "픽셀아트 게임 장면";
            case PHOTO_LANDSCAPE -> "실사 풍경·공간";
            case FIRST_PERSON_ANIME -> "1인칭 장면묘사";
        };
    }
}
