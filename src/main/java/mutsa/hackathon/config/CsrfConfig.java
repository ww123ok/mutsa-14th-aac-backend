package mutsa.hackathon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import java.util.Locale;

@Configuration
public class CsrfConfig {

    @Bean
    public CsrfTokenRepository csrfTokenRepository(
            @Value(
                    "${app.jwt.cookie-secure:false}"
            )
            boolean cookieSecure,

            @Value(
                    "${app.jwt.cookie-same-site:Lax}"
            )
            String cookieSameSite
    ) {
        String normalizedSameSite =
                normalizeSameSite(
                        cookieSameSite
                );

        validateCookiePolicy(
                cookieSecure,
                normalizedSameSite
        );

        CookieCsrfTokenRepository repository =
                new CookieCsrfTokenRepository();

        repository.setCookieName(
                "XSRF-TOKEN"
        );

        repository.setHeaderName(
                "X-XSRF-TOKEN"
        );

        repository.setCookiePath("/");

        /*
         * 프론트는 Cookie 자체를 읽지 않고
         * GET /api/auth/csrf의 JSON 응답에서
         * CSRF token 값을 받음.
         * 따라서 CSRF Cookie도 HttpOnly=true로
         * 유지할 수 있음.
         */
        repository.setCookieCustomizer(
                cookie -> cookie
                        .httpOnly(true)
                        .secure(
                                cookieSecure
                        )
                        .sameSite(
                                normalizedSameSite
                        )
                        .path("/")
        );

        return repository;
    }

    private String normalizeSameSite(
            String sameSite
    ) {
        if (
                sameSite == null
                        || sameSite.isBlank()
        ) {
            return "Lax";
        }

        return switch (
                sameSite.trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
                ) {
            case "lax" -> "Lax";
            case "strict" -> "Strict";
            case "none" -> "None";

            default ->
                    throw new IllegalArgumentException(
                            "SameSite는 Lax, Strict, None 중 하나여야 합니다."
                    );
        };
    }

    private void validateCookiePolicy(
            boolean secure,
            String sameSite
    ) {
        if (
                "None".equals(sameSite)
                        && !secure
        ) {
            throw new IllegalArgumentException(
                    "SameSite=None 쿠키는 Secure=true가 필요합니다."
            );
        }
    }
}