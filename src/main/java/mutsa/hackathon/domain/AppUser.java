package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🚨 무분별한 객체 생성 방지 (멋사 규칙)
@AllArgsConstructor(access = AccessLevel.PRIVATE)  // 🚨 Builder를 위해 필요하지만 외부에는 닫음
@Builder(access = AccessLevel.PRIVATE)             // 🚨 Builder를 외부 서비스에서 직접 쓰지 못하게 은닉 (멋사 규칙)
@Table(name = "app_user")
public class AppUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, unique = true, length = 100)
    private String providerId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(length = 150)
    private String email;

    @Column(length = 500)
    private String profileImage;

    @Column(nullable = false)
    private LocalDateTime lastLoginAt;

    @Column(length = 1000)
    private String refreshToken;

    private LocalDateTime refreshTokenExpiresAt;

    public static AppUser createKakaoUser(String providerId, String nickname, String email, String profileImage) {
        return AppUser.builder()
                .provider("kakao")
                .providerId(providerId)
                .nickname(nickname)
                .email(email)
                .profileImage(profileImage)
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    public void updateProfile(String nickname, String email, String profileImage) {
        this.nickname = nickname;
        if (email != null) this.email = email;
        this.profileImage = profileImage;
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime refreshTokenExpiresAt) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpiresAt = null;
    }
}