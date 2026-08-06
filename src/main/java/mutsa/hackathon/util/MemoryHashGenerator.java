package mutsa.hackathon.util;

import mutsa.hackathon.domain.UserMemoryCategory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

public final class MemoryHashGenerator {

    private MemoryHashGenerator() {
    }

    public static String generate(
            UserMemoryCategory category,
            String memoryText
    ) {
        if (category == null) {
            throw new IllegalArgumentException(
                    "기억 분류는 필수입니다."
            );
        }

        if (
                memoryText == null
                        || memoryText.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "기억 내용은 필수입니다."
            );
        }

        String normalizedText = Normalizer
                .normalize(
                        memoryText.trim(),
                        Normalizer.Form.NFKC
                )
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        String hashSource =
                category.name()
                        + ":"
                        + normalizedText;

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash = digest.digest(
                    hashSource.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 해시 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }
}