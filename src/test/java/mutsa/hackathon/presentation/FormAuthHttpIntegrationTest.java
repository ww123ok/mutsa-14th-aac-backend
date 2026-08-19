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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    private static final String
            CSRF_COOKIE_NAME =
            "XSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository
            appUserRepository;

    @Autowired
    private PasswordEncoder
            passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void csrf_엔드포인트는_실제_CSRF쿠키와_헤더용_토큰을_발급한다()
            throws Exception {

        CsrfContext csrf =
                fetchCsrf();

        assertEquals(
                "X-XSRF-TOKEN",
                csrf.headerName()
        );

        assertFalse(
                csrf.cookieValue()
                        .isBlank()
        );

        assertFalse(
                csrf.token()
                        .isBlank()
        );
    }

    @Test
    void CSRF없이_회원가입하면_403이고_사용자는_생성되지_않는다()
            throws Exception {

        String email =
                uniqueEmail(
                        "signup-no-csrf"
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
                                                email,
                                                VALID_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status()
                                .isForbidden()
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
                                        "AUTH403_2"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        )
                                .value(
                                        "CSRF 토큰이 없거나 유효하지 않습니다."
                                )
                );

        assertTrue(
                appUserRepository
                        .findByProviderAndProviderId(
                                "local",
                                email
                        )
                        .isEmpty()
        );
    }

    @Test
    void Cookie와_Header의_CSRF가_일치하지_않으면_403이다()
            throws Exception {

        String email =
                uniqueEmail(
                        "signup-wrong-csrf"
                );

        CsrfContext csrf =
                fetchCsrf();

        mockMvc.perform(
                        post(
                                "/api/auth/signup"
                        )
                                .cookie(
                                        csrf.cookie()
                                )
                                .header(
                                        csrf.headerName(),
                                        csrf.token()
                                                + "-tampered"
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
                                .isForbidden()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "AUTH403_2"
                                )
                );

        assertTrue(
                appUserRepository
                        .findByProviderAndProviderId(
                                "local",
                                email
                        )
                        .isEmpty()
        );
    }

    @Test
    void 실제_CSRF를_사용한_회원가입은_local계정과_JWT쿠키를_생성한다()
            throws Exception {

        String email =
                uniqueEmail(
                        "signup"
                );

        String upperCaseEmail =
                email.toUpperCase(
                        Locale.ROOT
                );

        CsrfContext csrf =
                fetchCsrf();

        MvcResult result =
                mockMvc.perform(
                                withCsrf(
                                        post(
                                                "/api/auth/signup"
                                        ),
                                        csrf
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

        /*
         * 회원가입으로 인증 상태가 바뀌었으므로
         * 가입 전에 사용한 CSRF token은 폐기되어야 합니다.
         */
        assertCsrfCookieExpired(
                result
        );

        AppUser savedUser =
                findLocalUser(
                        email
                );

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

        assertEquals(
                refreshToken,
                savedUser.getRefreshToken()
        );
    }

    @Test
    void GET은_CSRF없이_가능하지만_인증된_PATCH는_CSRF가_필요하다()
            throws Exception {

        String email =
                uniqueEmail(
                        "me-csrf"
                );

        MvcResult signupResult =
                signupWithCsrf(
                        email,
                        VALID_PASSWORD
                );

        String accessToken =
                extractCookieValue(
                        signupResult,
                        JwtCookieUtils
                                .ACCESS_TOKEN_COOKIE_NAME
                );

        /*
         * GET은 safe method이므로
         * access token만 있으면 CSRF 없이 가능합니다.
         */
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

        /*
         * 동일하게 인증된 사용자라도
         * PATCH는 CSRF 없이는 차단되어야 합니다.
         */
        mockMvc.perform(
                        patch(
                                "/api/me"
                        )
                                .cookie(
                                        new Cookie(
                                                JwtCookieUtils
                                                        .ACCESS_TOKEN_COOKIE_NAME,
                                                accessToken
                                        )
                                )
                                .contentType(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                                .content(
                                        onboardingBody()
                                )
                )
                .andExpect(
                        status()
                                .isForbidden()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "AUTH403_2"
                                )
                );

        AppUser beforeValidPatch =
                findLocalUser(
                        email
                );

        assertFalse(
                beforeValidPatch
                        .isOnboardingCompleted()
        );

        /*
         * 회원가입 성공 시 이전 token을 폐기했으므로
         * 인증 후 사용할 새 CSRF token을 다시 발급받습니다.
         */
        CsrfContext authenticatedCsrf =
                fetchCsrf(
                        new Cookie(
                                JwtCookieUtils
                                        .ACCESS_TOKEN_COOKIE_NAME,
                                accessToken
                        )
                );

        mockMvc.perform(
                        withCsrf(
                                patch(
                                        "/api/me"
                                ),
                                authenticatedCsrf
                        )
                                .cookie(
                                        new Cookie(
                                                JwtCookieUtils
                                                        .ACCESS_TOKEN_COOKIE_NAME,
                                                accessToken
                                        )
                                )
                                .contentType(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                                .content(
                                        onboardingBody()
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
                                "$.result.nickname"
                        )
                                .value(
                                        "데이빛"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.result.job"
                        )
                                .value(
                                        "대학생"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.result.reminderTime"
                        )
                                .value(
                                        "21:30"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.result.aiMemoryConsent"
                        )
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.result.onboardingCompleted"
                        )
                                .value(true)
                );

        AppUser afterValidPatch =
                findLocalUser(
                        email
                );

        assertTrue(
                afterValidPatch
                        .isOnboardingCompleted()
        );

        assertEquals(
                "데이빛",
                afterValidPatch.getNickname()
        );

    }

    @Test
    void 로그인도_CSRF가_필요하고_정상_CSRF에서는_기존_인증오류_계약을_유지한다()
            throws Exception {

        String knownEmail =
                uniqueEmail(
                        "login"
                );

        signupWithCsrf(
                knownEmail,
                VALID_PASSWORD
        );

        /*
         * 로그인 endpoint가 permitAll이어도
         * CSRF 검증까지 면제되는 것은 아닙니다.
         */
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
                                                VALID_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status()
                                .isForbidden()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "AUTH403_2"
                                )
                );

        CsrfContext csrf =
                fetchCsrf();

        /*
         * CSRF가 정상인 경우에만
         * 실제 이메일/비밀번호 검증까지 도달합니다.
         */
        mockMvc.perform(
                        withCsrf(
                                post(
                                        "/api/auth/login"
                                ),
                                csrf
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
                        withCsrf(
                                post(
                                        "/api/auth/login"
                                ),
                                csrf
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
                );

        /*
         * 인증 실패는 인증 상태를 바꾸지 않으므로
         * 동일 CSRF token으로 정상 로그인을 다시 시도할 수 있습니다.
         */
        MvcResult loginResult =
                mockMvc.perform(
                                withCsrf(
                                        post(
                                                "/api/auth/login"
                                        ),
                                        csrf
                                )
                                        .contentType(
                                                MediaType
                                                        .APPLICATION_JSON
                                        )
                                        .content(
                                                authBody(
                                                        knownEmail,
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
                                        "$.result.email"
                                )
                                        .value(
                                                knownEmail
                                        )
                        )
                        .andReturn();

        assertTokenCookiesIssued(
                loginResult
        );

        /*
         * 로그인 성공 역시 인증 상태 변화이므로
         * 로그인 전에 사용한 CSRF token을 폐기합니다.
         */
        assertCsrfCookieExpired(
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
                        knownEmail
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
    void 같은이메일의_중복회원가입은_정상_CSRF에서도_409를_반환한다()
            throws Exception {

        String email =
                uniqueEmail(
                        "duplicate"
                );

        signupWithCsrf(
                email,
                VALID_PASSWORD
        );

        CsrfContext csrf =
                fetchCsrf();

        mockMvc.perform(
                        withCsrf(
                                post(
                                        "/api/auth/signup"
                                ),
                                csrf
                        )
                                .contentType(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                                .content(
                                        authBody(
                                                email.toUpperCase(
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
    void refresh는_CSRF없이_차단되고_정상_CSRF에서는_JWT를_갱신한다()
            throws Exception {

        String email =
                uniqueEmail(
                        "refresh"
                );

        MvcResult signupResult =
                signupWithCsrf(
                        email,
                        VALID_PASSWORD
                );

        String refreshToken =
                extractCookieValue(
                        signupResult,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        AppUser beforeBlockedRefresh =
                findLocalUser(
                        email
                );

        assertEquals(
                refreshToken,
                beforeBlockedRefresh
                        .getRefreshToken()
        );

        /*
         * Refresh 역시 POST이므로
         * refresh_token Cookie만으로는 요청할 수 없습니다.
         */
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
                                .isForbidden()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "AUTH403_2"
                                )
                );

        /*
         * CSRF 차단 요청은 Controller까지 도달하지 않았으므로
         * 서버의 refresh token도 바뀌면 안 됩니다.
         */
        AppUser afterBlockedRefresh =
                findLocalUser(
                        email
                );

        assertEquals(
                refreshToken,
                afterBlockedRefresh
                        .getRefreshToken()
        );

        CsrfContext csrf =
                fetchCsrf();

        MvcResult refreshResult =
                mockMvc.perform(
                                withCsrf(
                                        post(
                                                "/api/auth/refresh"
                                        ),
                                        csrf
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
         * 새 Access Token이 실제 JWT Filter를 통과하는지도
         * 다시 검증합니다.
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
    void logout은_CSRF없이_차단되며_DB_refresh_token도_삭제되면_안된다()
            throws Exception {

        String email =
                uniqueEmail(
                        "logout-blocked"
                );

        MvcResult signupResult =
                signupWithCsrf(
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
                                .isForbidden()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        )
                                .value(
                                        "AUTH403_2"
                                )
                );

        /*
         * CSRF Filter에서 차단되었으므로
         * LogoutHandler가 refresh token을 삭제해서는 안 됩니다.
         */
        AppUser savedUser =
                findLocalUser(
                        email
                );

        assertEquals(
                refreshToken,
                savedUser.getRefreshToken()
        );
    }

    @Test
    void 정상_CSRF_logout은_DB_refresh_token과_JWT쿠키를_폐기한다()
            throws Exception {

        String email =
                uniqueEmail(
                        "logout"
                );

        MvcResult signupResult =
                signupWithCsrf(
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

        assertEquals(
                refreshToken,
                beforeLogout.getRefreshToken()
        );

        /*
         * 회원가입 성공 시 이전 CSRF는 폐기됐으므로
         * 로그아웃에 사용할 새 CSRF를 가져옵니다.
         */
        CsrfContext csrf =
                fetchCsrf(
                        new Cookie(
                                JwtCookieUtils
                                        .ACCESS_TOKEN_COOKIE_NAME,
                                accessToken
                        )
                );

        MvcResult logoutResult =
                mockMvc.perform(
                                withCsrf(
                                        post(
                                                "/api/logout"
                                        ),
                                        csrf
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

    private MvcResult signupWithCsrf(
            String email,
            String password
    ) throws Exception {

        CsrfContext csrf =
                fetchCsrf();

        MvcResult result =
                mockMvc.perform(
                                withCsrf(
                                        post(
                                                "/api/auth/signup"
                                        ),
                                        csrf
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

        assertCsrfCookieExpired(
                result
        );

        return result;
    }

    /**
     * 실제 GET /api/auth/csrf 응답을 이용합니다.
     *
     * Spring Security Test의 .with(csrf())를 사용하지 않아
     * 실제 production CookieCsrfTokenRepository 흐름과
     * 동일한 Cookie + Header 조합을 검증할 수 있습니다.
     */
    private CsrfContext fetchCsrf(
            Cookie... cookies
    ) throws Exception {

        MockHttpServletRequestBuilder request =
                get(
                        "/api/auth/csrf"
                );

        if (
                cookies != null
                        && cookies.length > 0
        ) {
            request.cookie(
                    cookies
            );
        }

        MvcResult result =
                mockMvc.perform(
                                request
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
                                        "$.result.headerName"
                                )
                                        .value(
                                                "X-XSRF-TOKEN"
                                        )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.result.token"
                                )
                                        .isString()
                        )
                        .andReturn();

        JsonNode responseBody =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        String headerName =
                responseBody
                        .path("result")
                        .path("headerName")
                        .asText();

        String token =
                responseBody
                        .path("result")
                        .path("token")
                        .asText();

        /*
         * 중요:
         *
         * CookieCsrfTokenRepository는 Servlet 6+ 환경에서
         * SameSite를 jakarta.servlet.http.Cookie의
         * attribute로 설정한 뒤 response.addCookie()를 사용합니다.
         *
         * MockMvc의 문자열 Set-Cookie 표현에서는
         * 해당 일반 attribute가 생략될 수 있으므로,
         * 문자열을 다시 parse하지 않고 Mock response가
         * 실제로 보관하는 Cookie 객체를 직접 검사합니다.
         */
        Cookie csrfCookie =
                result.getResponse()
                        .getCookie(
                                CSRF_COOKIE_NAME
                        );

        assertNotNull(
                csrfCookie,
                "응답에 XSRF-TOKEN Cookie가 없습니다."
        );

        String cookieValue =
                csrfCookie.getValue();

        assertFalse(
                token.isBlank()
        );

        assertFalse(
                cookieValue.isBlank()
        );

        assertTrue(
                csrfCookie.isHttpOnly()
        );

        assertEquals(
                "/",
                csrfCookie.getPath()
        );

        assertEquals(
                "Lax",
                csrfCookie.getAttribute(
                        "SameSite"
                ),
                "MockMvc가 보관한 실제 CSRF Cookie의 SameSite 속성이 올바르지 않습니다."
        );

        return new CsrfContext(
                cookieValue,
                headerName,
                token
        );
    }

    private MockHttpServletRequestBuilder
    withCsrf(
            MockHttpServletRequestBuilder request,
            CsrfContext csrf
    ) {
        return request
                .cookie(
                        csrf.cookie()
                )
                .header(
                        csrf.headerName(),
                        csrf.token()
                );
    }

    private void assertCsrfCookieExpired(
            MvcResult result
    ) {
        String csrfCookieHeader =
                findCookieHeader(
                        result,
                        CSRF_COOKIE_NAME
                );

        assertTrue(
                csrfCookieHeader
                        .contains(
                                "Max-Age=0"
                        )
        );
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

    private String onboardingBody() {
        return """
                {
                  "nickname": "데이빛",
                  "reminderTime": "21:30",
                  "dayStartTime": "06:00",
                  "aiMemoryConsent": true
                }
                """;
    }

    private String uniqueEmail(
            String prefix
    ) {
        return prefix
                + "-"
                + System.nanoTime()
                + "@example.com";
    }

    private record CsrfContext(
            String cookieValue,
            String headerName,
            String token
    ) {

        private Cookie cookie() {
            return new Cookie(
                    FormAuthHttpIntegrationTest.CSRF_COOKIE_NAME,
                    cookieValue
            );
        }
    }
}
