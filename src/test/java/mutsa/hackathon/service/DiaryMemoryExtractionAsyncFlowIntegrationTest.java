package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(
        properties = {
                "app.openai.memory-extraction-enabled=true",
                "app.openai.reflection-enabled=false",
                "app.openai.reward-enabled=false"
        }
)
@Import(
        DiaryMemoryExtractionAsyncFlowIntegrationTest
                .TestMemoryExtractorConfiguration.class
)
class DiaryMemoryExtractionAsyncFlowIntegrationTest {

    private static final Duration
            WAIT_TIMEOUT =
            Duration.ofSeconds(10);

    private static final Duration
            POLL_INTERVAL =
            Duration.ofMillis(100);

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private AppUserRepository
            appUserRepository;

    @Autowired
    private DiaryRepository
            diaryRepository;

    @Autowired
    private UserMemoryItemRepository
            userMemoryItemRepository;

    @Test
    void 일기개인화에_동의하면_비동기로_기억을_추출하고_승인프로필에_반영한다()
            throws InterruptedException {

        AppUser user =
                saveUser(
                        true
                );

        DiaryCreateResponse created =
                diaryService.create(
                        user.getId(),
                        new DiaryCreateRequest(
                                """
                                오늘 프로젝트를 진행하고 집에 돌아와
                                반려묘와 시간을 보냈다.
                                최근 프로젝트 마감이 조금 신경 쓰인다.
                                """,
                                true
                        )
                );

        Diary processedDiary =
                waitUntilMemoryApplied(
                        created.diaryId()
                );

        assertNotNull(
                processedDiary
                        .getMemoryAppliedAt()
        );

        List<UserMemoryItem> memories =
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                user.getId(),
                                created.diaryId()
                        );

        assertEquals(
                3,
                memories.size()
        );

        assertTrue(
                memories.stream()
                        .allMatch(memory ->
                                memory.getStatus()
                                        == UserMemoryStatus.APPROVED
                        )
        );

        UserMemoryItem petMemory =
                memories.stream()
                        .filter(memory ->
                                memory.getCategory()
                                        == UserMemoryCategory.PET
                        )
                        .findFirst()
                        .orElseThrow();

        UserMemoryItem hobbyMemory =
                memories.stream()
                        .filter(memory ->
                                memory.getCategory()
                                        == UserMemoryCategory.HOBBY
                        )
                        .findFirst()
                        .orElseThrow();

        UserMemoryItem concernMemory =
                memories.stream()
                        .filter(memory ->
                                memory.getCategory()
                                        == UserMemoryCategory.CONCERN
                        )
                        .findFirst()
                        .orElseThrow();

        assertNull(
                petMemory.getExpiresAt()
        );

        assertNull(
                hobbyMemory.getExpiresAt()
        );

        assertNotNull(
                concernMemory.getExpiresAt()
        );

        AppUser savedUser =
                appUserRepository
                        .findById(
                                user.getId()
                        )
                        .orElseThrow();

        assertNotNull(
                savedUser.getAiMemoryProfile()
        );

        assertTrue(
                savedUser
                        .getAiMemoryProfile()
                        .contains(
                                "반려묘와 함께 생활함"
                        )
        );

        assertTrue(
                savedUser
                        .getAiMemoryProfile()
                        .contains(
                                "러닝을 취미로 즐김"
                        )
        );

        assertTrue(
                savedUser
                        .getAiMemoryProfile()
                        .contains(
                                "최근 팀 프로젝트 마감을 준비하고 있음"
                        )
        );
    }

    @Test
    void 오늘일기의_개인화반영을_거부하면_기억추출을_시작하지_않는다() {

        AppUser user =
                saveUser(
                        true
                );

        DiaryCreateResponse created =
                diaryService.create(
                        user.getId(),
                        new DiaryCreateRequest(
                                "오늘 반려묘와 시간을 보냈다.",
                                false
                        )
                );

        Diary diary =
                diaryRepository
                        .findById(
                                created.diaryId()
                        )
                        .orElseThrow();

        assertNull(
                diary.getMemoryAppliedAt()
        );

        assertTrue(
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                user.getId(),
                                created.diaryId()
                        )
                        .isEmpty()
        );
    }

    @Test
    void 전역AI기억동의가_없으면_일기개인화선택이_true여도_기억추출하지_않는다() {

        AppUser user =
                saveUser(
                        false
                );

        DiaryCreateResponse created =
                diaryService.create(
                        user.getId(),
                        new DiaryCreateRequest(
                                "오늘 반려묘와 시간을 보냈다.",
                                true
                        )
                );

        Diary diary =
                diaryRepository
                        .findById(
                                created.diaryId()
                        )
                        .orElseThrow();

        assertNull(
                diary.getMemoryAppliedAt()
        );

        assertTrue(
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                user.getId(),
                                created.diaryId()
                        )
                        .isEmpty()
        );
    }

    private Diary waitUntilMemoryApplied(
            Long diaryId
    ) throws InterruptedException {

        Instant deadline =
                Instant.now()
                        .plus(
                                WAIT_TIMEOUT
                        );

        while (
                Instant.now()
                        .isBefore(
                                deadline
                        )
        ) {
            Diary diary =
                    diaryRepository
                            .findById(
                                    diaryId
                            )
                            .orElseThrow();

            if (
                    diary.getMemoryAppliedAt()
                            != null
            ) {
                return diary;
            }

            Thread.sleep(
                    POLL_INTERVAL.toMillis()
            );
        }

        fail(
                "제한 시간 안에 개인화 기억 반영이 완료되지 않았습니다."
        );

        throw new IllegalStateException(
                "도달할 수 없는 코드입니다."
        );
    }

    private AppUser saveUser(
            boolean aiMemoryConsent
    ) {
        AppUser user =
                AppUser.createKakaoUser(
                        "memory-async-"
                                + System.nanoTime(),
                        "데이빗",
                        null,
                        null
                );

        user.updatePersonalSettings(
                "데이빗",
                "대학생",
                LocalTime.of(
                        21,
                        0
                ),
                aiMemoryConsent
        );

        return appUserRepository
                .saveAndFlush(
                        user
                );
    }

    @TestConfiguration(
            proxyBeanMethods = false
    )
    static class
    TestMemoryExtractorConfiguration {

        @Bean
        @Primary
        DiaryMemoryCandidateExtractor
        testDiaryMemoryCandidateExtractor() {

            return diaryContent ->
                    List.of(
                            new DiaryMemoryCandidate(
                                    UserMemoryCategory.PET,
                                    "반려묘와 함께 생활함"
                            ),

                            new DiaryMemoryCandidate(
                                    UserMemoryCategory.HOBBY,
                                    "러닝을 취미로 즐김"
                            ),

                            new DiaryMemoryCandidate(
                                    UserMemoryCategory.CONCERN,
                                    "최근 팀 프로젝트 마감을 준비하고 있음"
                            )
                    );
        }
    }
}