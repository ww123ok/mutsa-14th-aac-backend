package mutsa.hackathon.presentation;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import mutsa.hackathon.dto.AuthTokenResponse;
import mutsa.hackathon.dto.MeResponse;
import mutsa.hackathon.dto.MeUpdateRequest;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.AppUserService;
import mutsa.hackathon.service.JwtAuthService;
import mutsa.hackathon.util.JwtCookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final JwtAuthService jwtAuthService;
    private final AppUserService appUserService;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            JwtAuthService jwtAuthService,
            AppUserService appUserService,
            @Value("${app.jwt.cookie-secure:false}")
            boolean cookieSecure,
            @Value("${app.jwt.cookie-same-site:Lax}")
            String cookieSameSite
    ) {
        this.jwtAuthService = jwtAuthService;
        this.appUserService = appUserService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        MeResponse response =
                appUserService.findMe(
                        user.getKakaoUserProfile().id()
                );

        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/me")
    public ApiResponse<MeResponse> updateMe(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @Valid
            @RequestBody
            MeUpdateRequest request
    ) {
        MeResponse response =
                appUserService.updateMe(
                        user.getKakaoUserProfile().id(),
                        request
                );

        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<AuthTokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Cookie refreshTokenCookie =
                JwtCookieUtils.getCookie(
                        request,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        if (refreshTokenCookie == null) {
            throw new ProjectException(
                    ErrorCode.INVALID_TOKEN
            );
        }

        AuthTokenResponse tokenResponse =
                jwtAuthService.refresh(
                        refreshTokenCookie.getValue()
                );

        JwtCookieUtils.addTokenCookie(
                response,
                JwtCookieUtils.ACCESS_TOKEN_COOKIE_NAME,
                tokenResponse.accessToken(),
                tokenResponse.accessTokenExpiresIn()
                        / 1000,
                cookieSecure,
                cookieSameSite
        );

        JwtCookieUtils.addTokenCookie(
                response,
                JwtCookieUtils.REFRESH_TOKEN_COOKIE_NAME,
                tokenResponse.refreshToken(),
                tokenResponse.refreshTokenExpiresIn()
                        / 1000,
                cookieSecure,
                cookieSameSite
        );

        return ApiResponse.onSuccess(
                tokenResponse
        );
    }
}