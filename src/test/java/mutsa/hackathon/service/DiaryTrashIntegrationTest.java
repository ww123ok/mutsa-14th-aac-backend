package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.DiaryShare;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.domain.SharedDiaryLog;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.domain.WeeklyRewardEntry;
import mutsa.hackathon.dto.DiaryTrashDetailResponse;
import mutsa.hackathon.dto.DiaryTrashResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import mutsa.hackathon.repository.DiaryShareRepository;
import mutsa.hackathon.repository.SharedDiaryLogRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import mutsa.hackathon.repository.WeeklyRewardEntryRepository;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DiaryTrashIntegrationTest {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private DiaryTrashService diaryTrashService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private DiaryRewardRepository diaryRewardRepository;

    @Autowired
    private AiQuestionRepository aiQuestionRepository;

    @Autowired
    private UserMemoryItemRepository userMemoryItemRepository;

    @Autowired
    private DiaryShareRepository diaryShareRepository;

    @Autowired
    private SharedDiaryLogRepository sharedDiaryLogRepository;

    @Autowired
    private WeeklyRewardRepository weeklyRewardRepository;

    @Autowired
    private WeeklyRewardEntryRepository weeklyRewardEntryRepository;

    @Test
    void 삭제한_일기는_일반조회에서_사라지고_휴지통_목록과_상세에서_조회된다() {
        AppUser user = saveUser("trash-list-owner");

        Diary older = saveDiary(
                user,
                LocalDate.of(2026, 8, 1),
                "먼저 삭제할 일기"
        );
        saveCompletedReward(older, "#AA7755");
        saveReflection(user, older, "첫 번째 성찰 질문");

        Diary newer = saveDiary(
                user,
                LocalDate.of(2026, 8, 2),
                "나중에 삭제할 일기"
        );
        saveCompletedReward(newer, "#5577AA");

        diaryService.deleteDiary(user.getId(), older.getId());
        diaryService.deleteDiary(user.getId(), newer.getId());
        diaryRepository.flush();

        assertThrows(
                ProjectException.class,
                () -> diaryService.getDiary(
                        user.getId(),
                        older.getId()
                )
        );

        List<DiaryTrashResponse> trash =
                diaryTrashService.getTrash(user.getId());

        assertEquals(2, trash.size());
        assertEquals(
                java.util.Set.of(older.getId(), newer.getId()),
                trash.stream()
                        .map(DiaryTrashResponse::diaryId)
                        .collect(java.util.stream.Collectors.toSet())
        );
        assertNotNull(trash.get(0).deletedAt());
        assertNotNull(trash.get(1).deletedAt());
        assertFalse(
                trash.get(0).deletedAt()
                        .isBefore(trash.get(1).deletedAt())
        );

        DiaryTrashDetailResponse detail =
                diaryTrashService.getTrashDiary(
                        user.getId(),
                        older.getId()
                );

        assertEquals(older.getId(), detail.diaryId());
        assertEquals("먼저 삭제할 일기", detail.content());
        assertNotNull(detail.deletedAt());
        assertEquals("#AA7755", detail.reward().colorHex());
        assertEquals("첫 번째 성찰 질문", detail.reflection().questionText());
    }

    @Test
    void 휴지통_일기를_복원하면_일반_아카이브에_다시_노출되고_휴지통에서는_사라진다() {
        AppUser user = saveUser("trash-restore-owner");
        Diary diary = saveDiary(
                user,
                LocalDate.of(2026, 8, 3),
                "복원할 일기"
        );
        saveCompletedReward(diary, "#668855");
        saveReflection(user, diary, "복원 후에도 남아야 할 질문");

        diaryService.deleteDiary(user.getId(), diary.getId());
        diaryTrashService.restore(user.getId(), diary.getId());
        diaryRepository.flush();

        Diary restored = diaryRepository.findById(diary.getId())
                .orElseThrow();

        assertFalse(restored.isDeleted());
        assertEquals(null, restored.getDeletedAt());
        assertTrue(diaryTrashService.getTrash(user.getId()).isEmpty());

        assertEquals(
                diary.getId(),
                diaryService.getDiary(
                        user.getId(),
                        diary.getId()
                ).diaryId()
        );
    }

    @Test
    void 휴지통_일기를_영구삭제하면_원본과_직접_파생데이터를_실제로_제거한다() {
        AppUser owner = saveUser("trash-hard-delete-owner");
        AppUser receiver = saveUser("trash-hard-delete-receiver");

        Diary diary = saveDiary(
                owner,
                LocalDate.of(2026, 8, 4),
                "영구 삭제할 일기"
        );
        DiaryReward reward = saveCompletedReward(
                diary,
                "#886644"
        );
        AiQuestion reflection = saveReflection(
                owner,
                diary,
                "삭제될 성찰 질문"
        );

        UserMemoryItem memory = userMemoryItemRepository.saveAndFlush(
                UserMemoryItem.createCandidate(
                        owner,
                        diary,
                        UserMemoryCategory.INTEREST,
                        "삭제될 기억 후보",
                        "a".repeat(64),
                        null
                )
        );

        DiaryShare share = DiaryShare.request(diary);
        share.requireReview(
                "익명화된 경험",
                "일상",
                List.of("일상"),
                "익명화된 경험 일상"
        );
        share.approve("[0.1,0.2]", "test-embedding");
        share = diaryShareRepository.saveAndFlush(share);

        SharedDiaryLog delivery = sharedDiaryLogRepository.saveAndFlush(
                SharedDiaryLog.create(
                        receiver,
                        share,
                        1
                )
        );

        WeeklyReward weeklyReward = weeklyRewardRepository.saveAndFlush(
                WeeklyReward.createPending(
                        owner,
                        LocalDate.of(2026, 8, 3),
                        LocalDate.of(2026, 8, 9)
                )
        );
        WeeklyRewardEntry weeklyEntry = weeklyRewardEntryRepository.saveAndFlush(
                WeeklyRewardEntry.fallback(
                        weeklyReward,
                        diary,
                        "#886644"
                )
        );

        diaryService.deleteDiary(owner.getId(), diary.getId());
        diaryTrashService.permanentlyDelete(
                owner.getId(),
                diary.getId()
        );

        assertTrue(diaryRepository.findById(diary.getId()).isEmpty());
        assertTrue(diaryRewardRepository.findById(reward.getId()).isEmpty());
        assertTrue(aiQuestionRepository.findById(reflection.getId()).isEmpty());
        assertTrue(userMemoryItemRepository.findById(memory.getId()).isEmpty());
        assertTrue(diaryShareRepository.findById(share.getId()).isEmpty());
        assertTrue(sharedDiaryLogRepository.findById(delivery.getId()).isEmpty());
        assertTrue(weeklyRewardEntryRepository.findById(weeklyEntry.getId()).isEmpty());
        assertTrue(weeklyRewardRepository.findById(weeklyReward.getId()).isEmpty());
    }

    @Test
    void 다른_사용자와_일반_일기는_휴지통_API로_복원하거나_영구삭제할_수_없다() {
        AppUser owner = saveUser("trash-owner-policy");
        AppUser other = saveUser("trash-other-policy");

        Diary trashed = saveDiary(
                owner,
                LocalDate.of(2026, 8, 5),
                "소유자의 휴지통 일기"
        );
        diaryService.deleteDiary(owner.getId(), trashed.getId());

        ProjectException otherUserException = assertThrows(
                ProjectException.class,
                () -> diaryTrashService.restore(
                        other.getId(),
                        trashed.getId()
                )
        );
        assertEquals(
                ErrorCode.TRASH_DIARY_NOT_FOUND,
                otherUserException.getErrorCode()
        );

        Diary active = saveDiary(
                owner,
                LocalDate.of(2026, 8, 6),
                "아직 삭제하지 않은 일기"
        );

        ProjectException activeDiaryException = assertThrows(
                ProjectException.class,
                () -> diaryTrashService.permanentlyDelete(
                        owner.getId(),
                        active.getId()
                )
        );
        assertEquals(
                ErrorCode.TRASH_DIARY_NOT_FOUND,
                activeDiaryException.getErrorCode()
        );
    }

    private AppUser saveUser(
            String providerIdPrefix
    ) {
        return appUserRepository.saveAndFlush(
                AppUser.createKakaoUser(
                        providerIdPrefix + "-" + System.nanoTime(),
                        "테스트사용자",
                        null,
                        null
                )
        );
    }

    private Diary saveDiary(
            AppUser user,
            LocalDate recordedDate,
            String content
    ) {
        return diaryRepository.saveAndFlush(
                Diary.create(
                        user,
                        content,
                        recordedDate
                )
        );
    }

    private DiaryReward saveCompletedReward(
            Diary diary,
            String colorHex
    ) {
        DiaryReward reward = DiaryReward.createPending(diary);
        reward.complete(
                colorHex,
                List.of("기록"),
                "기록 속 장면을 바탕으로 오늘의 색을 정했습니다."
        );
        return diaryRewardRepository.saveAndFlush(reward);
    }

    private AiQuestion saveReflection(
            AppUser user,
            Diary diary,
            String questionText
    ) {
        return aiQuestionRepository.saveAndFlush(
                AiQuestion.createReflection(
                        user,
                        diary,
                        questionText,
                        diary.getRecordedDate(),
                        QuestionGenerationSource.AI
                )
        );
    }
}
