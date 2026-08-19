package mutsa.hackathon.service;

import mutsa.hackathon.domain.*;
import mutsa.hackathon.dto.ExperienceMatchResponse;
import mutsa.hackathon.dto.ExperienceFragmentFeedbackRequest;
import mutsa.hackathon.dto.ReceivedExperienceFragmentListResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExperienceFragmentServiceTest {
    @Mock private DiaryRepository diaryRepository;
    @Mock private DiaryShareRepository diaryShareRepository;
    @Mock private ExperienceFragmentArrivalRepository experienceFragmentArrivalRepository;
    @Mock private SharedDiaryLogRepository sharedDiaryLogRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private CreditTransactionRepository creditTransactionRepository;
    @Mock private ExperienceFragmentProcessor experienceFragmentProcessor;
    @Mock private ExperienceStructureExtractor experienceStructureExtractor;
    @Mock private ExperienceEmbeddingGenerator experienceEmbeddingGenerator;
    @Mock private ExperienceFragmentDraftPersistenceService draftPersistenceService;
    @Mock private ExperienceFragmentApprovalPersistenceService approvalPersistenceService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ExperienceFragmentService service;

    @Test
    void prioritizesKeywordOverlapAfterSemanticSimilarityPassesThreshold() {
        ReflectionTestUtils.setField(service, "jsonMapper", JsonMapper.builder().build());
        ReflectionTestUtils.setField(service, "minimumSimilarity", 0.78d);

        AppUser receiver = user(1L);
        Diary queryDiary = diary(receiver, 10L, "알바에서 손님 응대 때문에 힘들었다.");
        returnsDiaryAsStructure(queryDiary);
        DiaryShare matching = approvedShare(20L, diary(user(2L), 21L, "source"), List.of("알바"), "[1.0,0.0]");
        DiaryShare unrelated = approvedShare(30L, diary(user(3L), 31L, "source"), List.of("운동"), "[1.0,0.0]");

        when(diaryRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(queryDiary));
        when(experienceEmbeddingGenerator.generate(queryDiary.getContent())).thenReturn(new ExperienceEmbedding("test", List.of(1.0, 0.0)));
        when(diaryShareRepository.findAllByShareStatus(DiaryShareStatus.APPROVED)).thenReturn(List.of(matching, unrelated));

        Optional<ExperienceMatchResponse> result = service.findBestMatch(1L, 10L);

        assertTrue(result.isPresent());
        assertEquals(20L, result.get().shareId());
        verify(sharedDiaryLogRepository).existsByReceiverIdAndDiaryShareId(1L, 20L);
        verify(sharedDiaryLogRepository).existsByReceiverIdAndDiaryShareId(1L, 30L);
    }

    @Test
    void matchesSemanticallySimilarFragmentEvenWhenGeneratedKeywordsDoNotAppearInDiaryText() {
        ReflectionTestUtils.setField(service, "jsonMapper", JsonMapper.builder().build());
        ReflectionTestUtils.setField(service, "minimumSimilarity", 0.78d);

        AppUser receiver = user(1L);
        Diary queryDiary = diary(receiver, 10L, "오늘 카페 알바에서 처음으로 혼자 마감을 했다.");
        returnsDiaryAsStructure(queryDiary);
        DiaryShare matching = approvedShare(
                20L,
                diary(user(2L), 21L, "source"),
                List.of("마감업무", "불안", "서비스업무"),
                "[1.0,0.0]"
        );

        when(diaryRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(queryDiary));
        when(experienceEmbeddingGenerator.generate(queryDiary.getContent()))
                .thenReturn(new ExperienceEmbedding("test", List.of(1.0, 0.0)));
        when(diaryShareRepository.findAllByShareStatus(DiaryShareStatus.APPROVED)).thenReturn(List.of(matching));

        Optional<ExperienceMatchResponse> result = service.findBestMatch(1L, 10L);

        assertTrue(result.isPresent());
        assertEquals(20L, result.get().shareId());
    }

    @Test
    void comparesTheReceiverStructureInsteadOfTheRawDiaryText() {
        ReflectionTestUtils.setField(service, "jsonMapper", JsonMapper.builder().build());
        ReflectionTestUtils.setField(service, "minimumSimilarity", 0.78d);

        AppUser receiver = user(1L);
        Diary queryDiary = diary(receiver, 10L, "면접 결과가 언제 나올지 몰라 메일을 계속 확인했다.");
        ExperienceStructure structure = new ExperienceStructure(
                "상황: 중요한 결과를 기다리는 중 | 핵심 어려움: 불확실성 | 반응: 반복 확인 | 영향 또는 변화: 집중이 흐트러짐",
                List.of("결과 기다림", "반복 확인")
        );
        DiaryShare matching = approvedShare(
                20L,
                diary(user(2L), 21L, "source"),
                List.of("결과 기다림", "반복 확인"),
                "[1.0,0.0]"
        );

        when(diaryRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(queryDiary));
        when(experienceStructureExtractor.extract(queryDiary.getContent())).thenReturn(structure);
        when(experienceEmbeddingGenerator.generate(structure.matchingText()))
                .thenReturn(new ExperienceEmbedding("test", List.of(1.0, 0.0)));
        when(diaryShareRepository.findAllByShareStatus(DiaryShareStatus.APPROVED)).thenReturn(List.of(matching));

        Optional<ExperienceMatchResponse> result = service.findBestMatch(1L, 10L);

        assertTrue(result.isPresent());
        verify(experienceEmbeddingGenerator).generate(structure.matchingText());
    }

    @Test
    void skipsPendingInboxCandidateAndSelectsTheNextEligibleMatch() {
        ReflectionTestUtils.setField(service, "jsonMapper", JsonMapper.builder().build());
        ReflectionTestUtils.setField(service, "minimumSimilarity", 0.78d);

        AppUser receiver = user(1L);
        Diary queryDiary = diary(receiver, 10L, "중요한 결과를 기다리며 계속 확인했다.");
        returnsDiaryAsStructure(queryDiary);
        DiaryShare pendingFirst = approvedShare(20L, diary(user(2L), 21L, "source"),
                List.of("결과 기다림"), "[1.0,0.0]");
        DiaryShare nextBest = approvedShare(30L, diary(user(3L), 31L, "source"),
                List.of("결과 기다림"), "[0.9,0.1]");

        when(diaryRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(queryDiary));
        when(experienceEmbeddingGenerator.generate(anyString()))
                .thenReturn(new ExperienceEmbedding("test", List.of(1.0, 0.0)));
        when(diaryShareRepository.findAllByShareStatus(DiaryShareStatus.APPROVED))
                .thenReturn(List.of(pendingFirst, nextBest));
        when(experienceFragmentArrivalRepository.existsByReceiverIdAndDiaryShareId(1L, 20L)).thenReturn(true);
        when(experienceFragmentArrivalRepository.existsByReceiverIdAndDiaryShareId(1L, 30L)).thenReturn(false);

        Optional<ExperienceMatchResponse> result = service.findBestMatch(1L, 10L);

        assertTrue(result.isPresent());
        assertEquals(30L, result.get().shareId());
    }

    @Test
    void deletedSourceDiaryIsExcludedFromExperienceMatching() {
        ReflectionTestUtils.setField(service, "jsonMapper", JsonMapper.builder().build());
        ReflectionTestUtils.setField(service, "minimumSimilarity", 0.78d);

        AppUser receiver = user(1L);
        Diary queryDiary = diary(receiver, 10L, "알바에서 손님 응대 때문에 힘들었다.");
        returnsDiaryAsStructure(queryDiary);
        Diary deletedSource = diary(user(2L), 21L, "source");
        DiaryShare matching = approvedShare(20L, deletedSource, List.of("알바"), "[1.0,0.0]");
        deletedSource.softDelete();

        when(diaryRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(queryDiary));
        when(experienceEmbeddingGenerator.generate(queryDiary.getContent())).thenReturn(new ExperienceEmbedding("test", List.of(1.0, 0.0)));
        when(diaryShareRepository.findAllByShareStatus(DiaryShareStatus.APPROVED)).thenReturn(List.of(matching));

        Optional<ExperienceMatchResponse> result = service.findBestMatch(1L, 10L);

        assertTrue(result.isEmpty());
        verify(sharedDiaryLogRepository, never()).existsByReceiverIdAndDiaryShareId(anyLong(), anyLong());
    }

    @Test
    void deletedSourceDiaryCannotBeReceivedWhileItIsInTrash() {
        AppUser receiver = user(1L);
        Diary deletedSource = diary(user(2L), 21L, "source");
        DiaryShare share = approvedShare(20L, deletedSource, List.of("알바"), "[1.0,0.0]");
        deletedSource.softDelete();

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(receiver));
        when(diaryShareRepository.findByIdWithDiaryAndUser(20L)).thenReturn(Optional.of(share));

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> service.receive(1L, 20L)
        );

        assertEquals(ErrorCode.SHARE_NOT_FOUND, exception.getErrorCode());
        verify(sharedDiaryLogRepository, never()).save(any());
        verify(creditTransactionRepository, never()).save(any());
    }

    @Test
    void persistsReviewDraftAfterAsyncGeneration() {
        DiaryShare requested = DiaryShare.request(diary(user(2L), 21L, "알바 일기"));
        ExperienceFragmentDraft draft = new ExperienceFragmentDraft(
                "익명화된 경험", "일과 관계", List.of("알바"), "알바 중 손님 응대 경험"
        );

        when(diaryShareRepository.findByIdWithDiaryAndUser(1L)).thenReturn(Optional.of(requested));
        when(experienceFragmentProcessor.createDraft("알바 일기")).thenReturn(draft);

        service.generateDraft(1L);

        verify(draftPersistenceService).saveDraft(1L, draft);
        verify(draftPersistenceService, never()).block(anyLong());
    }

    @Test
    void rejectsDuplicateExperienceFragmentRequestWithProjectException() {
        Diary diary = diary(user(1L), 10L, "shared diary");
        when(diaryRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(diary));
        when(diaryShareRepository.existsByDiaryId(10L)).thenReturn(true);

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> service.request(1L, 10L)
        );

        assertEquals(ErrorCode.SHARE_ALREADY_REQUESTED, exception.getErrorCode());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsUnavailableReviewStatusWithProjectException() {
        DiaryShare requested = DiaryShare.request(diary(user(1L), 10L, "shared diary"));
        when(diaryShareRepository.findByIdWithDiaryAndUser(20L)).thenReturn(Optional.of(requested));

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> service.approve(1L, 20L)
        );

        assertEquals(ErrorCode.SHARE_REVIEW_NOT_AVAILABLE, exception.getErrorCode());
        verifyNoInteractions(experienceEmbeddingGenerator, approvalPersistenceService);
    }

    @Test
    void storesOnlyAnInboxArrivalWhenAutomaticMatchingFindsAnEligibleFragment() {
        ReflectionTestUtils.setField(service, "jsonMapper", JsonMapper.builder().build());
        ReflectionTestUtils.setField(service, "minimumSimilarity", 0.78d);

        AppUser receiver = user(1L);
        Diary queryDiary = diary(receiver, 10L, "공원에서 곤충을 관찰했다.");
        returnsDiaryAsStructure(queryDiary);
        DiaryShare matching = approvedShare(20L, diary(user(2L), 21L, "source"), List.of("곤충"), "[1.0,0.0]");

        when(diaryShareRepository.existsByShareStatus(DiaryShareStatus.APPROVED)).thenReturn(true);
        when(diaryRepository.findByIdWithUser(10L)).thenReturn(Optional.of(queryDiary));
        when(diaryRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(queryDiary));
        when(experienceEmbeddingGenerator.generate(queryDiary.getContent()))
                .thenReturn(new ExperienceEmbedding("test", List.of(1.0, 0.0)));
        when(diaryShareRepository.findAllByShareStatus(DiaryShareStatus.APPROVED)).thenReturn(List.of(matching));
        when(diaryShareRepository.findByIdWithDiaryAndUser(20L)).thenReturn(Optional.of(matching));
        when(experienceFragmentArrivalRepository.existsByReceiverIdAndDiaryShareId(1L, 20L)).thenReturn(false);

        service.createInboxArrival(10L);

        verify(experienceFragmentArrivalRepository).save(argThat(arrival ->
                arrival.getReceiver().getId().equals(1L)
                        && arrival.getQueryDiary().getId().equals(10L)
                        && arrival.getDiaryShare().getId().equals(20L)
                        && arrival.getStatus() == ExperienceFragmentArrivalStatus.PENDING
        ));
        verify(appUserRepository, never()).findById(anyLong());
        verify(creditTransactionRepository, never()).save(any());
    }

    @Test
    void chargesCreditAndExposesContentOnlyWhenInboxArrivalIsReceived() {
        AppUser receiver = user(1L);
        receiver.addCredit(1);
        DiaryShare share = approvedShare(20L, diary(user(2L), 21L, "source"), List.of("곤충"), "[1.0,0.0]");
        ExperienceFragmentArrival arrival = ExperienceFragmentArrival.pending(
                receiver, diary(receiver, 10L, "곤충을 관찰했다."), share
        );

        when(experienceFragmentArrivalRepository.findByIdWithReceiverAndShare(30L))
                .thenReturn(Optional.of(arrival));
        when(sharedDiaryLogRepository.existsByReceiverIdAndDiaryShareId(1L, 20L)).thenReturn(false);
        when(sharedDiaryLogRepository.save(any(SharedDiaryLog.class)))
                .thenAnswer(invocation -> {
                    SharedDiaryLog delivery = invocation.getArgument(0);
                    ReflectionTestUtils.setField(delivery, "id", 40L);
                    return delivery;
                });

        service.receiveFromInbox(1L, 30L);

        assertEquals(0, receiver.getCredit());
        assertEquals(ExperienceFragmentArrivalStatus.RECEIVED, arrival.getStatus());
        verify(creditTransactionRepository).save(any(CreditTransaction.class));
    }

    @Test
    void returnsPersistedReceivedFragmentsWithFeedbackState() {
        AppUser receiver = user(1L);
        DiaryShare share = approvedShare(20L, diary(user(2L), 21L, "source"), List.of("topic"), "[1.0,0.0]");
        SharedDiaryLog delivery = SharedDiaryLog.create(receiver, share, 1);
        ReflectionTestUtils.setField(delivery, "id", 30L);
        LocalDateTime receivedAt = LocalDateTime.of(2026, 8, 18, 12, 30);
        ReflectionTestUtils.setField(delivery, "createdAt", receivedAt);
        delivery.recordFeedbackSummary("도움이 되었습니다.");
        when(sharedDiaryLogRepository.findAllReceivedByReceiverId(1L)).thenReturn(List.of(delivery));

        List<ReceivedExperienceFragmentListResponse> result = service.received(1L);

        assertEquals(1, result.size());
        ReceivedExperienceFragmentListResponse received = result.get(0);
        assertEquals(30L, received.deliveryId());
        assertEquals(20L, received.shareId());
        assertEquals(receivedAt, received.receivedAt());
        assertTrue(received.feedbackSubmitted());
        assertNotNull(received.feedbackSubmittedAt());
    }

    @Test
    void rejectsDuplicateFeedbackWithProjectException() {
        AppUser receiver = user(1L);
        DiaryShare share = approvedShare(20L, diary(user(2L), 21L, "source"), List.of("topic"), "[1.0,0.0]");
        SharedDiaryLog delivery = SharedDiaryLog.create(receiver, share, 1);
        delivery.recordFeedbackSummary("이미 보낸 반응");
        when(sharedDiaryLogRepository.findByIdAndReceiverId(30L, 1L)).thenReturn(Optional.of(delivery));

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> service.submitFeedback(1L, 30L, new ExperienceFragmentFeedbackRequest("새 반응"))
        );

        assertEquals(ErrorCode.SHARED_DIARY_FEEDBACK_ALREADY_SUBMITTED, exception.getErrorCode());
    }

    @Test
    void removesStaleArrivalFromInboxWhenSourceShareIsWithdrawn() {
        AppUser receiver = user(1L);
        DiaryShare share = approvedShare(20L, diary(user(2L), 21L, "source"), List.of("topic"), "[1.0,0.0]");
        share.withdraw();
        ExperienceFragmentArrival arrival = ExperienceFragmentArrival.pending(
                receiver, diary(receiver, 10L, "query"), share
        );
        when(experienceFragmentArrivalRepository.findAllByReceiverIdAndStatusWithShare(
                1L, ExperienceFragmentArrivalStatus.PENDING
        )).thenReturn(List.of(arrival));

        assertTrue(service.inbox(1L).isEmpty());

        verify(experienceFragmentArrivalRepository).deleteAll(List.of(arrival));
    }

    @Test
    void removesStaleArrivalBeforeCreditIsCharged() {
        AppUser receiver = user(1L);
        receiver.addCredit(1);
        DiaryShare share = approvedShare(20L, diary(user(2L), 21L, "source"), List.of("topic"), "[1.0,0.0]");
        share.withdraw();
        ExperienceFragmentArrival arrival = ExperienceFragmentArrival.pending(
                receiver, diary(receiver, 10L, "query"), share
        );
        when(experienceFragmentArrivalRepository.findByIdWithReceiverAndShare(30L))
                .thenReturn(Optional.of(arrival));

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> service.receiveFromInbox(1L, 30L)
        );

        assertEquals(ErrorCode.SHARED_DIARY_NOT_AVAILABLE, exception.getErrorCode());
        assertEquals(1, receiver.getCredit());
        verify(experienceFragmentArrivalRepository).delete(arrival);
        verify(sharedDiaryLogRepository, never()).save(any());
        verify(creditTransactionRepository, never()).save(any());
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

    private DiaryShare approvedShare(Long id, Diary diary, List<String> keywords, String embedding) {
        DiaryShare share = DiaryShare.request(diary);
        ReflectionTestUtils.setField(share, "id", id);
        share.requireReview("익명화된 경험", "일과 관계", keywords, "알바 경험");
        share.approve(embedding, "test");
        return share;
    }

    private void returnsDiaryAsStructure(Diary diary) {
        when(experienceStructureExtractor.extract(diary.getContent()))
                .thenReturn(new ExperienceStructure(diary.getContent(), List.of()));
    }
}
