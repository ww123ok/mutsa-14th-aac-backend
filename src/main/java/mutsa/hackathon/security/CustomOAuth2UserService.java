package mutsa.hackathon.security;

import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.service.AppUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final AppUserService appUserService;

    public CustomOAuth2UserService(AppUserService appUserService) { this.appUserService = appUserService; }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        KakaoUserProfile kakaoUserProfile = extractAndSaveKakaoUser(oauth2User.getAttributes());
        return new CustomOAuth2User(oauth2User.getAuthorities(), oauth2User.getAttributes(), userNameAttributeName, kakaoUserProfile);
    }

    @SuppressWarnings("unchecked")
    private KakaoUserProfile extractAndSaveKakaoUser(Map<String, Object> attributes) {
        Map<String, Object> properties = (Map<String, Object>) attributes.getOrDefault("properties", Map.of());
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());

        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        return appUserService.saveOrUpdate("kakao", String.valueOf(attributes.get("id")), (String) properties.get("nickname"), email, (String) properties.get("profile_image"));
    }
}