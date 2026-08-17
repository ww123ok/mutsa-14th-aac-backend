package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.*;
import mutsa.hackathon.dto.*;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperienceFragmentService {
    private static final int RECEIVE_COST = 1;
    private static final double KEYWORD_OVERLAP_BONUS = 0.02d;

    private final DiaryRepository diaryRepository;
    private final DiaryShareRepository diaryShareRepository;
    private final ExperienceFragmentArrivalRepository experienceFragmentArrivalRepository;
    private final SharedDiaryLogRepository sharedDiaryLogRepository;
    private final AppUserRepository appUserRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final ExperienceFragmentProcessor experienceFragmentProcessor;
    private final ExperienceEmbeddingGenerator experienceEmbeddingGenerator;
    private final ExperienceFragmentDraftPersistenceService draftPersistenceService;
    private final ExperienceFragmentApprovalPersistenceService approvalPersistenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final JsonMapper jsonMapper;

    @Value("${app.experience-sharing.minimum-similarity:0.78}")
    private double minimumSimilarity;

    @Value("${app.experience-sharing.auto-approval-days:5}")
    private int autoApprovalDays;

    @Transactional
    public ExperienceFragmentResponse request(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByIdAndUserIdAndDeletedFalse(diaryId, userId)
                .orElseThrow(() -> new ProjectException(ErrorCode.DIARY_NOT_FOUND));
        if (diaryShareRepository.existsByDiaryId(diaryId)) {
            throw new ProjectException(ErrorCode.SHARE_ALREADY_REQUESTED);
        }
        DiaryShare share = diaryShareRepository.save(DiaryShare.request(diary));
        eventPublisher.publishEvent(new ExperienceFragmentGenerationRequested(share.getId()));
        return ExperienceFragmentResponse.from(share);
    }

    /** External AI work runs outside a database transaction. */
    public void generateDraft(Long shareId) {
        DiaryShare share = diaryShareRepository.findByIdWithDiaryAndUser(shareId).orElse(null);
        if (share == null || share.getShareStatus() != DiaryShareStatus.REQUESTED) return;
        try {
            ExperienceFragmentDraft draft = experienceFragmentProcessor.createDraft(share.getDiary().getContent());
            draftPersistenceService.saveDraft(shareId, draft);
        } catch (RuntimeException exception) {
            log.warn("Experience fragment generation failed: shareId={}, reason={}", shareId,
                    exception.getClass().getSimpleName());
            draftPersistenceService.block(shareId);
        }
    }

    /** Embedding is generated only after the sender confirms the anonymized draft. */
    public ExperienceFragmentResponse approve(Long userId, Long shareId) {
        DiaryShare share = ownedShare(userId, shareId);
        return approveReviewRequiredShare(share, userId);
    }

    @Transactional
    public ExperienceFragmentResponse reject(Long userId, Long shareId) {
        DiaryShare share = ownedShare(userId, shareId);
        requireReviewRequired(share);
        share.reject(null);
        return ExperienceFragmentResponse.from(share);
    }

    @Transactional(readOnly = true)
    public List<ExperienceFragmentResponse> mine(Long userId) {
        return diaryShareRepository.findAllByDiaryUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ExperienceFragmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ExperienceFragmentReviewResponse review(Long userId, Long shareId) {
        return ExperienceFragmentReviewResponse.from(ownedShare(userId, shareId));
    }

    /**
     * Approves only drafts that have remained reviewable for the configured period.
     * The embedding request deliberately stays outside a transaction.
     */
    public void autoApproveExpiredReviews() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(autoApprovalDays);
        diaryShareRepository.findIdsReadyForAutoApproval(DiaryShareStatus.REVIEW_REQUIRED, cutoff)
                .forEach(this::autoApprove);
    }

    @Transactional
    public ExperienceFragmentFeedbackResponse submitFeedback(
            Long receiverId,
            Long deliveryId,
            ExperienceFragmentFeedbackRequest request
    ) {
        SharedDiaryLog delivery = sharedDiaryLogRepository.findByIdAndReceiverId(deliveryId, receiverId)
                .orElseThrow(() -> new ProjectException(ErrorCode.SHARED_DIARY_NOT_AVAILABLE));
        if (delivery.hasFeedbackSubmitted()) {
            throw new ProjectException(ErrorCode.SHARED_DIARY_FEEDBACK_ALREADY_SUBMITTED);
        }
        delivery.recordFeedbackSummary(request.content());

        eventPublisher.publishEvent(
                InAppNotificationRequested.experienceFragmentFeedback(
                        delivery.getDiaryShare().getDiary().getUser().getId(),
                        delivery.getId(),
                        delivery.getDiaryShare().getId()
                )
        );

        return ExperienceFragmentFeedbackResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public List<ReceivedExperienceFragmentListResponse> received(Long receiverId) {
        return sharedDiaryLogRepository.findAllReceivedByReceiverId(receiverId)
                .stream()
                .map(ReceivedExperienceFragmentListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExperienceFragmentFeedbackResponse> firstThreeFeedbacks(Long senderId, Long shareId) {
        ownedShare(senderId, shareId);
        return sharedDiaryLogRepository
                .findTop3ByDiaryShareIdAndFeedbackSummaryIsNotNullOrderByFeedbackSubmittedAtAscIdAsc(shareId)
                .stream()
                .map(ExperienceFragmentFeedbackResponse::from)
                .toList();
    }

    /**
     * Hybrid matching: semantic similarity decides eligibility and keyword overlap
     * only improves ranking. Keywords generated for identical diaries can differ,
     * so they must not exclude an otherwise highly similar experience.
     */
    @Transactional(readOnly = true)
    public Optional<ExperienceMatchResponse> findBestMatch(Long receiverId, Long diaryId) {
        Diary queryDiary = diaryRepository.findByIdAndUserIdAndDeletedFalse(diaryId, receiverId)
                .orElseThrow(() -> new ProjectException(ErrorCode.DIARY_NOT_FOUND));
        ExperienceEmbedding queryEmbedding = experienceEmbeddingGenerator.generate(queryDiary.getContent());
        String queryText = queryDiary.getContent().toLowerCase(Locale.ROOT);
        return diaryShareRepository.findAllByShareStatus(DiaryShareStatus.APPROVED).stream()
                .filter(share -> !share.getDiary().isDeleted())
                .filter(share -> !share.getDiary().getUser().getId().equals(receiverId))
                .filter(share -> !sharedDiaryLogRepository.existsByReceiverIdAndDiaryShareId(receiverId, share.getId()))
                .map(share -> ranked(share, queryEmbedding.values(), queryText))
                .filter(Objects::nonNull)
                .filter(match -> match.response().similarity() >= minimumSimilarity)
                .max(Comparator.comparingDouble(RankedExperienceMatch::rankingScore))
                .map(RankedExperienceMatch::response);
    }

    /**
     * Stores only a private arrival notice. No credit is charged and no anonymized
     * diary content is exposed until receiveFromInbox is called.
     */
    @Transactional
    public void createInboxArrival(Long diaryId) {
        if (!diaryShareRepository.existsByShareStatus(DiaryShareStatus.APPROVED)) {
            return;
        }

        Diary queryDiary = diaryRepository.findByIdWithUser(diaryId)
                .filter(diary -> !diary.isDeleted())
                .orElse(null);
        if (queryDiary == null) {
            return;
        }

        Long receiverId = queryDiary.getUser().getId();
        Optional<ExperienceMatchResponse> match = findBestMatch(receiverId, diaryId);
        if (match.isEmpty()
                || experienceFragmentArrivalRepository.existsByReceiverIdAndDiaryShareId(receiverId, match.get().shareId())) {
            return;
        }

        DiaryShare share = diaryShareRepository.findByIdWithDiaryAndUser(match.get().shareId())
                .orElse(null);
        if (share == null || share.getShareStatus() != DiaryShareStatus.APPROVED) {
            return;
        }

        ExperienceFragmentArrival arrival =
                ExperienceFragmentArrival.pending(
                        queryDiary.getUser(),
                        queryDiary,
                        share
                );
        experienceFragmentArrivalRepository.save(arrival);

        eventPublisher.publishEvent(
                InAppNotificationRequested.experienceFragmentArrived(
                        receiverId,
                        arrival.getId()
                )
        );
    }

    @Transactional
    public List<ExperienceFragmentInboxResponse> inbox(Long receiverId) {
        List<ExperienceFragmentArrival> arrivals = experienceFragmentArrivalRepository
                .findAllByReceiverIdAndStatusWithShare(receiverId, ExperienceFragmentArrivalStatus.PENDING)
                .stream()
                .toList();

        List<ExperienceFragmentArrival> staleArrivals = arrivals.stream()
                .filter(arrival -> !isDeliverableArrival(arrival))
                .toList();
        if (!staleArrivals.isEmpty()) {
            experienceFragmentArrivalRepository.deleteAll(staleArrivals);
        }

        return arrivals.stream()
                .filter(this::isDeliverableArrival)
                .map(ExperienceFragmentInboxResponse::from)
                .toList();
    }

    @Transactional
    public ReceivedExperienceFragmentResponse receive(Long receiverId, Long shareId) {
        AppUser receiver = appUserRepository.findById(receiverId).orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));
        DiaryShare share = diaryShareRepository.findByIdWithDiaryAndUser(shareId)
                .filter(candidate -> candidate.getShareStatus() == DiaryShareStatus.APPROVED)
                .filter(candidate -> !candidate.getDiary().isDeleted())
                .orElseThrow(() -> new ProjectException(ErrorCode.SHARE_NOT_FOUND));
        if (sharedDiaryLogRepository.existsByReceiverIdAndDiaryShareId(receiverId, shareId)) {
            throw new ProjectException(ErrorCode.SHARED_DIARY_ALREADY_RECEIVED);
        }
        return deliver(receiver, share);
    }

    @Transactional
    public ReceivedExperienceFragmentResponse receiveFromInbox(Long receiverId, Long arrivalId) {
        ExperienceFragmentArrival arrival = experienceFragmentArrivalRepository
                .findByIdWithReceiverAndShare(arrivalId)
                .filter(candidate -> candidate.getReceiver().getId().equals(receiverId))
                .filter(candidate -> candidate.getStatus() == ExperienceFragmentArrivalStatus.PENDING)
                .orElseThrow(() -> new ProjectException(ErrorCode.SHARED_DIARY_NOT_AVAILABLE));

        if (!isDeliverableArrival(arrival)) {
            experienceFragmentArrivalRepository.delete(arrival);
            throw new ProjectException(ErrorCode.SHARED_DIARY_NOT_AVAILABLE);
        }

        ReceivedExperienceFragmentResponse response = deliver(
                arrival.getReceiver(),
                arrival.getDiaryShare()
        );
        arrival.markReceived();
        return response;
    }

    private ReceivedExperienceFragmentResponse deliver(AppUser receiver, DiaryShare share) {
        if (!isDeliverableShare(share)) {
            throw new ProjectException(ErrorCode.SHARED_DIARY_NOT_AVAILABLE);
        }
        if (sharedDiaryLogRepository.existsByReceiverIdAndDiaryShareId(receiver.getId(), share.getId())) {
            throw new ProjectException(ErrorCode.SHARED_DIARY_ALREADY_RECEIVED);
        }
        try {
            receiver.useCredit(RECEIVE_COST);
        } catch (IllegalStateException exception) {
            throw new ProjectException(ErrorCode.INSUFFICIENT_CREDIT);
        }
        SharedDiaryLog delivery = sharedDiaryLogRepository.save(SharedDiaryLog.create(receiver, share, RECEIVE_COST));
        creditTransactionRepository.save(CreditTransaction.create(receiver, CreditTransactionType.SHARE_RECEIVE, -RECEIVE_COST,
                receiver.getCredit(), CreditReferenceType.SHARED_DIARY_LOG, delivery.getId(), "Experience fragment received"));
        return new ReceivedExperienceFragmentResponse(delivery.getId(), share.getId(), share.getAnonymizedContent(),
                share.getGeneralTopic(), List.copyOf(share.getKeywords()), receiver.getCredit());
    }

    private ExperienceMatchResponse scored(DiaryShare share, List<Double> query) {
        try {
            List<?> raw = jsonMapper.readValue(share.getEmbeddingJson(), List.class);
            List<Double> candidate = raw.stream().filter(Number.class::isInstance).map(Number.class::cast).map(Number::doubleValue).toList();
            if (candidate.size() != query.size()) return null;
            return new ExperienceMatchResponse(share.getId(), share.getGeneralTopic(), List.copyOf(share.getKeywords()), cosine(query, candidate));
        } catch (Exception exception) { return null; }
    }

    private RankedExperienceMatch ranked(
            DiaryShare share,
            List<Double> query,
            String queryText
    ) {
        ExperienceMatchResponse response = scored(share, query);
        if (response == null) {
            return null;
        }

        boolean hasKeywordOverlap = share.getKeywords().stream()
                .filter(Objects::nonNull)
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(queryText::contains);

        double rankingScore = response.similarity()
                + (hasKeywordOverlap ? KEYWORD_OVERLAP_BONUS : 0d);

        return new RankedExperienceMatch(response, rankingScore);
    }

    private record RankedExperienceMatch(
            ExperienceMatchResponse response,
            double rankingScore
    ) {
    }

    private ExperienceFragmentResponse approveReviewRequiredShare(DiaryShare share, Long userId) {
        requireReviewRequired(share);
        ExperienceEmbedding embedding = experienceEmbeddingGenerator.generate(share.getMatchingText());
        return approvalPersistenceService.approve(userId, share.getId(), embedding);
    }

    private void autoApprove(Long shareId) {
        DiaryShare share = diaryShareRepository.findByIdWithDiaryAndUser(shareId).orElse(null);
        if (share == null || share.getShareStatus() != DiaryShareStatus.REVIEW_REQUIRED) {
            return;
        }
        LocalDateTime reviewAvailableAt = Optional.ofNullable(share.getReviewAvailableAt())
                .orElse(share.getCreatedAt());
        if (reviewAvailableAt.isAfter(LocalDateTime.now().minusDays(autoApprovalDays))) {
            return;
        }

        try {
            approveReviewRequiredShare(share, share.getDiary().getUser().getId());
            log.info("Experience fragment auto-approved: shareId={}", shareId);
        } catch (RuntimeException exception) {
            log.warn("Experience fragment auto-approval failed: shareId={}, reason={}", shareId,
                    exception.getClass().getSimpleName());
        }
    }

    private double cosine(List<Double> left, List<Double> right) {
        double dot = 0, leftNorm = 0, rightNorm = 0;
        for (int i = 0; i < left.size(); i++) { dot += left.get(i) * right.get(i); leftNorm += left.get(i) * left.get(i); rightNorm += right.get(i) * right.get(i); }
        return leftNorm == 0 || rightNorm == 0 ? 0 : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private boolean isDeliverableArrival(ExperienceFragmentArrival arrival) {
        return arrival.getStatus() == ExperienceFragmentArrivalStatus.PENDING
                && !arrival.getQueryDiary().isDeleted()
                && isDeliverableShare(arrival.getDiaryShare());
    }

    private boolean isDeliverableShare(DiaryShare share) {
        return share.getShareStatus() == DiaryShareStatus.APPROVED
                && !share.getDiary().isDeleted();
    }

    private void requireReviewRequired(DiaryShare share) {
        if (share.getShareStatus() != DiaryShareStatus.REVIEW_REQUIRED) {
            throw new ProjectException(ErrorCode.SHARE_REVIEW_NOT_AVAILABLE);
        }
    }

    private DiaryShare ownedShare(Long userId, Long shareId) {
        return diaryShareRepository.findByIdWithDiaryAndUser(shareId)
                .filter(share -> share.getDiary().getUser().getId().equals(userId))
                .orElseThrow(() -> new ProjectException(ErrorCode.SHARE_NOT_FOUND));
    }

}
