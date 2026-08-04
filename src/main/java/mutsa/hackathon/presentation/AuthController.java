package mutsa.hackathon.presentation;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.dto.AuthTokenResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.JwtAuthService;
import mutsa.hackathon.util.JwtCookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
    public class AuthController {
        private final JwtAuthService jwtAuthService;
        private final boolean cookieSecure;

        @Value("${app.jwt.cookie-same-site:Lax}")
        private String cookieSameSite;

        public AuthController(JwtAuthService jwtAuthService, @Value("${app.jwt.cookie-secure:false}") boolean cookieSecure) {
            this.jwtAuthService = jwtAuthService;
            this.cookieSecure = cookieSecure;
        }

        @GetMapping("/me")
        public ApiResponse<KakaoUserProfile> me(@AuthenticationPrincipal CustomOAuth2User user) {
            return ApiResponse.onSuccess(user.getKakaoUserProfile());
        }

    @PostMapping("/auth/refresh")
    public ApiResponse<AuthTokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Cookie refreshTokenCookie = JwtCookieUtils.getCookie(
                request,
                JwtCookieUtils.REFRESH_TOKEN_COOKIE_NAME
        );

        if (refreshTokenCookie == null) {
            throw new IllegalArgumentException("Refresh token cookie is missing");
        }

        AuthTokenResponse tokenResponse =
                jwtAuthService.refresh(refreshTokenCookie.getValue());

        JwtCookieUtils.addTokenCookie(
                response,
                JwtCookieUtils.ACCESS_TOKEN_COOKIE_NAME,
                tokenResponse.accessToken(),
                tokenResponse.accessTokenExpiresIn() / 1000,
                cookieSecure,
                cookieSameSite
        );

        JwtCookieUtils.addTokenCookie(
                response,
                JwtCookieUtils.REFRESH_TOKEN_COOKIE_NAME,
                tokenResponse.refreshToken(),
                tokenResponse.refreshTokenExpiresIn() / 1000,
                cookieSecure,
                cookieSameSite
        );

        return ApiResponse.onSuccess(tokenResponse);
    }
    }