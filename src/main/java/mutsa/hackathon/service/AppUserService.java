package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.KakaoUserProfile;
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
    public KakaoUserProfile saveOrUpdate(String provider, String providerId, String nickname, String email, String profileImage) {
        return appUserRepository.findByProviderAndProviderId(provider, providerId).map(existingUser -> {
            existingUser.updateProfile(nickname, email, profileImage);
            return KakaoUserProfile.from(existingUser, false);
        }).orElseGet(() -> KakaoUserProfile.from(appUserRepository.save(new AppUser(provider, providerId, nickname, email, profileImage)), true));
    }

    @Transactional(readOnly = true)
    public KakaoUserProfile findProfileById(Long userId) {
        return KakaoUserProfile.from(appUserRepository.findById(userId).orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND)), false);
    }

    @Transactional(readOnly = true)
    public AppUser findByRefreshToken(String refreshToken) {
        return appUserRepository.findByRefreshToken(refreshToken).orElseThrow(() -> new ProjectException(ErrorCode.INVALID_TOKEN));
    }

    @Transactional
    public void updateRefreshToken(Long userId, String refreshToken, LocalDateTime expiresAt) {
        appUserRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND)) // 명확한 커스텀 예외 지정
                .updateRefreshToken(refreshToken, expiresAt);
    }

    @Transactional
    public void clearRefreshToken(Long userId) {
        appUserRepository.findById(userId).ifPresent(AppUser::clearRefreshToken);
    }
}