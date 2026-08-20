package mutsa.hackathon.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

@Component
public class FallbackWeeklyRewardResultTextFactory {

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

        String title = fallbackTitle(
                visualPlan.visualCategory()
        );
        String summary = fallbackSummary(
                visualPlan.visualCategory()
        );

        return new WeeklyRewardResultText(
                title,
                summary,
                categoryKeyword(visualPlan.visualCategory()),
                List.copyOf(keywords)
        );
    }

    private String fallbackTitle(
            WeeklyVisualCategory category
    ) {
        return switch (category) {
            case GRAPHIC_POSTER ->
                    "겹쳐진 장면과 선명한 색면";
            case NON_HUMAN_CHARACTER ->
                    "작은 캐릭터가 머문 생활 장면";
            case OIL_ACRYLIC ->
                    "부드러운 붓결이 남은 풍경";
            case ALBUM_COVER ->
                    "빛과 장면이 겹친 한 컷";
            case PIXEL_ART ->
                    "작은 픽셀로 이어진 생활 장면";
            case PHOTO_LANDSCAPE ->
                    "빛이 머문 생활의 풍경";
            case FIRST_PERSON_ANIME ->
                    "눈앞에 펼쳐진 생활 장면";
        };
    }

    private String fallbackSummary(
            WeeklyVisualCategory category
    ) {
        return switch (category) {
            case GRAPHIC_POSTER -> """
                    이미지에는 기록에서 가져온 주요 사물과 장면이 선명한 색면과 겹쳐진 형태로 배치되었습니다. 서로 다른 날의 색은 큰 면과 작은 포인트로 나뉘어 한눈에 흐름이 보이도록 이어집니다.
                    """.trim();
            case NON_HUMAN_CHARACTER -> """
                    이미지에는 기록 속 행동을 떠올릴 수 있는 작은 비인간 캐릭터와 주변 사물이 중심 장면으로 배치되었습니다. 부드러운 입체 질감과 또렷한 색 대비가 캐릭터의 행동과 생활 공간을 자연스럽게 드러냅니다.
                    """.trim();
            case OIL_ACRYLIC -> """
                    이미지에는 기록에서 가져온 생활 공간과 사물이 부드러운 붓결과 겹친 색으로 표현되었습니다. 은은하게 번지는 빛과 표면의 질감이 가까운 사물과 주변 공간의 깊이를 천천히 드러냅니다.
                    """.trim();
            case ALBUM_COVER -> """
                    이미지에는 기록에서 가져온 중심 장면이 한 장의 커버처럼 압축되어 배치되었습니다. 강한 빛과 어두운 면의 대비, 선명한 포인트 색이 시선을 한곳으로 모으도록 표현되었습니다.
                    """.trim();
            case PIXEL_ART -> """
                    이미지에는 기록 속 생활 공간과 작은 사물이 또렷한 픽셀 단위로 정리되어 있습니다. 여러 공간의 흔적은 하나의 장면처럼 이어지고, 각 날의 색이 작은 조명과 오브젝트에 나뉘어 들어갑니다.
                    """.trim();
            case PHOTO_LANDSCAPE -> """
                    이미지에는 기록에서 가져온 공간의 빛과 날씨, 주변 사물이 실제 풍경처럼 구체적으로 담겨 있습니다. 자연스러운 원근과 빛의 방향, 각 날의 색이 섞인 톤이 장면의 분위기를 또렷하게 보여줍니다.
                    """.trim();
            case FIRST_PERSON_ANIME -> """
                    이미지에는 기록에서 가져온 공간과 사물이 눈앞에서 바라보는 구도로 펼쳐집니다. 가까운 물체와 멀리 이어지는 배경, 각 날의 색이 자연스러운 시선 흐름을 만듭니다.
                    """.trim();
        };
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
