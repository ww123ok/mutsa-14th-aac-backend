package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.dto.DiaryRewardResponse;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
    private DiaryRewardRepository
            diaryRewardRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void 일기를_생성하면_비동기로_HEX와_키워드_보상이_COMPLETED가_된다()
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
                List.of(
                        "해결",
                        "성취",
                        "안도"
                ),
                completedReward.keywords()
        );

        assertNotNull(
                completedReward.colorHex()
        );

        assertEquals(
                "밝은 화면과 오류 해결 장면이 이어져 밝고 선명한 방향의 색을 골랐습니다.",
                completedReward.colorComment()
        );

        DiaryReward savedReward =
                diaryRewardRepository
                        .findByDiaryId(
                                created.diaryId()
                        )
                        .orElseThrow();

        assertEquals(
                "해결",
                savedReward.getKeyword1()
        );

        assertEquals(
                "성취",
                savedReward.getKeyword2()
        );

        assertEquals(
                "안도",
                savedReward.getKeyword3()
        );

        assertEquals(
                completedReward.keywords(),
                savedReward.getKeywords()
        );

        assertEquals(
                completedReward.colorComment(),
                savedReward.getColorComment()
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

        @Bean
        @Primary
        DiaryColorRewardGenerator
        testDiaryColorRewardGenerator() {

            return diaryContent ->
                    new DiaryColorReward(
                            "#73D8B4",
                            List.of(
                                    "해결",
                                    "성취",
                                    "안도"
                            ),
                            "밝은 화면과 오류 해결 장면이 이어져 밝고 선명한 방향의 색을 골랐습니다."
                    );
        }
    }
}