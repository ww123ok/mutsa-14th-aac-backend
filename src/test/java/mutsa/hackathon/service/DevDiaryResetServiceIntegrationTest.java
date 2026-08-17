package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.DiaryShare;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.domain.SharedDiaryLog;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.dto.DevTodayDiaryResetResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import mutsa.hackathon.repository.DiaryShareRepository;
import mutsa.hackathon.repository.SharedDiaryLogRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import mutsa.hackathon.util.MemoryHashGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = {
                "app.dev.reset-enabled=true",
                "app.openai.reflection-enabled=false",
                "app.openai.reward-enabled=false"
        }
)
@ActiveProfiles("dev-reset-test")
class DevDiaryResetServiceIntegrationTest {

    private static final ZoneId SERVICE_ZONE =
            ZoneId.of("Asia/Seoul");

    @Autowired
    private DevDiaryResetService
            devDiaryResetService;

    @Autowired
    private AppUserRepository
            appUserRepository;

    @Autowired
    private DiaryRepository
            diaryRepository;

    @Autowired
    private DiaryRewardRepository
            diaryRewardRepository;

    @Autowired
    private AiQuestionRepository
            aiQuestionRepository;

    @Autowired
    private UserMemoryItemRepository
            userMemoryItemRepository;

    @Autowired
    private DiaryShareRepository
            diaryShareRepository;

    @Autowired
    private SharedDiaryLogRepository
            sharedDiaryLogRepository;

    @Autowired
    private AiMemoryProfileService
            aiMemoryProfileService;

    @Test
    void 오늘_일기와_연결된_데이터를_하드삭제하고_다시_작성할_수_있다() {
        LocalDate today =
                LocalDate.now(SERVICE_ZONE);

        AppUser user =
                saveUser();

        Diary yesterdayDiary =
                diaryRepository.saveAndFlush(
                        Diary.create(
                                user,
                                "어제는 반려묘와 산책했다.",
                                today.minusDays(1)
                        )
                );

        UserMemoryItem yesterdayMemory =
                saveApprovedMemory(
                        user,
                        yesterdayDiary,
                        UserMemoryCategory.PET,
                        "반려묘와 함께 생활함"
                );

        Diary todayDiary =
                diaryRepository.saveAndFlush(
                        Diary.create(
                                user,
                                "오늘은 프로젝트 테스트를 진행했다.",
                                today
                        )
                );

        DiaryReward todayReward =
                diaryRewardRepository.saveAndFlush(
                        DiaryReward.createPending(
                                todayDiary
                        )
                );

        AiQuestion todayQuestion =
                aiQuestionRepository.saveAndFlush(
                        AiQuestion.createReflection(
                                user,
                                todayDiary,
                                "오늘 가장 의미 있었던 순간은 무엇인가요?",
                                today,
                                QuestionGenerationSource.FALLBACK
                        )
                );

        AiQuestion todayWritingHelpQuestion =
                aiQuestionRepository.saveAndFlush(
                        AiQuestion.createWritingHelp(
                                user,
                                "오늘 가장 선명하게 기억나는 순간은 무엇인가요?",
                                1,
                                today,
                                QuestionGenerationSource.AI
                        )
                );

        UserMemoryItem todayMemory =
                saveApprovedMemory(
                        user,
                        todayDiary,
                        UserMemoryCategory.WORK_STUDY,
                        "대학 팀 프로젝트를 진행 중임"
                );

        aiMemoryProfileService.rebuildProfile(
                user.getId()
        );

        /*
         * rebuildProfile()은 서비스 트랜잭션에서 사용자를
         * 다시 조회하여 수정하므로 최신 엔티티를 재조회
         */
        AppUser profileBeforeReset =
                appUserRepository
                        .findById(user.getId())
                        .orElseThrow();

        assertNotNull(
                profileBeforeReset.getAiMemoryProfile()
        );

        assertTrue(
                profileBeforeReset
                        .getAiMemoryProfile()
                        .contains(
                                "대학 팀 프로젝트를 진행 중임"
                        )
        );

        DevTodayDiaryResetResponse response =
                devDiaryResetService.resetToday(
                        user.getId()
                );

        assertTrue(
                response.deleted()
        );

        assertEquals(
                todayDiary.getId(),
                response.diaryId()
        );

        assertEquals(
                today,
                response.recordedDate()
        );

        assertEquals(
                1,
                response.deletedRewardCount()
        );

        assertEquals(
                2,
                response.deletedQuestionCount()
        );

        assertEquals(
                1,
                response.deletedMemoryCount()
        );

        assertFalse(
                diaryRepository
                        .findById(
                                todayDiary.getId()
                        )
                        .isPresent()
        );

        assertFalse(
                diaryRewardRepository
                        .findById(
                                todayReward.getId()
                        )
                        .isPresent()
        );

        assertFalse(
                aiQuestionRepository
                        .findById(
                                todayQuestion.getId()
                        )
                        .isPresent()
        );

        assertFalse(
                aiQuestionRepository
                        .findById(
                                todayWritingHelpQuestion.getId()
                        )
                        .isPresent()
        );

        assertFalse(
                userMemoryItemRepository
                        .findById(
                                todayMemory.getId()
                        )
                        .isPresent()
        );

        assertTrue(
                userMemoryItemRepository
                        .findById(
                                yesterdayMemory.getId()
                        )
                        .isPresent()
        );

        AppUser savedUser =
                appUserRepository
                        .findById(user.getId())
                        .orElseThrow();

        assertNotNull(
                savedUser.getAiMemoryProfile()
        );

        assertTrue(
                savedUser.getAiMemoryProfile()
                        .contains(
                                "반려묘와 함께 생활함"
                        )
        );

        assertFalse(
                savedUser.getAiMemoryProfile()
                        .contains(
                                "대학 팀 프로젝트를 진행 중임"
                        )
        );

        Diary rewrittenDiary =
                diaryRepository.saveAndFlush(
                        Diary.create(
                                savedUser,
                                "초기화 후 오늘의 일기를 다시 작성했다.",
                                today
                        )
                );

        assertNotNull(
                rewrittenDiary.getId()
        );
    }

    @Test
    void 오늘_일기가_없어도_오늘_작성도움_질문은_초기화한다() {
        LocalDate today =
                LocalDate.now(SERVICE_ZONE);

        AppUser user =
                saveUser();

        AiQuestion todayWritingHelpQuestion =
                aiQuestionRepository.saveAndFlush(
                        AiQuestion.createWritingHelp(
                                user,
                                "오늘 무엇부터 기록해볼까요?",
                                1,
                                today,
                                QuestionGenerationSource.AI
                        )
                );

        AiQuestion yesterdayWritingHelpQuestion =
                aiQuestionRepository.saveAndFlush(
                        AiQuestion.createWritingHelp(
                                user,
                                "어제 가장 기억나는 순간은 무엇인가요?",
                                1,
                                today.minusDays(1),
                                QuestionGenerationSource.AI
                        )
                );

        DevTodayDiaryResetResponse response =
                devDiaryResetService.resetToday(
                        user.getId()
                );

        assertFalse(
                response.deleted()
        );

        assertEquals(
                today,
                response.recordedDate()
        );

        assertEquals(
                0,
                response.deletedRewardCount()
        );

        assertEquals(
                1,
                response.deletedQuestionCount()
        );

        assertEquals(
                0,
                response.deletedMemoryCount()
        );

        assertFalse(
                aiQuestionRepository
                        .findById(
                                todayWritingHelpQuestion.getId()
                        )
                        .isPresent()
        );

        assertTrue(
                aiQuestionRepository
                        .findById(
                                yesterdayWritingHelpQuestion.getId()
                        )
                        .isPresent()
        );

        assertEquals(
                0,
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                user.getId(),
                                AiQuestionType.WRITING_HELP,
                                today
                        )
        );
    }

    @Test
    void resetsTodayDiaryAndUnreceivedExperienceFragmentTogether() {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        AppUser user = saveUser();
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, "오늘 알바에서 손님 응대가 부담스러웠다.", today)
        );
        DiaryShare share = diaryShareRepository.saveAndFlush(DiaryShare.request(diary));

        DevTodayDiaryResetResponse response = devDiaryResetService.resetToday(user.getId());

        assertTrue(response.deleted());
        assertFalse(diaryRepository.findById(diary.getId()).isPresent());
        assertFalse(diaryShareRepository.findById(share.getId()).isPresent());
    }

    @Test
    void blocksResetWhenExperienceFragmentWasDeliveredToAnotherUser() {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        AppUser sender = saveUser();
        AppUser receiver = saveUser();
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(sender, "오늘 알바에서 손님 응대가 부담스러웠다.", today)
        );
        DiaryShare share = DiaryShare.request(diary);
        share.requireReview(
                "업무 상황을 일반화한 경험입니다.",
                "일과 부담",
                java.util.List.of("알바"),
                "알바 업무 부담 경험"
        );
        share.approve("[1.0,0.0]", "test");
        share = diaryShareRepository.saveAndFlush(share);
        DiaryShare persistedShare = diaryShareRepository.findByIdWithDiaryAndUser(share.getId()).orElseThrow();
        sharedDiaryLogRepository.saveAndFlush(SharedDiaryLog.create(receiver, persistedShare, 1));

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> devDiaryResetService.resetToday(sender.getId())
        );

        assertEquals(ErrorCode.DEV_DIARY_RESET_SHARED_DIARY_BLOCKED, exception.getErrorCode());
        assertTrue(diaryRepository.findById(diary.getId()).isPresent());
        assertTrue(diaryShareRepository.findById(share.getId()).isPresent());
    }

    private AppUser saveUser() {
        AppUser user =
                AppUser.createKakaoUser(
                        "dev-reset-"
                                + System.nanoTime(),
                        "데이빗",
                        null,
                        null
                );

        /*
         * 저장 전에 설정을 변경하여 한 트랜잭션으로
         * 확실하게 DB에 반영
         */
        user.updatePersonalSettings(
                "데이빗",
                "대학생",
                LocalTime.of(21, 0),
                true
        );

        return appUserRepository.saveAndFlush(
                user
        );
    }

    private UserMemoryItem saveApprovedMemory(
            AppUser user,
            Diary diary,
            UserMemoryCategory category,
            String memoryText
    ) {
        UserMemoryItem memory =
                UserMemoryItem.createCandidate(
                        user,
                        diary,
                        category,
                        memoryText,
                        MemoryHashGenerator.generate(
                                category,
                                memoryText
                        ),
                        null
                );

        /*
         * 저장된 엔티티를 트랜잭션 밖에서 수정하지 않고,
         * 승인한 상태로 한 번에 저장
         */
        memory.approve();

        return userMemoryItemRepository
                .saveAndFlush(memory);
    }
}
