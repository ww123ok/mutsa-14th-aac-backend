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

    private final DiaryRepository diaryRepository;
    private final DiaryShareRepository diaryShareRepository;
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
        return approvalPersistenceService.approve(userId, shareId, embedding);
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


    @Transactional(readOnly = true)
    public ExperienceFragmentReviewResponse review(Long userId, Long shareId) {
        return ExperienceFragmentReviewResponse.from(ownedShare(userId, shareId));
    }
    /** Hybrid matching: exact approved-keyword overlap narrows candidates, cosine similarity ranks them. */
    @Transactional(readOnly = true)
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

    @Transactional
    public ReceivedExperienceFragmentResponse receive(Long receiverId, Long shareId) {
        AppUser receiver = appUserRepository.findById(receiverId).orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));
        DiaryShare share = diaryShareRepository.findByIdWithDiaryAndUser(shareId)
                .filter(candidate -> candidate.getShareStatus() == DiaryShareStatus.APPROVED)
                .orElseThrow(() -> new ProjectException(ErrorCode.SHARE_NOT_FOUND));
        if (sharedDiaryLogRepository.existsByReceiverIdAndDiaryShareId(receiverId, shareId)) {
            throw new ProjectException(ErrorCode.SHARED_DIARY_ALREADY_RECEIVED);
        }
        try { receiver.useCredit(RECEIVE_COST); }
        catch (IllegalStateException exception) { throw new ProjectException(ErrorCode.INSUFFICIENT_CREDIT); }
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

}
