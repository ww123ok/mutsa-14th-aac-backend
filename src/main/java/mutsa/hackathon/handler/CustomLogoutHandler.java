package mutsa.hackathon.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.AppUserService;
import mutsa.hackathon.util.JwtCookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomLogoutHandler implements LogoutHandler {
    private final AppUserService appUserService;
    @Value("${app.jwt.cookie-secure:false}") private boolean cookieSecure;
    @Value("${app.jwt.cookie-same-site:Lax}") private String cookieSameSite;

    public CustomLogoutHandler(AppUserService appUserService) { this.appUserService = appUserService; }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomOAuth2User principal) {
            appUserService.clearRefreshToken(principal.getKakaoUserProfile().id());
        }
        JwtCookieUtils.expireCookie(response, JwtCookieUtils.ACCESS_TOKEN_COOKIE_NAME, cookieSecure, cookieSameSite);
        JwtCookieUtils.expireCookie(response, JwtCookieUtils.REFRESH_TOKEN_COOKIE_NAME, cookieSecure, cookieSameSite);
    }
}