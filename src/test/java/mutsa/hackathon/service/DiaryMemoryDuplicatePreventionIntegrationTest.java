package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DiaryMemoryDuplicatePreventionIntegrationTest {

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
    void 온보딩_직업과_같은_WORK_STUDY_후보는_저장하지_않고_새로운_최근맥락만_저장한다() {

        TestContext context =
                saveContext();

        int savedCount =
                persistenceService.saveCandidates(
                        context.diary().getId(),
                        List.of(
                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.WORK_STUDY,
                                        "대학생임"
                                ),
                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.ONGOING_TOPIC,
                                        "최근 자격시험을 준비하고 있음"
                                )
                        )
                );

        assertEquals(
                1,
                savedCount
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

        assertEquals(
                UserMemoryCategory.ONGOING_TOPIC,
                memories.get(0)
                        .getCategory()
        );
    }

    @Test
    void 같은_AI_응답에서_공백과_문장부호만_다른_후보는_한번만_저장한다() {

        TestContext context =
                saveContext();

        int savedCount =
                persistenceService.saveCandidates(
                        context.diary().getId(),
                        List.of(
                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.INTEREST,
                                        "식물 키우기에 관심이 있음"
                                ),
                                new DiaryMemoryCandidate(
                                        UserMemoryCategory.INTEREST,
                                        "식물 키우기에 관심이 있음."
                                )
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

    private TestContext saveContext() {

        AppUser user =
                AppUser.createKakaoUser(
                        "memory-dedup-"
                                + System.nanoTime(),
                        "데이빗",
                        null,
                        null
                );

        user.updatePersonalSettings(
                "데이빗",
                "대학생",
                LocalTime.of(21, 0),
                true
        );

        AppUser savedUser =
                appUserRepository
                        .saveAndFlush(user);

        Diary diary =
                diaryRepository.saveAndFlush(
                        Diary.create(
                                savedUser,
                                "오늘은 프로젝트를 진행했다.",
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