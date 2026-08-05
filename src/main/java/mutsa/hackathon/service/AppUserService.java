package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.dto.MeResponse;
import mutsa.hackathon.dto.MeUpdateRequest;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;

    @Transactional
    public KakaoUserProfile saveOrUpdate(
            String provider,
            String providerId,
            String nickname,
            String email,
            String profileImage
    ) {
        return appUserRepository
                .findByProviderAndProviderId(
                        provider,
                        providerId
                )
                .map(existingUser -> {
                    existingUser.updateKakaoProfile(
                            nickname,
                            email,
                            profileImage
                    );

                    return KakaoUserProfile.from(
                            existingUser,
                            false
                    );
                })
                .orElseGet(() -> {
                    AppUser newUser =
                            AppUser.createKakaoUser(
                                    providerId,
                                    nickname,
                                    email,
                                    profileImage
                            );

                    AppUser savedUser =
                            appUserRepository.save(
                                    newUser
                            );

                    return KakaoUserProfile.from(
                            savedUser,
                            true
                    );
                });
    }

    @Transactional(readOnly = true)
    public MeResponse findMe(Long userId) {
        return MeResponse.from(
                findUser(userId)
        );
    }

    @Transactional
    public MeResponse updateMe(
            Long userId,
            MeUpdateRequest request
    ) {
        AppUser user = findUser(userId);

        boolean requestedConsent =
                Boolean.TRUE.equals(
                        request.aiMemoryConsent()
                );

        /*
         * 최초 온보딩 화면에는 동의하기 버튼만 있으므로
         * 온보딩 최초 완료에는 동의를 필수로 처리함.
         *
         * 이미 온보딩을 완료한 사용자는 설정 화면에서
         * false를 보내 동의를 철회할 수 있음.
         */
        if (
                !user.isOnboardingCompleted()
                        && !requestedConsent
        ) {
            throw new ProjectException(
                    ErrorCode
                            .ONBOARDING_CONSENT_REQUIRED
            );
        }

        user.updatePersonalSettings(
                request.nickname(),
                request.job(),
                request.reminderTime(),
                requestedConsent
        );

        return MeResponse.from(user);
    }

    /**
     * JWT 인증 객체 생성에 사용하는 내부 프로필.
     * 외부 API 응답으로 직접 반환하지 않음.
     */
    @Transactional(readOnly = true)
    public KakaoUserProfile findProfileById(
            Long userId
    ) {
        return KakaoUserProfile.from(
                findUser(userId),
                false
        );
    }

    @Transactional(readOnly = true)
    public AppUser findByRefreshToken(
            String refreshToken
    ) {
        return appUserRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.INVALID_TOKEN
                        )
                );
    }

    @Transactional
    public void updateRefreshToken(
            Long userId,
            String refreshToken,
            LocalDateTime expiresAt
    ) {
        findUser(userId).updateRefreshToken(
                refreshToken,
                expiresAt
        );
    }

    @Transactional
    public void clearRefreshToken(
            Long userId
    ) {
        appUserRepository
                .findById(userId)
                .ifPresent(
                        AppUser::clearRefreshToken
                );
    }

    private AppUser findUser(Long userId) {
        return appUserRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }
}