package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_app_user_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
public class AppUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(length = 150)
    private String email;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(length = 100)
    private String job;

    @Column(nullable = false)
    private int credit;

    @Column(name = "ai_memory_profile", columnDefinition = "TEXT")
    private String aiMemoryProfile;

    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    public static AppUser createKakaoUser(
            String providerId,
            String nickname,
            String email,
            String profileImage
    ) {
        validateRequired(providerId, "카카오 사용자 식별자는 필수입니다.");
        validateRequired(nickname, "닉네임은 필수입니다.");

        return AppUser.builder()
                .version(0L)
                .provider("kakao")
                .providerId(providerId)
                .nickname(nickname)
                .email(email)
                .profileImage(profileImage)
                .credit(0)
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    public void updateProfile(String nickname, String email, String profileImage) {
        validateRequired(nickname, "닉네임은 필수입니다.");

        this.nickname = nickname;
        if (email != null) {
            this.email = email;
        }
        this.profileImage = profileImage;
        this.lastLoginAt = LocalDateTime.now();
    }

    public void completeOnboarding(String nickname, String job) {
        validateRequired(nickname, "닉네임은 필수입니다.");
        validateRequired(job, "직업 정보는 필수입니다.");

        this.nickname = nickname;
        this.job = job;
    }

    public void addCredit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("지급할 크레딧은 1 이상이어야 합니다.");
        }
        this.credit += amount;
    }

    public void useCredit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용할 크레딧은 1 이상이어야 합니다.");
        }
        if (this.credit < amount) {
            throw new IllegalStateException("크레딧이 부족합니다.");
        }
        this.credit -= amount;
    }

    public void updateAiMemoryProfile(String aiMemoryProfile) {
        this.aiMemoryProfile = aiMemoryProfile;
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime refreshTokenExpiresAt) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpiresAt = null;
    }

    public boolean isOnboardingCompleted() {
        return job != null && !job.isBlank();
    }

    private static void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}