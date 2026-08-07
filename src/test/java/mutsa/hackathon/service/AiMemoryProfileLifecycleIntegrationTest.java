package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import mutsa.hackathon.dto.MeUpdateRequest;
import mutsa.hackathon.dto.MemoryCandidateReviewRequest;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import mutsa.hackathon.util.MemoryHashGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AiMemoryProfileLifecycleIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private UserMemoryItemRepository
            userMemoryItemRepository;

    @Autowired
    private AiMemoryProfileService
            aiMemoryProfileService;

    @Autowired
    private UserMemoryService userMemoryService;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void 승인된_활성_기억만_AI_기억_프로필에_포함된다()
            throws Exception {

        TestContext context =
                saveContext();

        UserMemoryItem stable =
                saveCandidate(
                        context,
                        UserMemoryCategory.PET,
                        "반려묘와 함께 생활함",
                        null
                );

        stable.approve();

        UserMemoryItem ongoing =
                saveCandidate(
                        context,
                        UserMemoryCategory
                                .ONGOING_TOPIC,
                        "최근 팀 프로젝트 통합 테스트를 진행 중임",
                        LocalDateTime.now()
                                .plusDays(30)
                );

        ongoing.approve();

        UserMemoryItem pending =
                saveCandidate(
                        context,
                        UserMemoryCategory.INTEREST,
                        "식물 기르기에 관심이 있음",
                        null
                );

        UserMemoryItem rejected =
                saveCandidate(
                        context,
                        UserMemoryCategory.ROUTINE,
                        "저녁에 산책하는 습관이 있음",
                        null
                );

        rejected.reject();

        UserMemoryItem expired =
                saveCandidate(
                        context,
                        UserMemoryCategory
                                .ONGOING_TOPIC,
                        "이미 끝난 과제를 진행 중임",
                        LocalDateTime.now()
                                .minusDays(1)
                );

        expired.approve();

        userMemoryItemRepository.flush();

        aiMemoryProfileService.rebuildProfile(
                context.user().getId()
        );

        AppUser savedUser =
                appUserRepository
                        .findById(
                                context.user().getId()
                        )
                        .orElseThrow();

        assertNotNull(
                savedUser.getAiMemoryProfile()
        );

        JsonNode profile =
                jsonMapper.readTree(
                        savedUser
                                .getAiMemoryProfile()
                );

        assertEquals(
                1,
                profile.get("schemaVersion")
                        .asInt()
        );

        assertTrue(
                containsText(
                        profile.get(
                                "stableMemories"
                        ),
                        "반려묘와 함께 생활함"
                )
        );

        assertTrue(
                containsText(
                        profile.get(
                                "ongoingTopics"
                        ),
                        "최근 팀 프로젝트 통합 테스트를 진행 중임"
                )
        );

        assertFalse(
                containsText(
                        profile.get(
                                "stableMemories"
                        ),
                        pending.getMemoryText()
                )
        );

        assertFalse(
                containsText(
                        profile.get(
                                "stableMemories"
                        ),
                        rejected.getMemoryText()
                )
        );

        assertFalse(
                containsText(
                        profile.get(
                                "ongoingTopics"
                        ),
                        expired.getMemoryText()
                )
        );
    }

    @Test
    void 기억_후보를_승인하면_AI_기억_프로필이_자동으로_생성된다()
            throws Exception {

        TestContext context =
                saveContext();

        saveCandidate(
                context,
                UserMemoryCategory.WORK_STUDY,
                "대학 팀 프로젝트를 진행 중임",
                null
        );

        userMemoryService.reviewCandidates(
                context.user().getId(),
                context.diary().getId(),
                new MemoryCandidateReviewRequest(
                        true
                )
        );

        AppUser savedUser =
                appUserRepository
                        .findById(
                                context.user().getId()
                        )
                        .orElseThrow();

        assertNotNull(
                savedUser.getAiMemoryProfile()
        );

        JsonNode profile =
                jsonMapper.readTree(
                        savedUser
                                .getAiMemoryProfile()
                );

        assertTrue(
                containsText(
                        profile.get(
                                "stableMemories"
                        ),
                        "대학 팀 프로젝트를 진행 중임"
                )
        );
    }

    @Test
    void AI_기억_동의를_철회하면_사용_가능한_기억과_프로필을_폐기한다() {
        TestContext context =
                saveContext();

        UserMemoryItem approved =
                saveCandidate(
                        context,
                        UserMemoryCategory.PET,
                        "반려묘와 함께 생활함",
                        null
                );

        approved.approve();

        UserMemoryItem pending =
                saveCandidate(
                        context,
                        UserMemoryCategory.GOAL,
                        "취업 준비를 진행 중임",
                        null
                );

        UserMemoryItem rejected =
                saveCandidate(
                        context,
                        UserMemoryCategory.INTEREST,
                        "요리에 관심이 있음",
                        null
                );

        rejected.reject();

        userMemoryItemRepository.flush();

        aiMemoryProfileService.rebuildProfile(
                context.user().getId()
        );

        assertNotNull(
                context.user()
                        .getAiMemoryProfile()
        );

        appUserService.updateMe(
                context.user().getId(),
                new MeUpdateRequest(
                        "데이빗",
                        "대학생",
                        LocalTime.of(21, 0),
                        false
                )
        );

        List<UserMemoryItem> memories =
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                context.user().getId(),
                                context.diary().getId()
                        );

        UserMemoryItem savedApproved =
                findById(
                        memories,
                        approved.getId()
                );

        UserMemoryItem savedPending =
                findById(
                        memories,
                        pending.getId()
                );

        UserMemoryItem savedRejected =
                findById(
                        memories,
                        rejected.getId()
                );

        assertEquals(
                UserMemoryStatus.REVOKED,
                savedApproved.getStatus()
        );

        assertEquals(
                UserMemoryStatus.REVOKED,
                savedPending.getStatus()
        );

        /*
         * 이미 거절된 후보는 사용 가능한 기억이 아니므로
         * REJECTED 이력을 그대로 유지합니다.
         */
        assertEquals(
                UserMemoryStatus.REJECTED,
                savedRejected.getStatus()
        );

        assertNull(
                context.user()
                        .getAiMemoryProfile()
        );

        assertFalse(
                context.user()
                        .isAiMemoryConsent()
        );
    }

    @Test
    void 일기를_삭제하면_해당_일기의_기억만_폐기하고_다른_기억은_유지한다()
            throws Exception {

        AppUser user = saveUser();

        Diary firstDiary =
                diaryRepository.save(
                        Diary.create(
                                user,
                                "오늘 반려묘와 시간을 보냈다.",
                                LocalDate.now()
                                        .minusDays(2)
                        )
                );

        Diary secondDiary =
                diaryRepository.save(
                        Diary.create(
                                user,
                                "오늘 팀 프로젝트를 진행했다.",
                                LocalDate.now()
                                        .minusDays(1)
                        )
                );

        UserMemoryItem firstMemory =
                saveCandidate(
                        user,
                        firstDiary,
                        UserMemoryCategory.PET,
                        "반려묘와 함께 생활함",
                        null
                );

        firstMemory.approve();

        UserMemoryItem secondMemory =
                saveCandidate(
                        user,
                        secondDiary,
                        UserMemoryCategory.WORK_STUDY,
                        "대학 팀 프로젝트를 진행 중임",
                        null
                );

        secondMemory.approve();

        userMemoryItemRepository.flush();

        aiMemoryProfileService.rebuildProfile(
                user.getId()
        );

        diaryService.deleteDiary(
                user.getId(),
                firstDiary.getId()
        );

        UserMemoryItem savedFirstMemory =
                userMemoryItemRepository
                        .findById(
                                firstMemory.getId()
                        )
                        .orElseThrow();

        UserMemoryItem savedSecondMemory =
                userMemoryItemRepository
                        .findById(
                                secondMemory.getId()
                        )
                        .orElseThrow();

        assertEquals(
                UserMemoryStatus.REVOKED,
                savedFirstMemory.getStatus()
        );

        assertEquals(
                UserMemoryStatus.APPROVED,
                savedSecondMemory.getStatus()
        );

        AppUser savedUser =
                appUserRepository
                        .findById(user.getId())
                        .orElseThrow();

        assertNotNull(
                savedUser.getAiMemoryProfile()
        );

        JsonNode profile =
                jsonMapper.readTree(
                        savedUser
                                .getAiMemoryProfile()
                );

        assertFalse(
                containsText(
                        profile.get(
                                "stableMemories"
                        ),
                        "반려묘와 함께 생활함"
                )
        );

        assertTrue(
                containsText(
                        profile.get(
                                "stableMemories"
                        ),
                        "대학 팀 프로젝트를 진행 중임"
                )
        );
    }

    @Test
    void AI_기억_프로필은_삼천자를_넘지_않는다() {
        TestContext context =
                saveContext();

        for (int index = 0; index < 15; index++) {
            String memoryText =
                    "관심사 "
                            + index
                            + " "
                            + "가".repeat(430);

            UserMemoryItem memory =
                    saveCandidate(
                            context,
                            UserMemoryCategory.INTEREST,
                            memoryText,
                            null
                    );

            memory.approve();
        }

        userMemoryItemRepository.flush();

        aiMemoryProfileService.rebuildProfile(
                context.user().getId()
        );

        String profile =
                context.user()
                        .getAiMemoryProfile();

        assertNotNull(profile);

        assertTrue(
                profile.length() <= 3_000
        );
    }

    private TestContext saveContext() {
        AppUser user = saveUser();

        Diary diary =
                diaryRepository.save(
                        Diary.create(
                                user,
                                "오늘 반려묘와 놀고 팀 프로젝트를 진행했다.",
                                LocalDate.now()
                                        .minusDays(1)
                        )
                );

        return new TestContext(
                user,
                diary
        );
    }

    private AppUser saveUser() {
        AppUser user =
                appUserRepository.save(
                        AppUser.createKakaoUser(
                                "profile-lifecycle-"
                                        + System.nanoTime(),
                                "카카오닉네임",
                                null,
                                null
                        )
                );

        user.updatePersonalSettings(
                "데이빗",
                "대학생",
                LocalTime.of(21, 0),
                true
        );

        return user;
    }

    private UserMemoryItem saveCandidate(
            TestContext context,
            UserMemoryCategory category,
            String memoryText,
            LocalDateTime expiresAt
    ) {
        return saveCandidate(
                context.user(),
                context.diary(),
                category,
                memoryText,
                expiresAt
        );
    }

    private UserMemoryItem saveCandidate(
            AppUser user,
            Diary diary,
            UserMemoryCategory category,
            String memoryText,
            LocalDateTime expiresAt
    ) {
        String contentHash =
                MemoryHashGenerator.generate(
                        category,
                        memoryText
                );

        return userMemoryItemRepository.save(
                UserMemoryItem.createCandidate(
                        user,
                        diary,
                        category,
                        memoryText,
                        contentHash,
                        expiresAt
                )
        );
    }

    private UserMemoryItem findById(
            List<UserMemoryItem> memories,
            Long memoryId
    ) {
        return memories.stream()
                .filter(memory ->
                        memory.getId()
                                .equals(memoryId)
                )
                .findFirst()
                .orElseThrow();
    }

    private boolean containsText(
            JsonNode arrayNode,
            String expectedText
    ) {
        if (
                arrayNode == null
                        || !arrayNode.isArray()
        ) {
            return false;
        }

        for (JsonNode item : arrayNode) {
            JsonNode textNode =
                    item.get("text");

            if (
                    textNode != null
                            && expectedText.equals(
                            textNode.asText()
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    private record TestContext(
            AppUser user,
            Diary diary
    ) {
    }
}