package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.dto.AuthTokenResponse;
import mutsa.hackathon.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtAuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserService appUserService;

    public JwtAuthService(JwtTokenProvider jwtTokenProvider, AppUserService appUserService) { this.jwtTokenProvider = jwtTokenProvider; this.appUserService = appUserService; }

    @Transactional
    public AuthTokenResponse issueTokens(KakaoUserProfile userProfile) {
        String accessToken = jwtTokenProvider.createAccessToken(userProfile);
        String refreshToken = jwtTokenProvider.createRefreshToken(userProfile);
        appUserService.updateRefreshToken(userProfile.id(), refreshToken, jwtTokenProvider.getRefreshTokenExpiry(refreshToken));
        return new AuthTokenResponse(accessToken, refreshToken, "Bearer", jwtTokenProvider.getAccessTokenExpiration(), jwtTokenProvider.getRefreshTokenExpiration());
    }
    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.isValidToken(refreshToken) || !"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))) throw new IllegalArgumentException("Invalid refresh token");
        AppUser user = appUserService.findByRefreshToken(refreshToken);
        KakaoUserProfile profile = KakaoUserProfile.from(user, false);
        String newAccessToken = jwtTokenProvider.createAccessToken(profile);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(profile);
        appUserService.updateRefreshToken(profile.id(), newRefreshToken, jwtTokenProvider.getRefreshTokenExpiry(newRefreshToken));
        return new AuthTokenResponse(newAccessToken, newRefreshToken, "Bearer", jwtTokenProvider.getAccessTokenExpiration(), jwtTokenProvider.getRefreshTokenExpiration());
    }
}