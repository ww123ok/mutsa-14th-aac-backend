package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.dto.AuthLoginRequest;
import mutsa.hackathon.dto.AuthSignupRequest;
import mutsa.hackathon.dto.AuthTokenResponse;
import mutsa.hackathon.dto.MeResponse;
import org.springframework.stereotype.Service;

/**
 * 이메일/비밀번호 인증의 Application 흐름을 담당.
 * Controller가
 * 회원가입 → 사용자 조회 → JWT 발급
 * 같은 여러 Service 호출 순서를 직접 알지 않도록
 * orchestration 책임을 이곳에 둠.
 */
@Service
@RequiredArgsConstructor
public class FormAuthService {

    private final EmailPasswordAuthService
            emailPasswordAuthService;

    private final AppUserService
            appUserService;

    private final JwtAuthService
            jwtAuthService;

    /**
     * 일반 계정을 생성한 뒤 즉시 인증 Session을 발급.
     * 회원가입 직후 인증 상태가 되므로
     * 프론트는 바로 기존 onboarding API를 사용할 수 있음.
     */
    public FormAuthResult signup(
            AuthSignupRequest request
    ) {
        Long userId =
                emailPasswordAuthService
                        .register(
                                request.email(),
                                request.password()
                        );

        return issueSession(
                userId
        );
    }

    /**
     * 이메일/비밀번호 검증 후
     * Kakao와 동일한 JWT Session을 발급
     */
    public FormAuthResult login(
            AuthLoginRequest request
    ) {
        Long userId =
                emailPasswordAuthService
                        .authenticate(
                                request.email(),
                                request.password()
                        );

        return issueSession(
                userId
        );
    }

    private FormAuthResult issueSession(
            Long userId
    ) {
        /*
         * 기존 JWT 구조를 그대로 재사용.
         * KakaoUserProfile이라는 현재 타입 이름은
         * 역사적인 이름이지만 실제 record 안에는
         * provider가 포함되어 있어 local 사용자도
         * 표현할 수 있음.
         */
        KakaoUserProfile profile =
                appUserService
                        .findProfileById(
                                userId
                        );

        AuthTokenResponse tokenResponse =
                jwtAuthService
                        .issueTokens(
                                profile
                        );

        MeResponse meResponse =
                appUserService
                        .findMe(
                                userId
                        );

        return new FormAuthResult(
                tokenResponse,
                meResponse
        );
    }

    /**
     * Token은 Controller가 HttpOnly Cookie로만 사용하고,
     * JSON 응답에는 meResponse만 노출
     */
    public record FormAuthResult(
            AuthTokenResponse tokenResponse,
            MeResponse meResponse
    ) {
    }
}