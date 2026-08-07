package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import mutsa.hackathon.dto.MemoryCandidateListResponse;
import mutsa.hackathon.dto.MemoryCandidateReviewRequest;
import mutsa.hackathon.dto.MemoryCandidateReviewResponse;
import mutsa.hackathon.dto.MemoryReviewStatus;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import mutsa.hackathon.util.MemoryHashGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserMemoryServiceIntegrationTest {

    @Autowired
    private UserMemoryService userMemoryService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private UserMemoryItemRepository
            userMemoryItemRepository;

    @Test
    void 일기에서_생성된_기억_후보를_조회한다() {
        TestContext context =
                saveContext(true);

        saveCandidate(
                context,
                UserMemoryCategory.PET,
                "반려묘와 함께 생활함"
        );

        MemoryCandidateListResponse response =
                userMemoryService.getCandidates(
                        context.user().getId(),
                        context.diary().getId()
                );

        assertEquals(
                context.diary().getId(),
                response.diaryId()
        );

        assertEquals(
                MemoryReviewStatus.PENDING,
                response.reviewStatus()
        );

        assertTrue(
                response.reviewRequired()
        );

        assertEquals(
                1,
                response.items().size()
        );

        assertEquals(
                "반려묘와 함께 생활함",
                response.items()
                        .get(0)
                        .memoryText()
        );
    }

    @Test
    void 기억_후보가_없으면_NONE과_빈_목록을_반환한다() {
        TestContext context =
                saveContext(true);

        MemoryCandidateListResponse response =
                userMemoryService.getCandidates(
                        context.user().getId(),
                        context.diary().getId()
                );

        assertEquals(
                MemoryReviewStatus.NONE,
                response.reviewStatus()
        );

        assertFalse(
                response.reviewRequired()
        );

        assertTrue(
                response.items().isEmpty()
        );
    }

    @Test
    void Yes를_선택하면_모든_PENDING_기억을_승인한다() {
        TestContext context =
                saveContext(true);

        saveCandidate(
                context,
                UserMemoryCategory.PET,
                "반려묘와 함께 생활함"
        );

        saveCandidate(
                context,
                UserMemoryCategory.WORK_STUDY,
                "대학 팀 프로젝트를 진행 중임"
        );

        MemoryCandidateReviewResponse response =
                userMemoryService.reviewCandidates(
                        context.user().getId(),
                        context.diary().getId(),
                        new MemoryCandidateReviewRequest(
                                true
                        )
                );

        assertEquals(
                MemoryReviewStatus.APPROVED,
                response.reviewStatus()
        );

        assertEquals(
                2,
                response.reviewedCount()
        );

        assertTrue(
                response.items()
                        .stream()
                        .allMatch(item ->
                                item.status()
                                        == UserMemoryStatus
                                        .APPROVED
                        )
        );

        List<UserMemoryItem> savedMemories =
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                context.user().getId(),
                                context.diary().getId()
                        );

        assertTrue(
                savedMemories.stream()
                        .allMatch(memory ->
                                memory.getStatus()
                                        == UserMemoryStatus
                                        .APPROVED
                        )
        );
    }

    @Test
    void 동일한_승인_요청은_멱등하게_처리한다() {
        TestContext context =
                saveContext(true);

        saveCandidate(
                context,
                UserMemoryCategory.INTEREST,
                "식물 기르기에 관심이 있음"
        );

        MemoryCandidateReviewRequest request =
                new MemoryCandidateReviewRequest(
                        true
                );

        userMemoryService.reviewCandidates(
                context.user().getId(),
                context.diary().getId(),
                request
        );

        MemoryCandidateReviewResponse second =
                userMemoryService.reviewCandidates(
                        context.user().getId(),
                        context.diary().getId(),
                        request
                );

        assertEquals(
                MemoryReviewStatus.APPROVED,
                second.reviewStatus()
        );

        assertEquals(
                0,
                second.reviewedCount()
        );
    }

    @Test
    void No를_선택하면_모든_PENDING_기억을_거절한다() {
        TestContext context =
                saveContext(true);

        saveCandidate(
                context,
                UserMemoryCategory.ROUTINE,
                "저녁에 산책하는 습관이 있음"
        );

        MemoryCandidateReviewResponse response =
                userMemoryService.reviewCandidates(
                        context.user().getId(),
                        context.diary().getId(),
                        new MemoryCandidateReviewRequest(
                                false
                        )
                );

        assertEquals(
                MemoryReviewStatus.REJECTED,
                response.reviewStatus()
        );

        assertEquals(
                1,
                response.reviewedCount()
        );

        assertEquals(
                UserMemoryStatus.REJECTED,
                response.items()
                        .get(0)
                        .status()
        );
    }

    @Test
    void 검토가_끝난_후에는_반대_결정으로_변경할_수_없다() {
        TestContext context =
                saveContext(true);

        saveCandidate(
                context,
                UserMemoryCategory.GOAL,
                "취업 준비를 진행 중임"
        );

        userMemoryService.reviewCandidates(
                context.user().getId(),
                context.diary().getId(),
                new MemoryCandidateReviewRequest(
                        false
                )
        );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                userMemoryService
                                        .reviewCandidates(
                                                context
                                                        .user()
                                                        .getId(),
                                                context
                                                        .diary()
                                                        .getId(),
                                                new MemoryCandidateReviewRequest(
                                                        true
                                                )
                                        )
                );

        assertEquals(
                ErrorCode
                        .MEMORY_REVIEW_ALREADY_COMPLETED,
                exception.getErrorCode()
        );
    }

    @Test
    void AI_기억_동의가_없으면_후보를_승인할_수_없다() {
        TestContext context =
                saveContext(false);

        saveCandidate(
                context,
                UserMemoryCategory.PET,
                "반려묘와 함께 생활함"
        );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                userMemoryService
                                        .reviewCandidates(
                                                context
                                                        .user()
                                                        .getId(),
                                                context
                                                        .diary()
                                                        .getId(),
                                                new MemoryCandidateReviewRequest(
                                                        true
                                                )
                                        )
                );

        assertEquals(
                ErrorCode
                        .AI_MEMORY_CONSENT_REQUIRED,
                exception.getErrorCode()
        );
    }

    @Test
    void 다른_사용자의_일기_기억은_조회할_수_없다() {
        TestContext ownerContext =
                saveContext(true);

        AppUser otherUser =
                saveUser(true);

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                userMemoryService
                                        .getCandidates(
                                                otherUser.getId(),
                                                ownerContext
                                                        .diary()
                                                        .getId()
                                        )
                );

        assertEquals(
                ErrorCode.DIARY_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    private TestContext saveContext(
            boolean aiMemoryConsent
    ) {
        AppUser user =
                saveUser(aiMemoryConsent);

        Diary diary = diaryRepository.save(
                Diary.create(
                        user,
                        "오늘 반려묘와 놀고 팀 프로젝트를 진행했다.",
                        LocalDate.now()
                )
        );

        return new TestContext(
                user,
                diary
        );
    }

    private AppUser saveUser(
            boolean aiMemoryConsent
    ) {
        AppUser user =
                appUserRepository.save(
                        AppUser.createKakaoUser(
                                "memory-service-test-"
                                        + System.nanoTime(),
                                "카카오닉네임",
                                null,
                                null
                        )
                );

        if (aiMemoryConsent) {
            user.updatePersonalSettings(
                    "데이빗",
                    "대학생",
                    LocalTime.of(21, 0),
                    true
            );
        }

        return user;
    }

    private UserMemoryItem saveCandidate(
            TestContext context,
            UserMemoryCategory category,
            String memoryText
    ) {
        String contentHash =
                MemoryHashGenerator.generate(
                        category,
                        memoryText
                );

        return userMemoryItemRepository.save(
                UserMemoryItem.createCandidate(
                        context.user(),
                        context.diary(),
                        category,
                        memoryText,
                        contentHash,
                        null
                )
        );
    }

    private record TestContext(
            AppUser user,
            Diary diary
    ) {
    }
}