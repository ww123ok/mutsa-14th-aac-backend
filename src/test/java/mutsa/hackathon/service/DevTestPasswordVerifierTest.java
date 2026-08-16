package mutsa.hackathon.service;

import mutsa.hackathon.global.exception.ProjectException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DevTestPasswordVerifierTest {

    private static final String PASSWORD =
            "0825";

    @Test
    void 올바른_비밀번호는_허용한다() {
        DevTestPasswordVerifier verifier =
                new DevTestPasswordVerifier(
                        sha256(PASSWORD)
                );

        assertDoesNotThrow(
                () -> verifier.verify(PASSWORD)
        );
    }

    @Test
    void 잘못된_비밀번호는_차단한다() {
        DevTestPasswordVerifier verifier =
                new DevTestPasswordVerifier(
                        sha256(PASSWORD)
                );

        assertThrows(
                ProjectException.class,
                () -> verifier.verify(
                        "wrong-password"
                )
        );
    }

    @Test
    void 비밀번호가_없으면_차단한다() {
        DevTestPasswordVerifier verifier =
                new DevTestPasswordVerifier(
                        sha256(PASSWORD)
                );

        assertThrows(
                ProjectException.class,
                () -> verifier.verify(null)
        );
    }

    @Test
    void 서버에_해시가_설정되지_않으면_차단한다() {
        DevTestPasswordVerifier verifier =
                new DevTestPasswordVerifier("");

        assertThrows(
                ProjectException.class,
                () -> verifier.verify(PASSWORD)
        );
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    exception
            );
        }
    }
}