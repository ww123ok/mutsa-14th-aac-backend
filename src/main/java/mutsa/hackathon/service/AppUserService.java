package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AppUserService {
    private final AppUserRepository appUserRepository;
    public AppUserService(AppUserRepository appUserRepository) { this.appUserRepository = appUserRepository; }

    @Transactional
    public KakaoUserProfile saveOrUpdate(String provider, String providerId, String nickname, String email, String profileImage) {
        return appUserRepository.findByProviderAndProviderId(provider, providerId).map(existingUser -> {
            existingUser.updateProfile(nickname, email, profileImage);
            return KakaoUserProfile.from(existingUser, false);
        }).orElseGet(() -> KakaoUserProfile.from(appUserRepository.save(new AppUser(provider, providerId, nickname, email, profileImage)), true));
    }
    @Transactional(readOnly = true)
    public KakaoUserProfile findProfileById(Long userId) {
        return KakaoUserProfile.from(appUserRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found")), false);
    }
    @Transactional(readOnly = true)
    public AppUser findByRefreshToken(String refreshToken) {
        return appUserRepository.findByRefreshToken(refreshToken).orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
    }
    @Transactional
    public void updateRefreshToken(Long userId, String refreshToken, LocalDateTime expiresAt) {
        appUserRepository.findById(userId).orElseThrow().updateRefreshToken(refreshToken, expiresAt);
    }
    @Transactional
    public void clearRefreshToken(Long userId) {
        appUserRepository.findById(userId).ifPresent(AppUser::clearRefreshToken);
    }
}