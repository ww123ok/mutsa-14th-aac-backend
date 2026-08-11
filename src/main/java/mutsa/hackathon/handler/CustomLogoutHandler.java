package mutsa.hackathon.handler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.hackathon.security.JwtCookieService;
import mutsa.hackathon.service.AppUserService;
import mutsa.hackathon.util.JwtCookieUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomLogoutHandler
        implements LogoutHandler {

    private final AppUserService
            appUserService;

    private final JwtCookieService
            jwtCookieService;

    public CustomLogoutHandler(
            AppUserService appUserService,
            JwtCookieService jwtCookieService
    ) {
        this.appUserService =
                appUserService;

        this.jwtCookieService =
                jwtCookieService;
    }

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        /*
         * Stateless JWT 환경에서는 LogoutFilter 실행 시점에
         * SecurityContext Authentication이 아직 없을 수 있음.
         * 따라서 브라우저가 보낸 refresh_token Cookie를
         * 직접 기준으로 서버에 저장된 refresh token을 폐기.
         */
        Cookie refreshTokenCookie =
                JwtCookieUtils.getCookie(
                        request,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        if (refreshTokenCookie != null) {
            appUserService
                    .clearRefreshTokenByValue(
                            refreshTokenCookie
                                    .getValue()
                    );
        }

        /*
         * DB token 폐기 여부와 관계없이
         * 브라우저의 인증 Cookie는 항상 만료시킴
         */
        jwtCookieService
                .expireTokenCookies(
                        response
                );
    }
}