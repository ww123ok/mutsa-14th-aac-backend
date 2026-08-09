package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.dto.DiaryRewardResponse;
import mutsa.hackathon.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Import(
        DiaryRewardAsyncFlowIntegrationTest
                .TestRewardGeneratorConfiguration.class
)
class DiaryRewardAsyncFlowIntegrationTest {

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

    @Test
    void 일기를_생성하면_비동기로_색_보상이_COMPLETED가_된다()
            throws InterruptedException {

        AppUser user = saveUser();

        DiaryCreateResponse created =
                diaryService.create(
                        user.getId(),
                        new DiaryCreateRequest(
                                """
                                오늘 팀원들과 오류를 하나씩 해결했다.
                                테스트가 모두 성공해서 뿌듯했고,
                                마음이 한결 편안해졌다.
                                """,
                                true
                        )
                );

        /*
         * 일기 생성 응답 시점에는 비동기 작업이 이미 끝났을 수도 있고
         * 아직 PENDING일 수도 있으므로 최종 상태를 폴링.
         */
        DiaryRewardResponse completedReward =
                waitUntilCompleted(
                        user.getId(),
                        created.diaryId()
                );

        assertEquals(
                created.diaryId(),
                completedReward.diaryId()
        );

        assertEquals(
                "COMPLETED",
                completedReward.status()
        );

        assertEquals(
                "#73D8B4",
                completedReward.colorHex()
        );

        assertEquals(
                "포근한 민트빛",
                completedReward.colorName()
        );

        assertNotNull(
                completedReward.colorHex()
        );

        assertNotNull(
                completedReward.colorName()
        );
    }

    private DiaryRewardResponse waitUntilCompleted(
            Long userId,
            Long diaryId
    ) throws InterruptedException {

        Instant deadline =
                Instant.now()
                        .plus(WAIT_TIMEOUT);

        DiaryRewardResponse latestResponse =
                null;

        while (Instant.now().isBefore(deadline)) {
            latestResponse =
                    diaryRewardService.getReward(
                            userId,
                            diaryId
                    );

            if (
                    "COMPLETED".equals(
                            latestResponse.status()
                    )
            ) {
                return latestResponse;
            }

            if (
                    "FAILED".equals(
                            latestResponse.status()
                    )
            ) {
                fail(
                        "색 보상 비동기 생성이 FAILED 상태가 되었습니다."
                );
            }

            Thread.sleep(
                    POLL_INTERVAL.toMillis()
            );
        }

        String latestStatus =
                latestResponse == null
                        ? "조회되지 않음"
                        : latestResponse.status();

        fail(
                "제한 시간 안에 색 보상이 완료되지 않았습니다. "
                        + "최종 상태: "
                        + latestStatus
        );

        /*
         * fail()이 항상 예외를 발생시키므로
         * 실제로 도달하지 않는 코드입니다.
         */
        throw new IllegalStateException(
                "도달할 수 없는 코드입니다."
        );
    }

    private AppUser saveUser() {
        return appUserRepository.save(
                AppUser.createKakaoUser(
                        "async-reward-flow-"
                                + System.nanoTime(),
                        "데이빗",
                        null,
                        null
                )
        );
    }

    @TestConfiguration(
            proxyBeanMethods = false
    )
    static class TestRewardGeneratorConfiguration {

        /**
         * 운영용 또는 fallback 생성기보다 우선해서
         * 이 테스트의 고정 결과 생성기를 주입
         */
        @Bean
        @Primary
        DiaryColorRewardGenerator
        testDiaryColorRewardGenerator() {

            return diaryContent ->
                    new DiaryColorReward(
                            "#73D8B4",
                            "포근한 민트빛"
                    );
        }
    }
}