package mutsa.hackathon.handler;

import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.dto.AuthTokenResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.JwtAuthService;
import mutsa.hackathon.util.JwtCookieUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2AuthenticationHandlerTest {

    private static final String SUCCESS_REDIRECT_URI =
            "http://localhost:3000/oauth2/callback/kakao";

    private static final String FAILURE_REDIRECT_URI =
            "http://localhost:3000/?error=oauth2_login_failed";

    @Test
    void 카카오_로그인_성공시_토큰_쿠키를_발급하고_프론트_콜백으로_이동한다()
            throws Exception {

        KakaoUserProfile profile = createProfile();

        AuthTokenResponse tokenResponse =
                new AuthTokenResponse(
                        "access-token",
                        "refresh-token",
                        "Bearer",
                        1_800_000L,
                        1_209_600_000L
                );

        JwtAuthService jwtAuthService =
                new JwtAuthService(null, null) {
                    @Override
                    public AuthTokenResponse issueTokens(
                            KakaoUserProfile ignoredProfile
                    ) {
                        return tokenResponse;
                    }
                };

        OAuth2AuthenticationSuccessHandler handler =
                new OAuth2AuthenticationSuccessHandler(
                        jwtAuthService,
                        false,
                        SUCCESS_REDIRECT_URI
                );

        CustomOAuth2User principal =
                new CustomOAuth2User(
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_USER"
                                )
                        ),
                        Map.of("id", profile.providerId()),
                        "id",
                        profile
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        assertEquals(
                SUCCESS_REDIRECT_URI,
                response.getRedirectedUrl()
        );

        Collection<String> setCookieHeaders =
                response.getHeaders(HttpHeaders.SET_COOKIE);

        assertEquals(2, setCookieHeaders.size());

        assertTrue(
                setCookieHeaders.stream()
                        .anyMatch(header ->
                                header.startsWith(
                                        JwtCookieUtils
                                                .ACCESS_TOKEN_COOKIE_NAME
                                                + "=access-token"
                                )
                        )
        );

        assertTrue(
                setCookieHeaders.stream()
                        .anyMatch(header ->
                                header.startsWith(
                                        JwtCookieUtils
                                                .REFRESH_TOKEN_COOKIE_NAME
                                                + "=refresh-token"
                                )
                        )
        );
    }

    @Test
    void 카카오_로그인_실패시_프론트_실패_주소로_이동한다()
            throws Exception {

        OAuth2AuthenticationFailureHandler handler =
                new OAuth2AuthenticationFailureHandler(
                        FAILURE_REDIRECT_URI
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new AuthenticationServiceException(
                        "카카오 로그인 실패"
                )
        );

        assertEquals(
                FAILURE_REDIRECT_URI,
                response.getRedirectedUrl()
        );
    }

    private KakaoUserProfile createProfile() {
        return new KakaoUserProfile(
                1L,
                "kakao",
                "test-provider-id",
                "테스트 사용자",
                null,
                null,
                LocalDateTime.now(),
                false,
                null,
                0,
                false
        );
    }
}