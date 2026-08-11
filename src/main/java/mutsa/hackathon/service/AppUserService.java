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

    private final AppUserRepository
            appUserRepository;

    private final AiMemoryProfileService
            aiMemoryProfileService;

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
                    existingUser
                            .updateKakaoProfile(
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
    public MeResponse findMe(
            Long userId
    ) {
        return MeResponse.from(
                findUser(userId)
        );
    }

    @Transactional
    public MeResponse updateMe(
            Long userId,
            MeUpdateRequest request
    ) {
        AppUser user =
                findUser(userId);

        boolean requestedConsent =
                Boolean.TRUE.equals(
                        request.aiMemoryConsent()
                );

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

        /*
         * 동의가 켜져 있으면 현재 승인 기억으로
         * 프로필 일관성을 다시 맞춤.
         * 동의가 꺼지면 PENDING / APPROVED 기억을
         * 모두 REVOKED로 변경하고 캐시를 제거.
         */
        if (requestedConsent) {
            aiMemoryProfileService
                    .rebuildProfile(
                            userId
                    );
        } else {
            aiMemoryProfileService
                    .revokeAllUsableMemories(
                            userId
                    );
        }

        return MeResponse.from(
                user
        );
    }

    /**
     * 이메일/비밀번호 로그인 성공 시
     * 마지막 로그인 시각만 짧은 transaction에서 갱신
     */
    @Transactional
    public void recordLogin(
            Long userId
    ) {
        findUser(userId)
                .recordLogin();
    }

    @Transactional(readOnly = true)
    public KakaoUserProfile
    findProfileById(
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
                .findByRefreshToken(
                        refreshToken
                )
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
        findUser(userId)
                .updateRefreshToken(
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

    private AppUser findUser(
            Long userId
    ) {
        return appUserRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }
}