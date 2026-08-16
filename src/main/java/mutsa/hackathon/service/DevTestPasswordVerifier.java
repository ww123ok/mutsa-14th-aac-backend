package mutsa.hackathon.service;

import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class DevTestPasswordVerifier {

    public static final String HEADER_NAME =
            "X-Daybit-Dev-Password";

    private static final int SHA_256_HEX_LENGTH = 64;
    private static final int MAX_PASSWORD_LENGTH = 256;

    private final String configuredPasswordHash;

    public DevTestPasswordVerifier(
            @Value("${app.dev.test-password-sha256:}")
            String configuredPasswordHash
    ) {
        this.configuredPasswordHash =
                configuredPasswordHash == null
                        ? ""
                        : configuredPasswordHash.trim();
    }

    public void verify(String rawPassword) {
        if (
                rawPassword == null
                        || rawPassword.isBlank()
                        || rawPassword.length()
                        > MAX_PASSWORD_LENGTH
        ) {
            throw accessDenied();
        }

        byte[] expectedHash =
                parseConfiguredHash();

        byte[] actualHash =
                generateSha256(rawPassword);

        if (
                !MessageDigest.isEqual(
                        expectedHash,
                        actualHash
                )
        ) {
            throw accessDenied();
        }
    }

    private byte[] parseConfiguredHash() {
        if (
                configuredPasswordHash.length()
                        != SHA_256_HEX_LENGTH
        ) {
            throw accessDenied();
        }

        try {
            return HexFormat.of()
                    .parseHex(configuredPasswordHash);
        } catch (IllegalArgumentException exception) {
            throw accessDenied();
        }
    }

    private byte[] generateSha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            return digest.digest(
                    value.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }

    private ProjectException accessDenied() {
        return new ProjectException(
                ErrorCode.ACCESS_DENIED
        );
    }
}