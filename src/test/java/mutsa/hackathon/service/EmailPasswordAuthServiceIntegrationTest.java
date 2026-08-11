package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions
        .assertEquals;

import static org.junit.jupiter.api.Assertions
        .assertFalse;

import static org.junit.jupiter.api.Assertions
        .assertNotEquals;

import static org.junit.jupiter.api.Assertions
        .assertNotNull;

import static org.junit.jupiter.api.Assertions
        .assertThrows;

import static org.junit.jupiter.api.Assertions
        .assertTrue;

@SpringBootTest
class EmailPasswordAuthServiceIntegrationTest {

    @Autowired
    private EmailPasswordAuthService
            authService;

    @Autowired
    private AppUserRepository
            appUserRepository;

    @Autowired
    private PasswordEncoder
            passwordEncoder;

    @Test
    void 이메일_회원가입은_소문자로_정규화하고_비밀번호를_해시로만_저장한다() {
        String rawPassword =
                "daybit-password-123";

        Long userId =
                authService.register(
                        "  User@Test.COM ",
                        rawPassword
                );

        AppUser savedUser =
                appUserRepository
                        .findById(userId)
                        .orElseThrow();

        assertEquals(
                "local",
                savedUser.getProvider()
        );

        assertEquals(
                "user@test.com",
                savedUser.getProviderId()
        );

        assertEquals(
                "user@test.com",
                savedUser.getEmail()
        );

        /*
         * 실제 닉네임은 기존 onboarding에서
         * 사용자가 설정합니다.
         */
        assertEquals(
                "데이빗",
                savedUser.getNickname()
        );

        assertFalse(
                savedUser
                        .isOnboardingCompleted()
        );

        assertNotNull(
                savedUser.getPasswordHash()
        );

        /*
         * 평문 자체가 저장되어서는 안 됩니다.
         */
        assertNotEquals(
                rawPassword,
                savedUser.getPasswordHash()
        );

        /*
         * DelegatingPasswordEncoder의
         * 현재 bcrypt 저장 형태 확인.
         */
        assertTrue(
                savedUser
                        .getPasswordHash()
                        .startsWith(
                                "{bcrypt}"
                        )
        );

        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        savedUser
                                .getPasswordHash()
                )
        );
    }

    @Test
    void 같은_이메일은_대소문자를_바꿔도_중복가입할_수_없다() {
        authService.register(
                "duplicate@test.com",
                "password-1234"
        );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                authService
                                        .register(
                                                "DUPLICATE@Test.COM",
                                                "another-password-123"
                                        )
                );

        assertEquals(
                ErrorCode
                        .EMAIL_ALREADY_REGISTERED,
                exception.getErrorCode()
        );
    }

    @Test
    void 올바른_이메일과_비밀번호로_로그인할_수_있다() {
        Long registeredUserId =
                authService.register(
                        "login@test.com",
                        "password-1234"
                );

        Long authenticatedUserId =
                authService.authenticate(
                        "LOGIN@Test.COM",
                        "password-1234"
                );

        assertEquals(
                registeredUserId,
                authenticatedUserId
        );
    }

    @Test
    void 틀린_비밀번호와_존재하지_않는_이메일은_같은_인증오류를_반환한다() {
        authService.register(
                "known@test.com",
                "password-1234"
        );

        ProjectException wrongPassword =
                assertThrows(
                        ProjectException.class,
                        () ->
                                authService
                                        .authenticate(
                                                "known@test.com",
                                                "wrong-password"
                                        )
                );

        ProjectException unknownEmail =
                assertThrows(
                        ProjectException.class,
                        () ->
                                authService
                                        .authenticate(
                                                "unknown@test.com",
                                                "wrong-password"
                                        )
                );

        assertEquals(
                ErrorCode.INVALID_CREDENTIALS,
                wrongPassword.getErrorCode()
        );

        assertEquals(
                ErrorCode.INVALID_CREDENTIALS,
                unknownEmail.getErrorCode()
        );
    }

    @Test
    void 잘못된_길이의_비밀번호로는_회원가입할_수_없다() {
        ProjectException tooShort =
                assertThrows(
                        ProjectException.class,
                        () ->
                                authService.register(
                                        "short@test.com",
                                        "1234567"
                                )
                );

        ProjectException tooLong =
                assertThrows(
                        ProjectException.class,
                        () ->
                                authService.register(
                                        "long@test.com",
                                        "a".repeat(65)
                                )
                );

        assertEquals(
                ErrorCode.INVALID_REQUEST,
                tooShort.getErrorCode()
        );

        assertEquals(
                ErrorCode.INVALID_REQUEST,
                tooLong.getErrorCode()
        );
    }

    @Test
    void 같은_이메일의_카카오계정과_일반계정은_자동연결하지_않고_별도계정으로_유지한다() {
        AppUser kakaoUser =
                appUserRepository
                        .saveAndFlush(
                                AppUser.createKakaoUser(
                                        "kakao-provider-id",
                                        "카카오사용자",
                                        "same@test.com",
                                        null
                                )
                        );

        Long localUserId =
                authService.register(
                        "same@test.com",
                        "password-1234"
                );

        AppUser localUser =
                appUserRepository
                        .findById(
                                localUserId
                        )
                        .orElseThrow();

        assertNotEquals(
                kakaoUser.getId(),
                localUser.getId()
        );

        assertEquals(
                "kakao",
                kakaoUser.getProvider()
        );

        assertEquals(
                "local",
                localUser.getProvider()
        );
    }
}