package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DiaryMemoryCandidatePersistenceServiceIntegrationTest {

    @Autowired
    private DiaryMemoryCandidatePersistenceService
            persistenceService;

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
    void 안정적인_기억은_만료없이_저장되고_최근맥락은_7일뒤_만료된다() {

        TestContext context =
                saveContext(true);

        LocalDateTime beforeSave =
                LocalDateTime.now();

        int savedCount =
                persistenceService.saveCandidates(
                        context.diary().getId(),
                        List.of(
                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.PET,
                                        "반려묘와 함께 생활함"
                                ),

                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.CONCERN,
                                        "최근 팀 프로젝트 마감에 대한 고민이 이어지고 있음"
                                )
                        )
                );

        assertEquals(
                2,
                savedCount
        );

        List<UserMemoryItem> memories =
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                context.user().getId(),
                                context.diary().getId()
                        );

        assertEquals(
                2,
                memories.size()
        );

        UserMemoryItem stable =
                memories.stream()
                        .filter(memory ->
                                memory.getCategory()
                                        == UserMemoryCategory.PET
                        )
                        .findFirst()
                        .orElseThrow();

        UserMemoryItem recent =
                memories.stream()
                        .filter(memory ->
                                memory.getCategory()
                                        == UserMemoryCategory.CONCERN
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                UserMemoryStatus.PENDING,
                stable.getStatus()
        );

        assertTrue(
                stable.getCategory()
                        .isStable()
        );

        assertNull(
                stable.getExpiresAt()
        );

        assertEquals(
                UserMemoryStatus.PENDING,
                recent.getStatus()
        );

        assertTrue(
                recent.getCategory()
                        .isRecent()
        );

        assertTrue(
                recent.getExpiresAt()
                        .isAfter(
                                beforeSave.plusDays(6)
                        )
        );

        assertTrue(
                recent.getExpiresAt()
                        .isBefore(
                                beforeSave.plusDays(8)
                        )
        );
    }

    @Test
    void 같은_사용자의_동일한_기억은_중복저장하지_않는다() {

        TestContext context =
                saveContext(true);

        DiaryMemoryCandidate candidate =
                new DiaryMemoryCandidate(
                        UserMemoryCategory.HOBBY,
                        "주말에 러닝을 즐김"
                );

        int firstSaved =
                persistenceService.saveCandidates(
                        context.diary().getId(),
                        List.of(candidate)
                );

        int secondSaved =
                persistenceService.saveCandidates(
                        context.diary().getId(),
                        List.of(candidate)
                );

        assertEquals(
                1,
                firstSaved
        );

        assertEquals(
                0,
                secondSaved
        );

        List<UserMemoryItem> memories =
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                context.user().getId(),
                                context.diary().getId()
                        );

        assertEquals(
                1,
                memories.size()
        );
    }

    @Test
    void 한_AI_응답안의_동일한_기억도_한번만_저장한다() {

        TestContext context =
                saveContext(true);

        DiaryMemoryCandidate candidate =
                new DiaryMemoryCandidate(
                        UserMemoryCategory.INTEREST,
                        "식물 키우기에 관심이 있음"
                );

        int savedCount =
                persistenceService.saveCandidates(
                        context.diary().getId(),
                        List.of(
                                candidate,
                                candidate
                        )
                );

        assertEquals(
                1,
                savedCount
        );

        assertEquals(
                1,
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                context.user().getId(),
                                context.diary().getId()
                        )
                        .size()
        );
    }

    @Test
    void AI_기억활용에_동의하지_않은_사용자의_후보는_저장하지_않는다() {

        TestContext context =
                saveContext(false);

        int savedCount =
                persistenceService.saveCandidates(
                        context.diary().getId(),
                        List.of(
                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.INTEREST,
                                        "식물 키우기에 관심이 있음"
                                )
                        )
                );

        assertEquals(
                0,
                savedCount
        );

        assertFalse(
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                context.user().getId(),
                                context.diary().getId()
                        )
                        .iterator()
                        .hasNext()
        );
    }

    @Test
    void 한_일기에서는_최대_다섯개의_후보만_저장한다() {

        TestContext context =
                saveContext(true);

        int savedCount =
                persistenceService.saveCandidates(
                        context.diary().getId(),
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
                                        UserMemoryCategory.INTEREST,
                                        "식물 키우기에 관심이 있음"
                                ),
                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.GOAL,
                                        "꾸준히 운동하는 것을 장기 목표로 삼고 있음"
                                ),
                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.ROUTINE,
                                        "저녁 시간에 운동하는 습관이 있음"
                                ),
                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.TRAIT,
                                        "새로운 활동을 직접 시도해보는 편임"
                                )
                        )
                );

        assertEquals(
                5,
                savedCount
        );

        assertEquals(
                5,
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                context.user().getId(),
                                context.diary().getId()
                        )
                        .size()
        );
    }

    private TestContext saveContext(
            boolean aiMemoryConsent
    ) {

        AppUser user =
                AppUser.createKakaoUser(
                        "memory-persistence-"
                                + System.nanoTime(),
                        "데이빗",
                        null,
                        null
                );

        user.updatePersonalSettings(
                "데이빗",
                "대학생",
                LocalTime.of(21, 0),
                aiMemoryConsent
        );

        AppUser savedUser =
                appUserRepository
                        .saveAndFlush(user);

        Diary diary =
                diaryRepository.saveAndFlush(
                        Diary.create(
                                savedUser,
                                """
                                오늘은 프로젝트를 진행했고
                                집에 와서 반려묘와 쉬었다.
                                """,
                                LocalDate.now()
                        )
                );

        return new TestContext(
                savedUser,
                diary
        );
    }

    private record TestContext(
            AppUser user,
            Diary diary
    ) {
    }
}