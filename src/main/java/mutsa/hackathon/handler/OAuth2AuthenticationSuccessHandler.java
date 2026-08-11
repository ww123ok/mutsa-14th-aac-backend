package mutsa.hackathon.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.hackathon.dto.AuthTokenResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.security.JwtCookieService;
import mutsa.hackathon.service.JwtAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtAuthService
            jwtAuthService;

    private final JwtCookieService
            jwtCookieService;

    private final String
            successRedirectUri;

    public OAuth2AuthenticationSuccessHandler(
            JwtAuthService jwtAuthService,
            JwtCookieService jwtCookieService,

            @Value(
                    "${app.oauth2.success-redirect-uri}"
            )
            String successRedirectUri
    ) {
        this.jwtAuthService =
                jwtAuthService;

        this.jwtCookieService =
                jwtCookieService;

        this.successRedirectUri =
                successRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2User principal =
                (CustomOAuth2User)
                        authentication
                                .getPrincipal();

        AuthTokenResponse tokenResponse =
                jwtAuthService.issueTokens(
                        principal
                                .getKakaoUserProfile()
                );

        jwtCookieService.addTokenCookies(
                response,
                tokenResponse
        );

        getRedirectStrategy()
                .sendRedirect(
                        request,
                        response,
                        successRedirectUri
                );
    }
}