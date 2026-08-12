package mutsa.hackathon.security;

import jakarta.servlet.http.HttpServletResponse;
import mutsa.hackathon.dto.AuthTokenResponse;
import mutsa.hackathon.util.JwtCookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 인증 Cookie 정책을 한 곳에서 관리.
 * OAuth 로그인, 이메일 로그인, refresh, logout이
 * 서로 다른 Cookie 옵션을 사용하는 문제를 방지.
 */
@Component
public class JwtCookieService {

    private final boolean cookieSecure;
    private final String cookieSameSite;

    public JwtCookieService(
            @Value(
                    "${app.jwt.cookie-secure:false}"
            )
            boolean cookieSecure,

            @Value(
                    "${app.jwt.cookie-same-site:Lax}"
            )
            String cookieSameSite
    ) {
        this.cookieSecure =
                cookieSecure;

        this.cookieSameSite =
                cookieSameSite;
    }

    /**
     * Access / Refresh Token을
     * HttpOnly Cookie로 함께 발급
     */
    public void addTokenCookies(
            HttpServletResponse response,
            AuthTokenResponse tokenResponse
    ) {
        if (tokenResponse == null) {
            throw new IllegalArgumentException(
                    "발급할 인증 토큰은 필수입니다."
            );
        }

        JwtCookieUtils.addTokenCookie(
                response,
                JwtCookieUtils
                        .ACCESS_TOKEN_COOKIE_NAME,
                tokenResponse.accessToken(),
                tokenResponse
                        .accessTokenExpiresIn()
                        / 1000,
                cookieSecure,
                cookieSameSite
        );

        JwtCookieUtils.addTokenCookie(
                response,
                JwtCookieUtils
                        .REFRESH_TOKEN_COOKIE_NAME,
                tokenResponse.refreshToken(),
                tokenResponse
                        .refreshTokenExpiresIn()
                        / 1000,
                cookieSecure,
                cookieSameSite
        );
    }

    /**
     * 로그아웃 시 두 인증 Cookie를
     * 동일한 Cookie 정책으로 만료
     */
    public void expireTokenCookies(
            HttpServletResponse response
    ) {
        JwtCookieUtils.expireCookie(
                response,
                JwtCookieUtils
                        .ACCESS_TOKEN_COOKIE_NAME,
                cookieSecure,
                cookieSameSite
        );

        JwtCookieUtils.expireCookie(
                response,
                JwtCookieUtils
                        .REFRESH_TOKEN_COOKIE_NAME,
                cookieSecure,
                cookieSameSite
        );
    }
}