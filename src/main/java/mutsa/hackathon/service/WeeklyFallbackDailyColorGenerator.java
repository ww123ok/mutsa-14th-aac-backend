package mutsa.hackathon.service;

import mutsa.hackathon.domain.DiaryRewardPolicy;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 일간 색 생성이 장시간 PENDING 또는 FAILED인 경우에도
 * 3회 작성 조건을 충족한 사용자의 주간 보상이 사라지지 않도록
 * 일기 내용의 SHA-256에서 안정적인 대체 색을 계산합니다.
 */
@Component
public class WeeklyFallbackDailyColorGenerator {

    public String generate(String diaryContent) {
        if (diaryContent == null || diaryContent.isBlank()) {
            return "#B58A72";
        }
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("SHA-256")
                    .digest(diaryContent.trim().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }

        float hue = (Byte.toUnsignedInt(hash[0]) / 255f);
        float saturation = 0.38f + (Byte.toUnsignedInt(hash[1]) / 255f) * 0.22f;
        float brightness = 0.62f + (Byte.toUnsignedInt(hash[2]) / 255f) * 0.18f;
        Color color = Color.getHSBColor(hue, saturation, brightness);
        String colorHex = "#" + HexFormat.of().withUpperCase().formatHex(new byte[]{
                (byte) color.getRed(),
                (byte) color.getGreen(),
                (byte) color.getBlue()
        });
        if (DiaryRewardPolicy.isReservedColor(colorHex)) {
            return "#B58A72";
        }
        return colorHex;
    }
}