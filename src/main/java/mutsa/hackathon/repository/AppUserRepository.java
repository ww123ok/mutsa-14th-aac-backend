package mutsa.hackathon.repository;

import mutsa.hackathon.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository
        extends JpaRepository<AppUser, Long> {

    Optional<AppUser>
    findByProviderAndProviderId(
            String provider,
            String providerId
    );

    Optional<AppUser>
    findByRefreshToken(
            String refreshToken
    );

    boolean existsByProviderAndProviderId(
            String provider,
            String providerId
    );

    /**
     * 사용자별 주간 보상 생성 시각을 정확히 맞추기 위해
     * 특정 DAYBIT 시작 시간을 가진 사용자만 조회
     */
    List<AppUser> findAllByDayStartTime(
            LocalTime dayStartTime
    );

    /**
     * 매분 실행되는 일기 작성 알림 스케줄러가
     * 현재 알림 시각에 해당하는 사용자만 조회.
     */
    List<AppUser> findAllByDiaryReminderTime(
            LocalTime diaryReminderTime
    );

    /**
     * day_start_time 컬럼 추가 이전 기존 사용자는 null일 수 있음.
     * 애플리케이션에서는 이 값을 00:00으로 해석하므로
     * 자정 경계 스케줄에서도 함께 포함.
     */
    List<AppUser> findAllByDayStartTimeIsNull();
}
