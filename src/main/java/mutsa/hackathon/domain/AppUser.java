package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "app_user")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30) private String provider;
    @Column(nullable = false, unique = true, length = 100) private String providerId;
    @Column(nullable = false, length = 100) private String nickname;
    @Column(length = 150) private String email;
    @Column(length = 500) private String profileImage;
    @Column(nullable = false) private LocalDateTime lastLoginAt;
    @Column(length = 1000) private String refreshToken;
    private LocalDateTime refreshTokenExpiresAt;

    public AppUser(String provider, String providerId, String nickname, String email, String profileImage) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.email = email;
        this.profileImage = profileImage;
        this.lastLoginAt = LocalDateTime.now();
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