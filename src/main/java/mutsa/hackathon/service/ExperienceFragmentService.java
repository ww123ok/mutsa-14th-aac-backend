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

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperienceFragmentService {
    private static final int RECEIVE_COST = 1;
    private static final int SHARE_REWARD_INTERVAL = 3;

    private final DiaryRepository diaryRepository;
    private final DiaryShareRepository diaryShareRepository;
    private final ExperienceFragmentArrivalRepository experienceFragmentArrivalRepository;
    private final SharedDiaryLogRepository sharedDiaryLogRepository;
    private final AppUserRepository appUserRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final ExperienceFragmentProcessor experienceFragmentProcessor;
    private final ExperienceEmbeddingGenerator experienceEmbeddingGenerator;
    private final ExperienceFragmentDraftPersistenceService draftPersistenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final JsonMapper jsonMapper;

    @Value("${app.experience-sharing.minimum-similarity:0.78}")
    private double minimumSimilarity;

    @Transactional
    public ExperienceFragmentResponse request(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByIdAndUserIdAndDeletedFalse(diaryId, userId)
                .orElseThrow(() -> new ProjectException(ErrorCode.DIARY_NOT_FOUND));
        if (diaryShareRepository.existsByDiaryId(diaryId)) {
            throw new IllegalStateException("An experience fragment already exists for this diary.");
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
        if (share.getShareStatus() != DiaryShareStatus.REVIEW_REQUIRED) throw new IllegalStateException("This fragment cannot be approved.");
        ExperienceEmbedding embedding = experienceEmbeddingGenerator.generate(share.getMatchingText());
        return persistApproval(userId, shareId, embedding);
    }

    @Transactional
    public ExperienceFragmentResponse persistApproval(Long userId, Long shareId, ExperienceEmbedding embedding) {
        DiaryShare share = ownedShare(userId, shareId);
        if (share.getShareStatus() != DiaryShareStatus.REVIEW_REQUIRED) throw new IllegalStateException("This fragment cannot be approved.");
        try {
            share.approve(jsonMapper.writeValueAsString(embedding.values()), embedding.model());
        } catch (Exception exception) {
            throw new IllegalStateException("Embedding could not be stored.", exception);
        }
        rewardEveryThirdShare(share);
        return ExperienceFragmentResponse.from(share);
    }

    @Transactional
    public ExperienceFragmentResponse reject(Long userId, Long shareId) {
        DiaryShare share = ownedShare(userId, shareId);
        share.reject(null);
        return ExperienceFragmentResponse.from(share);
    }

    @Transactional(readOnly = true)
    public List<ExperienceFragmentResponse> mine(Long userId) {
        return diaryShareRepository.findAllByDiaryUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ExperienceFragmentResponse::from).toList();
    }

    /** Hybrid matching: exact approved-keyword overlap narrows candidates, cosine similarity ranks them. */
    public Optional<ExperienceMatchResponse> findBestMatch(Long receiverId, Long diaryId) {
        Diary queryDiary = diaryRepository.findByIdAndUserIdAndDeletedFalse(diaryId, receiverId)
                .orElseThrow(() -> new ProjectException(ErrorCode.DIARY_NOT_FOUND));
        ExperienceEmbedding queryEmbedding = experienceEmbeddingGenerator.generate(queryDiary.getContent());
        String queryText = queryDiary.getContent().toLowerCase(Locale.ROOT);
        return diaryShareRepository.findAllByShareStatus(DiaryShareStatus.APPROVED).stream()
                .filter(share -> !share.getDiary().getUser().getId().equals(receiverId))
                .filter(share -> share.getKeywords().stream().anyMatch(keyword -> queryText.contains(keyword.toLowerCase(Locale.ROOT))))
                .filter(share -> !sharedDiaryLogRepository.existsByReceiverIdAndDiaryShareId(receiverId, share.getId()))
                .map(share -> scored(share, queryEmbedding.values()))
                .filter(Objects::nonNull)
                .filter(match -> match.similarity() >= minimumSimilarity)
                .max(Comparator.comparingDouble(ExperienceMatchResponse::similarity));
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

        experienceFragmentArrivalRepository.save(
                ExperienceFragmentArrival.pending(queryDiary.getUser(), queryDiary, share)
        );
    }

    @Transactional(readOnly = true)
    public List<ExperienceFragmentInboxResponse> inbox(Long receiverId) {
        return experienceFragmentArrivalRepository
                .findAllByReceiverIdAndStatusWithShare(receiverId, ExperienceFragmentArrivalStatus.PENDING)
                .stream()
                .map(ExperienceFragmentInboxResponse::from)
                .toList();
    }

    @Transactional
    public ReceivedExperienceFragmentResponse receive(Long receiverId, Long shareId) {
        AppUser receiver = appUserRepository.findById(receiverId).orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));
        DiaryShare share = diaryShareRepository.findByIdWithDiaryAndUser(shareId)
                .filter(candidate -> candidate.getShareStatus() == DiaryShareStatus.APPROVED)
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

        ReceivedExperienceFragmentResponse response = deliver(
                arrival.getReceiver(),
                arrival.getDiaryShare()
        );
        arrival.markReceived();
        return response;
    }

    private ReceivedExperienceFragmentResponse deliver(AppUser receiver, DiaryShare share) {
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

    private double cosine(List<Double> left, List<Double> right) {
        double dot = 0, leftNorm = 0, rightNorm = 0;
        for (int i = 0; i < left.size(); i++) { dot += left.get(i) * right.get(i); leftNorm += left.get(i) * left.get(i); rightNorm += right.get(i) * right.get(i); }
        return leftNorm == 0 || rightNorm == 0 ? 0 : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private DiaryShare ownedShare(Long userId, Long shareId) {
        return diaryShareRepository.findByIdWithDiaryAndUser(shareId)
                .filter(share -> share.getDiary().getUser().getId().equals(userId))
                .orElseThrow(() -> new ProjectException(ErrorCode.SHARE_NOT_FOUND));
    }

    private void rewardEveryThirdShare(DiaryShare share) {
        long approved = diaryShareRepository.findAllByDiaryUserIdOrderByCreatedAtDesc(share.getDiary().getUser().getId()).stream()
                .filter(item -> item.getShareStatus() == DiaryShareStatus.APPROVED).count();
        if (approved % SHARE_REWARD_INTERVAL != 0) return;
        AppUser sender = share.getDiary().getUser();
        sender.addCredit(1); share.markRewarded(1);
        creditTransactionRepository.save(CreditTransaction.create(sender, CreditTransactionType.SHARE_REWARD, 1, sender.getCredit(),
                CreditReferenceType.DIARY_SHARE, share.getId(), "Three experience fragments shared"));
    }
}
