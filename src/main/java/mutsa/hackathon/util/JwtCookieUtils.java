package mutsa.hackathon.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Locale;

public final class JwtCookieUtils {

    public static final String ACCESS_TOKEN_COOKIE_NAME =
            "access_token";

    public static final String REFRESH_TOKEN_COOKIE_NAME =
            "refresh_token";

    private JwtCookieUtils() {
    }

    public static void addTokenCookie(
            HttpServletResponse response,
            String name,
            String value,
            long maxAgeSeconds,
            boolean secure,
            String sameSite
    ) {
        String normalizedSameSite =
                normalizeSameSite(sameSite);

        validateCookiePolicy(
                secure,
                normalizedSameSite
        );

        ResponseCookie cookie = ResponseCookie
                .from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(normalizedSameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    public static void expireCookie(
            HttpServletResponse response,
            String name,
            boolean secure,
            String sameSite
    ) {
        String normalizedSameSite =
                normalizeSameSite(sameSite);

        validateCookiePolicy(
                secure,
                normalizedSameSite
        );

        ResponseCookie cookie = ResponseCookie
                .from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(normalizedSameSite)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    public static Cookie getCookie(
            HttpServletRequest request,
            String name
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }

    private static String normalizeSameSite(
            String sameSite
    ) {
        if (sameSite == null || sameSite.isBlank()) {
            return "Lax";
        }

        return switch (
                sameSite.trim()
                        .toLowerCase(Locale.ROOT)
                ) {
            case "lax" -> "Lax";
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> throw new IllegalArgumentException(
                    "SameSite는 Lax, Strict, None 중 하나여야 합니다."
            );
        };
    }

    private static void validateCookiePolicy(
            boolean secure,
            String sameSite
    ) {
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalArgumentException(
                    "SameSite=None 쿠키는 Secure=true가 필요합니다."
            );
        }
    }
}