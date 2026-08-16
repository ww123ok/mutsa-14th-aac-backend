package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryShare;
import mutsa.hackathon.domain.DiaryShareStatus;
import mutsa.hackathon.domain.SharedDiaryLog;
import mutsa.hackathon.dto.ExperienceFragmentFeedbackRequest;
import mutsa.hackathon.dto.ExperienceFragmentFeedbackResponse;
import mutsa.hackathon.dto.ExperienceFragmentReviewResponse;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.CreditTransactionRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryShareRepository;
import mutsa.hackathon.repository.SharedDiaryLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ExperienceFragmentReviewAndFeedbackServiceTest {

    @Mock private DiaryRepository diaryRepository;
    @Mock private DiaryShareRepository diaryShareRepository;
    @Mock private SharedDiaryLogRepository sharedDiaryLogRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private CreditTransactionRepository creditTransactionRepository;
    @Mock private ExperienceFragmentProcessor experienceFragmentProcessor;
    @Mock private ExperienceEmbeddingGenerator experienceEmbeddingGenerator;
    @Mock private ExperienceFragmentDraftPersistenceService draftPersistenceService;
    @Mock private ExperienceFragmentApprovalPersistenceService approvalPersistenceService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ExperienceFragmentService service;

    @Test
    void returnsOriginalAndAnonymizedContentOnlyToTheOwner() {
        DiaryShare share = reviewRequiredShare(10L, 1L);
        when(diaryShareRepository.findByIdWithDiaryAndUser(10L)).thenReturn(Optional.of(share));

        ExperienceFragmentReviewResponse result = service.review(1L, 10L);

        assertEquals("원문 일기", result.originalContent());
        assertEquals("익명화된 경험", result.anonymizedContent());
        assertNotNull(result.reviewAvailableAt());
    }

    @Test
    void autoApprovesOnlyReviewsThatHaveWaitedFiveDays() {
        DiaryShare share = reviewRequiredShare(10L, 1L);
        ReflectionTestUtils.setField(share, "reviewAvailableAt", LocalDateTime.now().minusDays(5).minusMinutes(1));
        when(diaryShareRepository.findIdsReadyForAutoApproval(
                org.mockito.ArgumentMatchers.eq(DiaryShareStatus.REVIEW_REQUIRED), any(LocalDateTime.class)))
                .thenReturn(List.of(10L));
        when(diaryShareRepository.findByIdWithDiaryAndUser(10L)).thenReturn(Optional.of(share));
        when(experienceEmbeddingGenerator.generate("알바 경험")).thenReturn(
                new ExperienceEmbedding("test", List.of(1.0, 0.0)));
        ReflectionTestUtils.setField(service, "autoApprovalDays", 5);

        service.autoApproveExpiredReviews();

        verify(approvalPersistenceService).approve(1L, 10L, new ExperienceEmbedding("test", List.of(1.0, 0.0)));
    }

    @Test
    void storesReceiverFeedbackOnceAndReturnsOnlyTheFirstThreeToSender() {
        DiaryShare share = approvedShare(10L, 1L);
        SharedDiaryLog delivery = SharedDiaryLog.create(user(2L), share, 1);
        ReflectionTestUtils.setField(delivery, "id", 20L);
        when(sharedDiaryLogRepository.findByIdAndReceiverId(20L, 2L)).thenReturn(Optional.of(delivery));

        ExperienceFragmentFeedbackResponse submitted = service.submitFeedback(
                2L, 20L, new ExperienceFragmentFeedbackRequest("경험을 읽고 제 상황을 다시 생각해 봤어요."));

        assertEquals("경험을 읽고 제 상황을 다시 생각해 봤어요.", submitted.content());
        assertNotNull(submitted.submittedAt());

        when(diaryShareRepository.findByIdWithDiaryAndUser(10L)).thenReturn(Optional.of(share));
        when(sharedDiaryLogRepository
                .findTop3ByDiaryShareIdAndFeedbackSummaryIsNotNullOrderByFeedbackSubmittedAtAscIdAsc(10L))
                .thenReturn(List.of(delivery));

        List<ExperienceFragmentFeedbackResponse> feedbacks = service.firstThreeFeedbacks(1L, 10L);

        assertEquals(1, feedbacks.size());
        assertFalse(feedbacks.get(0).content().isBlank());
    }

    private DiaryShare reviewRequiredShare(Long shareId, Long ownerId) {
        DiaryShare share = DiaryShare.request(diary(user(ownerId), 11L, "원문 일기"));
        ReflectionTestUtils.setField(share, "id", shareId);
        share.requireReview("익명화된 경험", "알바", List.of("알바"), "알바 경험");
        return share;
    }

    private DiaryShare approvedShare(Long shareId, Long ownerId) {
        DiaryShare share = reviewRequiredShare(shareId, ownerId);
        share.approve("[1.0,0.0]", "test");
        return share;
    }

    private AppUser user(Long id) {
        AppUser user = AppUser.createKakaoUser("provider-" + id, "사용자", null, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Diary diary(AppUser user, Long id, String content) {
        Diary diary = Diary.create(user, content, LocalDate.now());
        ReflectionTestUtils.setField(diary, "id", id);
        return diary;
    }
}
