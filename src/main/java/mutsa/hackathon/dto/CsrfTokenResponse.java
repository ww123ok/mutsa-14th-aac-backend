package mutsa.hackathon.dto;

import org.springframework.security.web.csrf.CsrfToken;

public record CsrfTokenResponse(
        String headerName,
        String token
) {

    public static CsrfTokenResponse from(
            CsrfToken csrfToken
    ) {
        if (csrfToken == null) {
            throw new IllegalArgumentException(
                    "CSRF 토큰은 필수입니다."
            );
        }

        return new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getToken()
        );
    }
}