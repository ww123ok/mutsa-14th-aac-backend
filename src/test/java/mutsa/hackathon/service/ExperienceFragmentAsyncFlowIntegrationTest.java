package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryShareStatus;
import mutsa.hackathon.dto.ExperienceFragmentResponse;
import mutsa.hackathon.dto.ExperienceMatchResponse;
import mutsa.hackathon.dto.ReceivedExperienceFragmentResponse;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryShareRepository;
import mutsa.hackathon.repository.SharedDiaryLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(ExperienceFragmentAsyncFlowIntegrationTest.ExperienceFragmentTestAiConfiguration.class)
class ExperienceFragmentAsyncFlowIntegrationTest {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    @Autowired
    private ExperienceFragmentService experienceFragmentService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private DiaryShareRepository diaryShareRepository;

    @Autowired
    private SharedDiaryLogRepository sharedDiaryLogRepository;

    @Test
    void requestsAnonymizationApprovesMatchesAndReceivesWithoutCallingOpenAi() throws InterruptedException {
        AppUser sender = saveUser("experience-sender");
        Diary senderDiary = diaryRepository.saveAndFlush(
                Diary.create(
                        sender,
                        "카페 알바에서 주문이 한꺼번에 밀려 손님 응대가 부담스러웠다.",
                        LocalDate.now().minusDays(1)
                )
        );

        ExperienceFragmentResponse requested = experienceFragmentService.request(sender.getId(), senderDiary.getId());
        assertEquals("REQUESTED", requested.status());

        ExperienceFragmentResponse reviewRequired = waitForReviewRequired(sender.getId(), requested.shareId());
        assertEquals("일과 부담", reviewRequired.generalTopic());
        assertEquals(List.of("알바", "업무 부담"), reviewRequired.keywords());

        ExperienceFragmentResponse approved = experienceFragmentService.approve(sender.getId(), requested.shareId());
        assertEquals("APPROVED", approved.status());
        assertNotNull(approved.approvedAt());

        AppUser receiver = saveUser("experience-receiver");
        receiver.addCredit(1);
        appUserRepository.saveAndFlush(receiver);
        Diary receiverDiary = diaryRepository.saveAndFlush(
                Diary.create(
                        receiver,
                        "오늘 알바에서 주문이 밀려서 손님 응대가 걱정됐다.",
                        LocalDate.now()
                )
        );

        Optional<ExperienceMatchResponse> match = experienceFragmentService.findBestMatch(
                receiver.getId(), receiverDiary.getId()
        );
        assertTrue(match.isPresent());
        assertEquals(requested.shareId(), match.get().shareId());
        assertEquals(1.0d, match.get().similarity());

        ReceivedExperienceFragmentResponse received = experienceFragmentService.receive(
                receiver.getId(), requested.shareId()
        );
        assertEquals(requested.shareId(), received.shareId());
        assertEquals(0, received.remainingCredit());
        assertTrue(sharedDiaryLogRepository.existsByReceiverIdAndDiaryShareId(receiver.getId(), requested.shareId()));
    }

    private ExperienceFragmentResponse waitForReviewRequired(Long userId, Long shareId) throws InterruptedException {
        Instant deadline = Instant.now().plus(WAIT_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            ExperienceFragmentResponse latest = experienceFragmentService.mine(userId).stream()
                    .filter(fragment -> fragment.shareId().equals(shareId))
                    .findFirst()
                    .orElseThrow();
            if (DiaryShareStatus.REVIEW_REQUIRED.name().equals(latest.status())) {
                return latest;
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        throw new AssertionError("Experience fragment did not reach REVIEW_REQUIRED within 10 seconds.");
    }

    private AppUser saveUser(String prefix) {
        String uniqueId = prefix + "-" + UUID.randomUUID();
        return appUserRepository.saveAndFlush(
                AppUser.createKakaoUser(uniqueId, "테스트사용자", uniqueId + "@example.com", null)
        );
    }

    @TestConfiguration
    static class ExperienceFragmentTestAiConfiguration {

        @Bean
        @Primary
        ExperienceFragmentProcessor experienceFragmentProcessor() {
            return diaryContent -> new ExperienceFragmentDraft(
                    "서비스 업무에서 부담을 느꼈지만 동료와 역할을 나눴다.",
                    "일과 부담",
                    List.of("알바", "업무 부담"),
                    "알바 업무 부담을 겪고 역할을 나눈 경험"
            );
        }

        @Bean
        @Primary
        ExperienceEmbeddingGenerator experienceEmbeddingGenerator() {
            return text -> new ExperienceEmbedding("test-embedding", List.of(1.0d, 0.0d));
        }

        @Bean
        @Primary
        ExperienceStructureExtractor experienceStructureExtractor() {
            return diaryContent -> new ExperienceStructure(
                    "상황: 업무 부담 | 핵심 어려움: 일이 한꺼번에 몰림 | 반응: 역할을 나눔",
                    List.of("업무 부담", "일이 한꺼번에 몰림")
            );
        }
    }
}
