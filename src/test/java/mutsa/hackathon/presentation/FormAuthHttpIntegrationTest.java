package mutsa.hackathon.presentation;

import jakarta.servlet.http.Cookie;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.util.JwtCookieUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "app.jwt.cookie-secure=false",
                "app.jwt.cookie-same-site=Lax"
        }
)
@AutoConfigureMockMvc
class FormAuthHttpIntegrationTest {

    private static final String
            VALID_PASSWORD =
            "password-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository
            appUserRepository;

    @Autowired
    private PasswordEncoder
            passwordEncoder;

    @Test
    void 회원가입은_인증없이_가능하고_local계정과_JWT쿠키를_생성한다()
            throws Exception {

        String email =
                uniqueEmail(
                        "signup"
                );

        String upperCaseEmail =
                email.toUpperCase(
                        Locale.ROOT
                );

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/auth/signup"
                                )
                                        .contentType(
                                                MediaType
                                                        .APPLICATION_JSON
                                        )
                                        .content(
                                                authBody(
                                                        upperCaseEmail,
                                                        VALID_PASSWORD
                                                )
                                        )
                        )
                        .andExpect(
                                status()
                                        .isCreated()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.isSuccess"
                                )
                                        .value(true)
                        )
                        .andExpect(
                                jsonPath(
                                        "$.code"
                                )
                                        .value(
                                                "COMMON201"
                                        )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.result.email"
                                )
                                        .value(
                                                email
                                        )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.result.nickname"
                                )
                                        .value(
                                                "데이빗"
                                        )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.result.onboardingCompleted"
                                )
                                        .value(false)
                        )
                        .andReturn();

        assertTokenCookiesIssued(
                result
        );

        AppUser savedUser =
                appUserRepository
                        .findByProviderAndProviderId(
                                "local",
                                email
                        )
                        .orElseThrow();

        assertEquals(
                "local",
                savedUser.getProvider()
        );

        assertEquals(
                email,
                savedUser.getProviderId()
        );

        assertEquals(
                email,
                savedUser.getEmail()
        );

        assertFalse(
                savedUser
                        .isOnboardingCompleted()
        );

        assertNotNull(
                savedUser.getPasswordHash()
        );

        assertNotEquals(
                VALID_PASSWORD,
                savedUser.getPasswordHash()
        );

        assertTrue(
                passwordEncoder.matches(
                        VALID_PASSWORD,
                        savedUser
                                .getPasswordHash()
                )
        );

        String refreshToken =
                extractCookieValue(
                        result,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        /*
         * JWT 발급과 동시에 서버에도
         * 현재 refresh token이 저장됩니다.
         */
        assertEquals(
                refreshToken,
                savedUser.getRefreshToken()
        );
    }

    @Test
    void 회원가입에서_받은_access쿠키로_api_me에_접근할_수_있다()
            throws Exception {

        String email =
                uniqueEmail(
                        "signup-me"
                );

        MvcResult signupResult =
                signup(
                        email,
                        VALID_PASSWORD
                );

        String accessToken =
                extractCookieValue(
                        signupResult,
                        JwtCookieUtils
                                .ACCESS_TOKEN_COOKIE_NAME
                );

        mockMvc.perform(
                        get(
                                "/api/me"
                        )
                                .cookie(
                                        new Cookie(
                                                JwtCookieUtils
                                                        .ACCESS_TOKEN_COOKIE_NAME,
                                                accessToken
                                        )
                                )
                )
                .andExpect(
                        status()
                                .isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.isSuccess"
                        )
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "COMMON200"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.result.email"
                        )
                                .value(
                                        email
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.result.onboardingCompleted"
                        )
                                .value(false)
                );
    }

    @Test
    void 일반로그인은_정상비밀번호를_검증하고_JWT쿠키를_재발급한다()
            throws Exception {

        String email =
                uniqueEmail(
                        "login"
                );

        signup(
                email,
                VALID_PASSWORD
        );

        MvcResult loginResult =
                mockMvc.perform(
                                post(
                                        "/api/auth/login"
                                )
                                        .contentType(
                                                MediaType
                                                        .APPLICATION_JSON
                                        )
                                        .content(
                                                authBody(
                                                        email,
                                                        VALID_PASSWORD
                                                )
                                        )
                        )
                        .andExpect(
                                status()
                                        .isOk()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.isSuccess"
                                )
                                        .value(true)
                        )
                        .andExpect(
                                jsonPath(
                                        "$.code"
                                )
                                        .value(
                                                "COMMON200"
                                        )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.result.email"
                                )
                                        .value(
                                                email
                                        )
                        )
                        .andReturn();

        assertTokenCookiesIssued(
                loginResult
        );

        String refreshToken =
                extractCookieValue(
                        loginResult,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        AppUser savedUser =
                findLocalUser(
                        email
                );

        assertEquals(
                refreshToken,
                savedUser.getRefreshToken()
        );

        assertNotNull(
                savedUser.getLastLoginAt()
        );
    }

    @Test
    void 틀린비밀번호와_존재하지않는이메일은_동일한_401인증오류를_반환한다()
            throws Exception {

        String knownEmail =
                uniqueEmail(
                        "known"
                );

        signup(
                knownEmail,
                VALID_PASSWORD
        );

        mockMvc.perform(
                        post(
                                "/api/auth/login"
                        )
                                .contentType(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                                .content(
                                        authBody(
                                                knownEmail,
                                                "wrong-password"
                                        )
                                )
                )
                .andExpect(
                        status()
                                .isUnauthorized()
                )
                .andExpect(
                        jsonPath(
                                "$.isSuccess"
                        )
                                .value(false)
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "AUTH401_3"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        )
                                .value(
                                        "이메일 또는 비밀번호가 올바르지 않습니다."
                                )
                );

        String unknownEmail =
                uniqueEmail(
                        "unknown"
                );

        mockMvc.perform(
                        post(
                                "/api/auth/login"
                        )
                                .contentType(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                                .content(
                                        authBody(
                                                unknownEmail,
                                                "wrong-password"
                                        )
                                )
                )
                .andExpect(
                        status()
                                .isUnauthorized()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "AUTH401_3"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        )
                                .value(
                                        "이메일 또는 비밀번호가 올바르지 않습니다."
                                )
                );
    }

    @Test
    void 같은이메일은_두번_회원가입할_수_없다()
            throws Exception {

        String email =
                uniqueEmail(
                        "duplicate"
                );

        signup(
                email,
                VALID_PASSWORD
        );

        mockMvc.perform(
                        post(
                                "/api/auth/signup"
                        )
                                .contentType(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                                .content(
                                        authBody(
                                                email
                                                        .toUpperCase(
                                                                Locale.ROOT
                                                        ),
                                                VALID_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status()
                                .isConflict()
                )
                .andExpect(
                        jsonPath(
                                "$.isSuccess"
                        )
                                .value(false)
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "AUTH409_1"
                                )
                );
    }

    @Test
    void refresh쿠키로_새JWT쿠키를_발급하고_서버의_refresh_token도_갱신한다()
            throws Exception {

        String email =
                uniqueEmail(
                        "refresh"
                );

        MvcResult signupResult =
                signup(
                        email,
                        VALID_PASSWORD
                );

        String refreshToken =
                extractCookieValue(
                        signupResult,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        MvcResult refreshResult =
                mockMvc.perform(
                                post(
                                        "/api/auth/refresh"
                                )
                                        .cookie(
                                                new Cookie(
                                                        JwtCookieUtils
                                                                .REFRESH_TOKEN_COOKIE_NAME,
                                                        refreshToken
                                                )
                                        )
                        )
                        .andExpect(
                                status()
                                        .isOk()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.isSuccess"
                                )
                                        .value(true)
                        )
                        .andExpect(
                                jsonPath(
                                        "$.result.accessToken"
                                )
                                        .isString()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.result.refreshToken"
                                )
                                        .isString()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.result.tokenType"
                                )
                                        .value(
                                                "Bearer"
                                        )
                        )
                        .andReturn();

        assertTokenCookiesIssued(
                refreshResult
        );

        String newAccessToken =
                extractCookieValue(
                        refreshResult,
                        JwtCookieUtils
                                .ACCESS_TOKEN_COOKIE_NAME
                );

        String newRefreshToken =
                extractCookieValue(
                        refreshResult,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        AppUser savedUser =
                findLocalUser(
                        email
                );

        assertEquals(
                newRefreshToken,
                savedUser.getRefreshToken()
        );

        /*
         * Refresh로 새로 받은 access token도
         * 실제 Security Filter에서 인증되는지 확인합니다.
         */
        mockMvc.perform(
                        get(
                                "/api/me"
                        )
                                .cookie(
                                        new Cookie(
                                                JwtCookieUtils
                                                        .ACCESS_TOKEN_COOKIE_NAME,
                                                newAccessToken
                                        )
                                )
                )
                .andExpect(
                        status()
                                .isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.result.email"
                        )
                                .value(
                                        email
                                )
                );
    }

    @Test
    void 로그아웃은_DB_refresh_token을_폐기하고_두인증쿠키를_만료한다()
            throws Exception {

        String email =
                uniqueEmail(
                        "logout"
                );

        MvcResult signupResult =
                signup(
                        email,
                        VALID_PASSWORD
                );

        String accessToken =
                extractCookieValue(
                        signupResult,
                        JwtCookieUtils
                                .ACCESS_TOKEN_COOKIE_NAME
                );

        String refreshToken =
                extractCookieValue(
                        signupResult,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        AppUser beforeLogout =
                findLocalUser(
                        email
                );

        assertNotNull(
                beforeLogout.getRefreshToken()
        );

        MvcResult logoutResult =
                mockMvc.perform(
                                post(
                                        "/api/logout"
                                )
                                        .cookie(
                                                new Cookie(
                                                        JwtCookieUtils
                                                                .ACCESS_TOKEN_COOKIE_NAME,
                                                        accessToken
                                                ),
                                                new Cookie(
                                                        JwtCookieUtils
                                                                .REFRESH_TOKEN_COOKIE_NAME,
                                                        refreshToken
                                                )
                                        )
                        )
                        .andExpect(
                                status()
                                        .isOk()
                        )
                        .andReturn();

        String expiredAccessCookie =
                findCookieHeader(
                        logoutResult,
                        JwtCookieUtils
                                .ACCESS_TOKEN_COOKIE_NAME
                );

        String expiredRefreshCookie =
                findCookieHeader(
                        logoutResult,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        assertTrue(
                expiredAccessCookie
                        .contains(
                                "Max-Age=0"
                        )
        );

        assertTrue(
                expiredRefreshCookie
                        .contains(
                                "Max-Age=0"
                        )
        );

        AppUser afterLogout =
                findLocalUser(
                        email
                );

        assertNull(
                afterLogout.getRefreshToken()
        );

        assertNull(
                afterLogout
                        .getRefreshTokenExpiresAt()
        );
    }

    private MvcResult signup(
            String email,
            String password
    ) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/auth/signup"
                                )
                                        .contentType(
                                                MediaType
                                                        .APPLICATION_JSON
                                        )
                                        .content(
                                                authBody(
                                                        email,
                                                        password
                                                )
                                        )
                        )
                        .andExpect(
                                status()
                                        .isCreated()
                        )
                        .andReturn();

        assertTokenCookiesIssued(
                result
        );

        return result;
    }

    private AppUser findLocalUser(
            String email
    ) {
        return appUserRepository
                .findByProviderAndProviderId(
                        "local",
                        email.toLowerCase(
                                Locale.ROOT
                        )
                )
                .orElseThrow();
    }

    private void assertTokenCookiesIssued(
            MvcResult result
    ) {
        String accessCookieHeader =
                findCookieHeader(
                        result,
                        JwtCookieUtils
                                .ACCESS_TOKEN_COOKIE_NAME
                );

        String refreshCookieHeader =
                findCookieHeader(
                        result,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        assertTrue(
                accessCookieHeader
                        .contains(
                                "HttpOnly"
                        )
        );

        assertTrue(
                refreshCookieHeader
                        .contains(
                                "HttpOnly"
                        )
        );

        assertTrue(
                accessCookieHeader
                        .contains(
                                "Path=/"
                        )
        );

        assertTrue(
                refreshCookieHeader
                        .contains(
                                "Path=/"
                        )
        );

        assertTrue(
                accessCookieHeader
                        .contains(
                                "SameSite=Lax"
                        )
        );

        assertTrue(
                refreshCookieHeader
                        .contains(
                                "SameSite=Lax"
                        )
        );

        assertFalse(
                extractCookieValue(
                        result,
                        JwtCookieUtils
                                .ACCESS_TOKEN_COOKIE_NAME
                )
                        .isBlank()
        );

        assertFalse(
                extractCookieValue(
                        result,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                )
                        .isBlank()
        );
    }

    private String extractCookieValue(
            MvcResult result,
            String cookieName
    ) {
        String header =
                findCookieHeader(
                        result,
                        cookieName
                );

        String prefix =
                cookieName + "=";

        int endIndex =
                header.indexOf(
                        ';',
                        prefix.length()
                );

        if (endIndex < 0) {
            endIndex =
                    header.length();
        }

        return header.substring(
                prefix.length(),
                endIndex
        );
    }

    private String findCookieHeader(
            MvcResult result,
            String cookieName
    ) {
        String prefix =
                cookieName + "=";

        List<String> headers =
                result.getResponse()
                        .getHeaders(
                                HttpHeaders.SET_COOKIE
                        );

        return headers.stream()
                .filter(header ->
                        header.startsWith(
                                prefix
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Set-Cookie 응답에 "
                                        + cookieName
                                        + " 쿠키가 없습니다. 전체 헤더: "
                                        + headers
                        )
                );
    }

    private String authBody(
            String email,
            String password
    ) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(
                email,
                password
        );
    }

    private String uniqueEmail(
            String prefix
    ) {
        return prefix
                + "-"
                + System.nanoTime()
                + "@example.com";
    }
}