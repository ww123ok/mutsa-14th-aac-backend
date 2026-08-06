package mutsa.hackathon.domain;

import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import mutsa.hackathon.util.MemoryHashGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserMemoryItemIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private UserMemoryItemRepository
            userMemoryItemRepository;

    @Test
    void 기억_후보를_PENDING_상태로_저장한다() {
        TestContext context = saveUserAndDiary();

        String memoryText =
                "반려묘와 함께 생활함";

        String contentHash =
                MemoryHashGenerator.generate(
                        UserMemoryCategory.PET,
                        memoryText
                );

        UserMemoryItem saved =
                userMemoryItemRepository.saveAndFlush(
                        UserMemoryItem.createCandidate(
                                context.user(),
                                context.diary(),
                                UserMemoryCategory.PET,
                                memoryText,
                                contentHash,
                                null
                        )
                );

        assertNotNull(saved.getId());

        assertEquals(
                UserMemoryStatus.PENDING,
                saved.getStatus()
        );

        assertEquals(
                memoryText,
                saved.getMemoryText()
        );

        assertEquals(
                64,
                saved.getContentHash().length()
        );

        assertNull(saved.getApprovedAt());
        assertNull(saved.getRejectedAt());
        assertNull(saved.getRevokedAt());
    }

    @Test
    void 기억_후보를_승인할_수_있다() {
        TestContext context = saveUserAndDiary();

        UserMemoryItem memory =
                saveMemoryCandidate(
                        context,
                        UserMemoryCategory.WORK_STUDY,
                        "대학 팀 프로젝트를 진행 중임",
                        null
                );

        memory.approve();

        assertEquals(
                UserMemoryStatus.APPROVED,
                memory.getStatus()
        );

        assertNotNull(memory.getApprovedAt());
        assertNull(memory.getRejectedAt());
    }

    @Test
    void 기억_후보를_거절할_수_있다() {
        TestContext context = saveUserAndDiary();

        UserMemoryItem memory =
                saveMemoryCandidate(
                        context,
                        UserMemoryCategory.INTEREST,
                        "식물 기르기에 관심이 있음",
                        null
                );

        memory.reject();

        assertEquals(
                UserMemoryStatus.REJECTED,
                memory.getStatus()
        );

        assertNotNull(memory.getRejectedAt());
        assertNull(memory.getApprovedAt());
    }

    @Test
    void 만료된_진행_주제는_사용할_수_없다() {
        TestContext context = saveUserAndDiary();

        UserMemoryItem memory =
                saveMemoryCandidate(
                        context,
                        UserMemoryCategory.ONGOING_TOPIC,
                        "최근 팀 프로젝트 통합 테스트를 진행 중임",
                        LocalDateTime.now().minusDays(1)
                );

        memory.approve();

        assertTrue(
                memory.isExpired(
                        LocalDateTime.now()
                )
        );

        assertThrows(
                IllegalStateException.class,
                memory::markUsed
        );
    }

    @Test
    void 같은_사용자에게_동일한_기억을_중복_저장할_수_없다() {
        TestContext context = saveUserAndDiary();

        String contentHash =
                MemoryHashGenerator.generate(
                        UserMemoryCategory.PET,
                        "반려묘와 함께 생활함"
                );

        UserMemoryItem first =
                UserMemoryItem.createCandidate(
                        context.user(),
                        context.diary(),
                        UserMemoryCategory.PET,
                        "반려묘와 함께 생활함",
                        contentHash,
                        null
                );

        userMemoryItemRepository.saveAndFlush(first);

        UserMemoryItem duplicate =
                UserMemoryItem.createCandidate(
                        context.user(),
                        context.diary(),
                        UserMemoryCategory.PET,
                        "반려묘와 함께 생활함",
                        contentHash,
                        null
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        userMemoryItemRepository
                                .saveAndFlush(
                                        duplicate
                                )
        );
    }

    @Test
    void 공백_차이가_있는_동일한_기억은_같은_해시를_가진다() {
        String firstHash =
                MemoryHashGenerator.generate(
                        UserMemoryCategory.PET,
                        "반려묘와 함께 생활함"
                );

        String secondHash =
                MemoryHashGenerator.generate(
                        UserMemoryCategory.PET,
                        "  반려묘와   함께 생활함  "
                );

        assertEquals(
                firstHash,
                secondHash
        );

        assertFalse(firstHash.isBlank());
    }

    private UserMemoryItem saveMemoryCandidate(
            TestContext context,
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
                        context.user(),
                        context.diary(),
                        category,
                        memoryText,
                        contentHash,
                        expiresAt
                )
        );
    }

    private TestContext saveUserAndDiary() {
        AppUser user = appUserRepository.save(
                AppUser.createKakaoUser(
                        "memory-test-"
                                + System.nanoTime(),
                        "테스트사용자",
                        null,
                        null
                )
        );

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

    private record TestContext(
            AppUser user,
            Diary diary
    ) {
    }
}