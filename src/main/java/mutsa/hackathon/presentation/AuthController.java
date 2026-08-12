package mutsa.hackathon.presentation;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import mutsa.hackathon.dto.AuthLoginRequest;
import mutsa.hackathon.dto.AuthSignupRequest;
import mutsa.hackathon.dto.AuthTokenResponse;
import mutsa.hackathon.dto.CsrfTokenResponse;
import mutsa.hackathon.dto.MeResponse;
import mutsa.hackathon.dto.MeUpdateRequest;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.code.SuccessCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.security.JwtCookieService;
import mutsa.hackathon.service.AppUserService;
import mutsa.hackathon.service.FormAuthService;
import mutsa.hackathon.service.JwtAuthService;
import mutsa.hackathon.util.JwtCookieUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final JwtAuthService
            jwtAuthService;

    private final AppUserService
            appUserService;

    private final FormAuthService
            formAuthService;

    private final JwtCookieService
            jwtCookieService;

    private final CsrfTokenRepository
            csrfTokenRepository;

    public AuthController(
            JwtAuthService jwtAuthService,
            AppUserService appUserService,
            FormAuthService formAuthService,
            JwtCookieService jwtCookieService,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.jwtAuthService =
                jwtAuthService;

        this.appUserService =
                appUserService;

        this.formAuthService =
                formAuthService;

        this.jwtCookieService =
                jwtCookieService;

        this.csrfTokenRepository =
                csrfTokenRepository;
    }

    /**
     * SPA가 unsafe 요청 전에 사용할
     * CSRF token을 발급.
     * 브라우저는 XSRF-TOKEN Cookie를 보관하고,
     * 프론트는 응답의 token 값을
     * X-XSRF-TOKEN Header에 담음.
     */
    @GetMapping("/auth/csrf")
    public ApiResponse<CsrfTokenResponse>
    csrf(
            CsrfToken csrfToken
    ) {
        return ApiResponse.onSuccess(
                CsrfTokenResponse.from(
                        csrfToken
                )
        );
    }

    /**
     * 이메일/비밀번호 회원가입.
     * 가입 성공과 동시에 Access / Refresh Cookie를 발급하여
     * 기존 onboarding API를 바로 사용할 수 있게 함.
     */
    @PostMapping("/auth/signup")
    public ResponseEntity<
            ApiResponse<MeResponse>
            > signup(

            @Valid
            @RequestBody
            AuthSignupRequest request,

            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        FormAuthService.FormAuthResult
                authResult =
                formAuthService.signup(
                        request
                );

        jwtCookieService.addTokenCookies(
                response,
                authResult.tokenResponse()
        );

        /*
         * 인증 상태가 바뀌었으므로
         * 로그인 전에 사용한 CSRF token은 폐기.
         * 프론트는 성공 후
         * GET /api/auth/csrf를 다시 호출.
         */
        csrfTokenRepository.saveToken(
                null,
                httpRequest,
                response
        );

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        ApiResponse.onSuccess(
                                SuccessCode.CREATED,
                                authResult
                                        .meResponse()
                        )
                );
    }

    /**
     * 이메일/비밀번호 로그인.
     * JWT를 JSON Body에 노출하지 않고
     * HttpOnly Cookie로 발급.
     */
    @PostMapping("/auth/login")
    public ApiResponse<MeResponse> login(

            @Valid
            @RequestBody
            AuthLoginRequest request,

            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        FormAuthService.FormAuthResult
                authResult =
                formAuthService.login(
                        request
                );

        jwtCookieService.addTokenCookies(
                response,
                authResult.tokenResponse()
        );

        /*
         * 로그인 전 CSRF token을 재사용하지 않음
         */
        csrfTokenRepository.saveToken(
                null,
                httpRequest,
                response
        );

        return ApiResponse.onSuccess(
                authResult.meResponse()
        );
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        MeResponse response =
                appUserService.findMe(
                        user.getKakaoUserProfile()
                                .id()
                );

        return ApiResponse.onSuccess(
                response
        );
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
                        user
                                .getKakaoUserProfile()
                                .id(),
                        request
                );

        return ApiResponse.onSuccess(
                response
        );
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<AuthTokenResponse>
    refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Cookie refreshTokenCookie =
                JwtCookieUtils.getCookie(
                        request,
                        JwtCookieUtils
                                .REFRESH_TOKEN_COOKIE_NAME
                );

        if (
                refreshTokenCookie
                        == null
        ) {
            throw new ProjectException(
                    ErrorCode.INVALID_TOKEN
            );
        }

        AuthTokenResponse tokenResponse =
                jwtAuthService.refresh(
                        refreshTokenCookie
                                .getValue()
                );

        jwtCookieService.addTokenCookies(
                response,
                tokenResponse
        );

        /*
         * 기존 refresh API의 Response 계약은
         * 이번 기능에서는 유지.
         * 최종 production hardening에서
         * Token Body 제거 여부를 프론트 사용 여부와
         * 함께 별도로 검토.
         */
        return ApiResponse.onSuccess(
                tokenResponse
        );
    }
}