package mutsa.hackathon.presentation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 배포 환경에서 사용할 Web Security 정책을 검증합니다.
 *
 * 검증 대상:
 *
 * 1. Vercel Frontend Origin의 credentialed CORS
 * 2. X-XSRF-TOKEN preflight 허용
 * 3. 허용하지 않은 Origin 차단
 * 4. Production CSRF Cookie의 Secure + SameSite=None
 * 5. Form Signup으로 발급되는 JWT Cookie의
 *    HttpOnly + Secure + SameSite=None
 */
@SpringBootTest(
        properties = {
                "app.jwt.cookie-secure=true",
                "app.jwt.cookie-same-site=None"
        }
)
@AutoConfigureMockMvc
class ProductionWebSecurityIntegrationTest {

    private static final String
            FRONTEND_ORIGIN =
            "https://likelion14th-hackathon.vercel.app";

    private static final String
            UNTRUSTED_ORIGIN =
            "https://evil.example";

    private static final String
            CSRF_COOKIE_NAME =
            "XSRF-TOKEN";

    private static final String
            ACCESS_TOKEN_COOKIE_NAME =
            "access_token";

    private static final String
            REFRESH_TOKEN_COOKIE_NAME =
            "refresh_token";

    private static final String
            CSRF_HEADER_NAME =
            "X-XSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void Vercel에서_로그인_POST를_위한_CORS_preflight가_허용된다()
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                options(
                                        "/api/auth/login"
                                )
                                        .header(
                                                HttpHeaders.ORIGIN,
                                                FRONTEND_ORIGIN
                                        )
                                        .header(
                                                HttpHeaders
                                                        .ACCESS_CONTROL_REQUEST_METHOD,
                                                "POST"
                                        )
                                        .header(
                                                HttpHeaders
                                                        .ACCESS_CONTROL_REQUEST_HEADERS,
                                                "content-type,"
                                                        + "x-xsrf-token"
                                        )
                        )
                        .andExpect(
                                status()
                                        .isOk()
                        )
                        .andReturn();

        assertEquals(
                FRONTEND_ORIGIN,
                result.getResponse()
                        .getHeader(
                                HttpHeaders
                                        .ACCESS_CONTROL_ALLOW_ORIGIN
                        )
        );

        assertEquals(
                "true",
                result.getResponse()
                        .getHeader(
                                HttpHeaders
                                        .ACCESS_CONTROL_ALLOW_CREDENTIALS
                        )
        );

        String allowedMethods =
                result.getResponse()
                        .getHeader(
                                HttpHeaders
                                        .ACCESS_CONTROL_ALLOW_METHODS
                        );

        assertNotNull(
                allowedMethods
        );

        assertTrue(
                allowedMethods
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .contains(
                                "POST"
                        )
        );

        String allowedHeaders =
                result.getResponse()
                        .getHeader(
                                HttpHeaders
                                        .ACCESS_CONTROL_ALLOW_HEADERS
                        );

        assertNotNull(
                allowedHeaders
        );

        String normalizedAllowedHeaders =
                allowedHeaders
                        .toLowerCase(
                                Locale.ROOT
                        );

        assertTrue(
                normalizedAllowedHeaders
                        .contains(
                                "content-type"
                        )
        );

        assertTrue(
                normalizedAllowedHeaders
                        .contains(
                                "x-xsrf-token"
                        )
        );
    }

    @Test
    void 허용하지_않은_Origin의_CORS_preflight는_차단된다()
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                options(
                                        "/api/auth/login"
                                )
                                        .header(
                                                HttpHeaders.ORIGIN,
                                                UNTRUSTED_ORIGIN
                                        )
                                        .header(
                                                HttpHeaders
                                                        .ACCESS_CONTROL_REQUEST_METHOD,
                                                "POST"
                                        )
                                        .header(
                                                HttpHeaders
                                                        .ACCESS_CONTROL_REQUEST_HEADERS,
                                                "content-type,"
                                                        + "x-xsrf-token"
                                        )
                        )
                        .andExpect(
                                status()
                                        .isForbidden()
                        )
                        .andReturn();

        assertNull(
                result.getResponse()
                        .getHeader(
                                HttpHeaders
                                        .ACCESS_CONTROL_ALLOW_ORIGIN
                        )
        );
    }

    @Test
    void Vercel에서_CSRF를_요청하면_CORS와_Production_Cookie정책이_함께_적용된다()
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                get(
                                        "/api/auth/csrf"
                                )
                                        .header(
                                                HttpHeaders.ORIGIN,
                                                FRONTEND_ORIGIN
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
                                        "$.result.headerName"
                                )
                                        .value(
                                                CSRF_HEADER_NAME
                                        )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.result.token"
                                )
                                        .isString()
                        )
                        .andReturn();

        assertEquals(
                FRONTEND_ORIGIN,
                result.getResponse()
                        .getHeader(
                                HttpHeaders
                                        .ACCESS_CONTROL_ALLOW_ORIGIN
                        )
        );

        assertEquals(
                "true",
                result.getResponse()
                        .getHeader(
                                HttpHeaders
                                        .ACCESS_CONTROL_ALLOW_CREDENTIALS
                        )
        );

        Cookie csrfCookie =
                result.getResponse()
                        .getCookie(
                                CSRF_COOKIE_NAME
                        );

        assertNotNull(
                csrfCookie
        );

        assertFalse(
                csrfCookie.getValue()
                        .isBlank()
        );

        assertTrue(
                csrfCookie.isHttpOnly()
        );

        assertTrue(
                csrfCookie.getSecure()
        );

        assertEquals(
                "/",
                csrfCookie.getPath()
        );

        assertEquals(
                "None",
                csrfCookie.getAttribute(
                        "SameSite"
                )
        );
    }

    @Test
    void Production_회원가입은_Secure_SameSiteNone_JWT쿠키를_발급한다()
            throws Exception {

        CsrfContext csrf =
                fetchCsrf();

        String email =
                uniqueEmail(
                        "prod-cookie"
                );

        MvcResult signupResult =
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
                                        )
                                        .header(
                                                HttpHeaders.ORIGIN,
                                                FRONTEND_ORIGIN
                                        )
                                        .contentType(
                                                MediaType
                                                        .APPLICATION_JSON
                                        )
                                        .content(
                                                authBody(
                                                        email,
                                                        "password-1234"
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
                        )
                        .andReturn();

        assertEquals(
                FRONTEND_ORIGIN,
                signupResult.getResponse()
                        .getHeader(
                                HttpHeaders
                                        .ACCESS_CONTROL_ALLOW_ORIGIN
                        )
        );

        assertEquals(
                "true",
                signupResult.getResponse()
                        .getHeader(
                                HttpHeaders
                                        .ACCESS_CONTROL_ALLOW_CREDENTIALS
                        )
        );

        assertProductionJwtCookie(
                signupResult,
                ACCESS_TOKEN_COOKIE_NAME
        );

        assertProductionJwtCookie(
                signupResult,
                REFRESH_TOKEN_COOKIE_NAME
        );
    }

    private CsrfContext fetchCsrf()
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                get(
                                        "/api/auth/csrf"
                                )
                                        .header(
                                                HttpHeaders.ORIGIN,
                                                FRONTEND_ORIGIN
                                        )
                        )
                        .andExpect(
                                status()
                                        .isOk()
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

        Cookie csrfCookie =
                result.getResponse()
                        .getCookie(
                                CSRF_COOKIE_NAME
                        );

        assertNotNull(
                csrfCookie
        );

        assertFalse(
                token.isBlank()
        );

        assertFalse(
                csrfCookie.getValue()
                        .isBlank()
        );

        assertTrue(
                csrfCookie.isHttpOnly()
        );

        assertTrue(
                csrfCookie.getSecure()
        );

        assertEquals(
                "None",
                csrfCookie.getAttribute(
                        "SameSite"
                )
        );

        return new CsrfContext(
                csrfCookie.getValue(),
                headerName,
                token
        );
    }

    private void assertProductionJwtCookie(
            MvcResult result,
            String cookieName
    ) {

        String cookieHeader =
                findCookieHeader(
                        result,
                        cookieName
                );

        assertTrue(
                cookieHeader.contains(
                        "HttpOnly"
                ),
                cookieName
                        + "에 HttpOnly가 없습니다: "
                        + cookieHeader
        );

        assertTrue(
                cookieHeader.contains(
                        "Secure"
                ),
                cookieName
                        + "에 Secure가 없습니다: "
                        + cookieHeader
        );

        assertTrue(
                cookieHeader.contains(
                        "Path=/"
                ),
                cookieName
                        + "의 Path가 올바르지 않습니다: "
                        + cookieHeader
        );

        assertTrue(
                cookieHeader.contains(
                        "SameSite=None"
                ),
                cookieName
                        + "의 SameSite가 None이 아닙니다: "
                        + cookieHeader
        );

        String cookieValue =
                extractCookieValue(
                        cookieHeader,
                        cookieName
                );

        assertFalse(
                cookieValue.isBlank()
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

    private String extractCookieValue(
            String cookieHeader,
            String cookieName
    ) {

        String prefix =
                cookieName + "=";

        int endIndex =
                cookieHeader.indexOf(
                        ';',
                        prefix.length()
                );

        if (endIndex < 0) {
            endIndex =
                    cookieHeader.length();
        }

        return cookieHeader.substring(
                prefix.length(),
                endIndex
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

    private record CsrfContext(
            String cookieValue,
            String headerName,
            String token
    ) {

        private Cookie cookie() {
            return new Cookie(
                    CSRF_COOKIE_NAME,
                    cookieValue
            );
        }
    }
}