package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailPasswordAuthService {

    private static final String
            LOCAL_PROVIDER = "local";

    private static final int
            EMAIL_MAX_LENGTH = 100;

    private static final int
            PASSWORD_MIN_LENGTH = 8;

    private static final int
            PASSWORD_MAX_LENGTH = 64;

    private final AppUserRepository
            appUserRepository;

    private final PasswordEncoder
            passwordEncoder;

    private final AppUserService
            appUserService;

    /**
     * 회원가입.
     * 이 메서드에는 의도적으로
     * service-level @Transactional을 붙이지 않음.
     * PasswordEncoder의 adaptive hashing은
     * 상대적으로 비용이 큰 CPU 작업이기 때문에
     * 비밀번호를 해시하는 동안 DB transaction과
     * connection을 잡고 있지 않도록 함.
     */
    public Long register(
            String email,
            String rawPassword
    ) {
        String normalizedEmail =
                normalizeEmail(
                        email
                );

        validateRegistrationPassword(
                rawPassword
        );

        if (
                appUserRepository
                        .existsByProviderAndProviderId(
                                LOCAL_PROVIDER,
                                normalizedEmail
                        )
        ) {
            throw new ProjectException(
                    ErrorCode
                            .EMAIL_ALREADY_REGISTERED
            );
        }

        /*
         * 평문 비밀번호는 이 순간에만 사용하며
         * DB에는 이 hash만 저장
         */
        String passwordHash =
                passwordEncoder.encode(
                        rawPassword
                );

        AppUser localUser =
                AppUser.createLocalUser(
                        normalizedEmail,
                        passwordHash
                );

        try {
            return appUserRepository
                    .saveAndFlush(
                            localUser
                    )
                    .getId();

        } catch (
                DataIntegrityViolationException
                        exception
        ) {
            /*
             * exists 확인과 INSERT 사이에
             * 두 가입 요청이 동시에 들어오면
             * DB unique constraint가 마지막으로 방어
             */
            throw new ProjectException(
                    ErrorCode
                            .EMAIL_ALREADY_REGISTERED
            );
        }
    }

    /**
     * 이메일/비밀번호 인증.
     * 존재하지 않는 이메일과 잘못된 비밀번호를
     * 동일한 인증 실패로 처리.
     */
    public Long authenticate(
            String email,
            String rawPassword
    ) {
        String normalizedEmail =
                normalizeEmail(
                        email
                );

        if (
                rawPassword == null
                        || rawPassword.isBlank()
        ) {
            throw invalidCredentials();
        }

        AppUser user =
                appUserRepository
                        .findByProviderAndProviderId(
                                LOCAL_PROVIDER,
                                normalizedEmail
                        )
                        .orElseThrow(
                                this::invalidCredentials
                        );

        if (
                user.getPasswordHash()
                        == null
                        || !passwordEncoder
                        .matches(
                                rawPassword,
                                user.getPasswordHash()
                        )
        ) {
            throw invalidCredentials();
        }

        /*
         * password 검증이 끝난 뒤
         * 마지막 로그인 시각만 별도의 짧은 transaction에서 갱신.
         */
        appUserService.recordLogin(
                user.getId()
        );

        return user.getId();
    }

    private String normalizeEmail(
            String email
    ) {
        if (
                email == null
                        || email.isBlank()
        ) {
            throw new ProjectException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        String normalized =
                email.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                normalized.length()
                        > EMAIL_MAX_LENGTH
        ) {
            throw new ProjectException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        return normalized;
    }

    private void
    validateRegistrationPassword(
            String rawPassword
    ) {
        if (
                rawPassword == null
                        || rawPassword.length()
                        < PASSWORD_MIN_LENGTH
                        || rawPassword.length()
                        > PASSWORD_MAX_LENGTH
        ) {
            throw new ProjectException(
                    ErrorCode.INVALID_REQUEST
            );
        }
    }

    private ProjectException
    invalidCredentials() {
        return new ProjectException(
                ErrorCode.INVALID_CREDENTIALS
        );
    }
}