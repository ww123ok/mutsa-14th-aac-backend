package mutsa.hackathon.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.hackathon.dto.AuthTokenResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.JwtAuthService;
import mutsa.hackathon.util.JwtCookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtAuthService jwtAuthService;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final String successRedirectUri;

    public OAuth2AuthenticationSuccessHandler(
            JwtAuthService jwtAuthService,
            @Value("${app.jwt.cookie-secure:false}")
            boolean cookieSecure,
            @Value("${app.jwt.cookie-same-site:Lax}")
            String cookieSameSite,
            @Value("${app.oauth2.success-redirect-uri}")
            String successRedirectUri
    ) {
        this.jwtAuthService = jwtAuthService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.successRedirectUri = successRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2User principal =
                (CustomOAuth2User)
                        authentication.getPrincipal();

        AuthTokenResponse tokenResponse =
                jwtAuthService.issueTokens(
                        principal.getKakaoUserProfile()
                );

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

        getRedirectStrategy().sendRedirect(
                request,
                response,
                successRedirectUri
        );
    }
}