package mutsa.hackathon.service;

/**
 * 색 보상 생성기가 반환하는 검증된 결과
 */
public record DiaryColorReward(
        String colorHex,
        String colorName
) {

    private static final int
            MAX_COLOR_NAME_LENGTH = 100;

    public DiaryColorReward {
        if (
                colorHex == null
                        || !colorHex.matches(
                        "^#[0-9A-Fa-f]{6}$"
                )
        ) {
            throw new IllegalArgumentException(
                    "색상 코드는 #RRGGBB 형식이어야 합니다."
            );
        }

        if (
                colorName == null
                        || colorName.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "색상 이름은 필수입니다."
            );
        }

        String normalizedColorName =
                colorName.trim();

        if (
                normalizedColorName.length()
                        > MAX_COLOR_NAME_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "색상 이름은 100자 이하여야 합니다."
            );
        }

        colorHex = colorHex.toUpperCase();
        colorName = normalizedColorName;
    }
}