package mutsa.hackathon.security;

import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.service.AppUserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class JwtPrincipalFactory {
    private final AppUserService appUserService;
    public JwtPrincipalFactory(AppUserService appUserService) { this.appUserService = appUserService; }
    public CustomOAuth2User create(Long userId) {
        KakaoUserProfile profile = appUserService.findProfileById(userId);
        return new CustomOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("id", profile.id()), "id", profile);
    }
}