package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.dto.DiaryRewardResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Import(
        DevDatedDiaryServiceIntegrationTest
                .TestRewardGeneratorConfiguration.class
)
class DevDatedDiaryServiceIntegrationTest {

    private static final ZoneId SERVICE_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final Duration WAIT_TIMEOUT =
            Duration.ofSeconds(10);

    private static final Duration POLL_INTERVAL =
            Duration.ofMillis(100);

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private DiaryRewardService diaryRewardService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private AiQuestionRepository aiQuestionRepository;

    @Test
    void 지정한_날짜로_일기와_성찰질문을_저장하고_색보상을_생성한다()
            throws InterruptedException {
        AppUser user = saveUser();
        LocalDate recordedDate =
                LocalDate.now(SERVICE_ZONE)
                        .minusDays(30);

        DevDatedDiaryService service =
                serviceFor(user.getId(), 3650);

        DiaryCreateResponse created =
                service.create(
                        user.getId(),
                        recordedDate,
                        new DiaryCreateRequest(
                                "과거 날짜 일기와 경험조각 기능을 테스트했다.",
                                false
                        )
                );

        assertEquals(
                recordedDate,
                created.recordedDate()
        );

        Diary savedDiary =
                diaryRepository
                        .findById(created.diaryId())
                        .orElseThrow();

        assertEquals(
                recordedDate,
                savedDiary.getRecordedDate()
        );

        AiQuestion reflectionQuestion =
                aiQuestionRepository
                        .findByDiaryIdAndQuestionType(
                                created.diaryId(),
                                AiQuestionType.REFLECTION
                        )
                        .orElseThrow();

        assertEquals(
                recordedDate,
                reflectionQuestion.getAskedDate()
        );

        DiaryRewardResponse completedReward =
                waitUntilCompleted(
                        user.getId(),
                        created.diaryId()
                );

        assertEquals(
                "COMPLETED",
                completedReward.status()
        );
        assertEquals(
                "#73D8B4",
                completedReward.colorHex()
        );
        assertNotNull(
                completedReward.colorComment()
        );
    }

    @Test
    void 허용되지_않은_사용자는_날짜지정_일기를_작성할_수_없다() {
        AppUser user = saveUser();

        DevDatedDiaryService service =
                serviceFor(
                        user.getId() + 1,
                        3650
                );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () -> service.create(
                                user.getId(),
                                LocalDate.now(SERVICE_ZONE)
                                        .minusDays(1),
                                new DiaryCreateRequest(
                                        "허용되지 않은 요청",
                                        false
                                )
                        )
                );

        assertEquals(
                ErrorCode.ACCESS_DENIED,
                exception.getErrorCode()
        );
    }

    @Test
    void 미래_날짜의_일기는_작성할_수_없다() {
        AppUser user = saveUser();

        DevDatedDiaryService service =
                serviceFor(user.getId(), 3650);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        user.getId(),
                        LocalDate.now(SERVICE_ZONE)
                                .plusDays(1),
                        new DiaryCreateRequest(
                                "미래 날짜 일기",
                                false
                        )
                )
        );
    }

    @Test
    void 설정된_허용범위보다_오래된_일기는_작성할_수_없다() {
        AppUser user = saveUser();

        DevDatedDiaryService service =
                serviceFor(user.getId(), 30);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        user.getId(),
                        LocalDate.now(SERVICE_ZONE)
                                .minusDays(31),
                        new DiaryCreateRequest(
                                "허용 범위를 벗어난 일기",
                                false
                        )
                )
        );
    }

    private DevDatedDiaryService serviceFor(
            Long allowedUserId,
            long maxPastDays
    ) {
        return new DevDatedDiaryService(
                diaryService,
                allowedUserId,
                maxPastDays
        );
    }

    private DiaryRewardResponse waitUntilCompleted(
            Long userId,
            Long diaryId
    ) throws InterruptedException {
        Instant deadline =
                Instant.now()
                        .plus(WAIT_TIMEOUT);

        DiaryRewardResponse latest = null;

        while (Instant.now().isBefore(deadline)) {
            latest = diaryRewardService.getReward(
                    userId,
                    diaryId
            );

            if ("COMPLETED".equals(latest.status())) {
                return latest;
            }

            if ("FAILED".equals(latest.status())) {
                fail("날짜 지정 일기의 색 보상 생성이 실패했습니다.");
            }

            Thread.sleep(
                    POLL_INTERVAL.toMillis()
            );
        }

        fail(
                "제한 시간 안에 날짜 지정 일기의 색 보상이 완료되지 않았습니다."
        );

        throw new IllegalStateException(
                "도달할 수 없는 코드입니다."
        );
    }

    private AppUser saveUser() {
        return appUserRepository.save(
                AppUser.createKakaoUser(
                        "dated-diary-test-"
                                + System.nanoTime(),
                        "테스트 사용자",
                        null,
                        null
                )
        );
    }

    @TestConfiguration(
            proxyBeanMethods = false
    )
    static class TestRewardGeneratorConfiguration {

        @Bean
        @Primary
        DiaryColorRewardGenerator
        testDiaryColorRewardGenerator() {
            return diaryContent ->
                    new DiaryColorReward(
                            "#73D8B4",
                            List.of(
                                    "기록",
                                    "테스트"
                            ),
                            "기록 속 화면과 움직임을 반영해 밝고 선명한 색을 골랐습니다."
                    );
        }
    }
}
