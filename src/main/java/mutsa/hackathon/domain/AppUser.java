package mutsa.hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name =
                                "uk_app_user_provider_provider_id",
                        columnNames = {
                                "provider",
                                "provider_id"
                        }
                )
        }
)
public class AppUser extends BaseEntity {

    private static final int
            NICKNAME_MIN_LENGTH = 2;

    private static final int
            NICKNAME_MAX_LENGTH = 8;

    private static final int
            JOB_MAX_LENGTH = 30;

    private static final int
            LOCAL_EMAIL_MAX_LENGTH = 100;

    private static final String
            LOCAL_PROVIDER = "local";

    /*
     * 일반 회원가입 직후에는 기존 DAYBIT 온보딩에서
     * 사용자가 실제 닉네임을 선택.
     * AppUser.nickname은 현재 nullable=false이므로
     * 온보딩 전까지 사용할 내부 임시값을 둠.
     */
    private static final String
            LOCAL_INITIAL_NICKNAME = "데이빗";

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(
            nullable = false,
            length = 30
    )
    private String provider;

    @Column(
            name = "provider_id",
            nullable = false,
            length = 100
    )
    private String providerId;

    @Column(
            nullable = false,
            length = 100
    )
    private String nickname;

    @Column(length = 150)
    private String email;

    /*
     * Kakao 사용자는 null.
     * 일반 계정에서만 PasswordEncoder로 생성한
     * 단방향 password hash를 저장.
     * 원본 비밀번호는 절대 저장하지 않음.
     */
    @Column(
            name = "password_hash",
            length = 255
    )
    private String passwordHash;

    @Column(
            name = "profile_image",
            length = 500
    )
    private String profileImage;

    @Column(length = JOB_MAX_LENGTH)
    private String job;

    @Column(name = "diary_reminder_time")
    private LocalTime diaryReminderTime;

    @Column(
            name = "ai_memory_consent",
            nullable = false
    )
    private boolean aiMemoryConsent;

    @Column(name = "ai_memory_consented_at")
    private LocalDateTime aiMemoryConsentedAt;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;

    @Column(nullable = false)
    private int credit;

    @Column(
            name = "ai_memory_profile",
            columnDefinition = "TEXT"
    )
    private String aiMemoryProfile;

    @Column(
            name = "last_login_at",
            nullable = false
    )
    private LocalDateTime lastLoginAt;

    @Column(
            name = "refresh_token",
            length = 1000
    )
    private String refreshToken;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    public static AppUser createKakaoUser(
            String providerId,
            String nickname,
            String email,
            String profileImage
    ) {
        return AppUser.builder()
                .version(0L)
                .provider("kakao")
                .providerId(
                        normalizeRequired(
                                providerId,
                                "카카오 사용자 식별자는 필수입니다."
                        )
                )
                .nickname(
                        normalizeInitialNickname(
                                nickname
                        )
                )
                .email(
                        normalizeOptional(
                                email
                        )
                )
                .profileImage(
                        normalizeOptional(
                                profileImage
                        )
                )
                .aiMemoryConsent(false)
                .credit(0)
                .lastLoginAt(
                        LocalDateTime.now()
                )
                .build();
    }

    /**
     * 이메일/비밀번호 기반 일반 사용자를 생성.
     * email:
     * 로그인 식별자로 사용하므로 소문자로 정규화.
     * passwordHash:
     * PasswordEncoder를 이미 통과한 값만 받음.
     */
    public static AppUser createLocalUser(
            String email,
            String passwordHash
    ) {
        String normalizedEmail =
                normalizeEmail(email);

        return AppUser.builder()
                .version(0L)
                .provider(
                        LOCAL_PROVIDER
                )
                .providerId(
                        normalizedEmail
                )
                .nickname(
                        LOCAL_INITIAL_NICKNAME
                )
                .email(
                        normalizedEmail
                )
                .passwordHash(
                        normalizeRequired(
                                passwordHash,
                                "암호화된 비밀번호는 필수입니다."
                        )
                )
                .aiMemoryConsent(false)
                .credit(0)
                .lastLoginAt(
                        LocalDateTime.now()
                )
                .build();
    }

    public void updateKakaoProfile(
            String kakaoNickname,
            String email,
            String profileImage
    ) {
        if (!isOnboardingCompleted()) {
            this.nickname =
                    normalizeInitialNickname(
                            kakaoNickname
                    );
        }

        if (email != null) {
            this.email =
                    normalizeOptional(
                            email
                    );
        }

        if (profileImage != null) {
            this.profileImage =
                    normalizeOptional(
                            profileImage
                    );
        }

        this.lastLoginAt =
                LocalDateTime.now();
    }

    public void updatePersonalSettings(
            String nickname,
            String job,
            LocalTime diaryReminderTime,
            boolean aiMemoryConsent
    ) {
        this.nickname =
                normalizeNickname(
                        nickname
                );

        this.job =
                normalizeJob(
                        job
                );

        if (diaryReminderTime == null) {
            throw new IllegalArgumentException(
                    "일기 알림 시간은 필수입니다."
            );
        }

        this.diaryReminderTime =
                diaryReminderTime;

        updateAiMemoryConsent(
                aiMemoryConsent
        );

        if (
                this.onboardingCompletedAt
                        == null
        ) {
            this.onboardingCompletedAt =
                    LocalDateTime.now();
        }
    }

    public void addCredit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "지급할 크레딧은 1 이상이어야 합니다."
            );
        }

        this.credit += amount;
    }

    public void useCredit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "사용할 크레딧은 1 이상이어야 합니다."
            );
        }

        if (this.credit < amount) {
            throw new IllegalStateException(
                    "크레딧이 부족합니다."
            );
        }

        this.credit -= amount;
    }

    public void updateAiMemoryProfile(
            String aiMemoryProfile
    ) {
        if (!this.aiMemoryConsent) {
            throw new IllegalStateException(
                    "AI 기억 활용에 동의한 사용자만 기억 정보를 저장할 수 있습니다."
            );
        }

        this.aiMemoryProfile =
                normalizeOptional(
                        aiMemoryProfile
                );
    }

    /**
     * 파생 캐시만 제거.
     * user_memory_item 원본 상태 변경은
     * AiMemoryProfileService에서 담당.
     */
    public void clearAiMemoryProfile() {
        this.aiMemoryProfile = null;
    }

    public void updateRefreshToken(
            String refreshToken,
            LocalDateTime
                    refreshTokenExpiresAt
    ) {
        this.refreshToken =
                refreshToken;

        this.refreshTokenExpiresAt =
                refreshTokenExpiresAt;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpiresAt = null;
    }

    /**
     * 이메일/비밀번호 로그인처럼
     * 프로필 정보 갱신이 없는 로그인에서도
     * 마지막 로그인 시각을 갱신
     */
    public void recordLogin() {
        this.lastLoginAt =
                LocalDateTime.now();
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompletedAt != null;
    }

    private void updateAiMemoryConsent(
            boolean consent
    ) {
        if (consent) {
            if (!this.aiMemoryConsent) {
                this.aiMemoryConsentedAt =
                        LocalDateTime.now();
            }

            this.aiMemoryConsent = true;
            return;
        }

        this.aiMemoryConsent = false;
        this.aiMemoryConsentedAt = null;
        this.aiMemoryProfile = null;
    }

    private static String
    normalizeInitialNickname(
            String value
    ) {
        return normalizeRequired(
                value,
                "닉네임은 필수입니다."
        );
    }

    private static String
    normalizeNickname(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "닉네임은 필수입니다."
                );

        if (
                normalized.length()
                        < NICKNAME_MIN_LENGTH
                        || normalized.length()
                        > NICKNAME_MAX_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "닉네임은 2자 이상 8자 이하로 입력해야 합니다."
            );
        }

        return normalized;
    }

    private static String normalizeJob(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "현재 하는 일은 필수입니다."
                );

        if (
                normalized.length()
                        > JOB_MAX_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "현재 하는 일은 30자 이하로 입력해야 합니다."
            );
        }

        return normalized;
    }

    private static String normalizeEmail(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "이메일은 필수입니다."
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                normalized.length()
                        > LOCAL_EMAIL_MAX_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "이메일은 100자 이하로 입력해야 합니다."
            );
        }

        return normalized;
    }

    private static String
    normalizeRequired(
            String value,
            String message
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                    message
            );
        }

        return value.trim();
    }

    private static String
    normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}