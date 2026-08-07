package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import mutsa.hackathon.dto.MemoryCandidateListResponse;
import mutsa.hackathon.dto.MemoryCandidateResponse;
import mutsa.hackathon.dto.MemoryCandidateReviewRequest;
import mutsa.hackathon.dto.MemoryCandidateReviewResponse;
import mutsa.hackathon.dto.MemoryReviewStatus;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMemoryService {

    private final DiaryRepository diaryRepository;

    private final UserMemoryItemRepository
            userMemoryItemRepository;

    private final AiMemoryProfileService
            aiMemoryProfileService;

    @Transactional(readOnly = true)
    public MemoryCandidateListResponse getCandidates(
            Long userId,
            Long diaryId
    ) {
        Diary diary = findActiveOwnedDiary(
                userId,
                diaryId
        );

        List<UserMemoryItem> memories =
                findMemories(
                        userId,
                        diary.getId()
                );

        MemoryReviewStatus reviewStatus =
                resolveReviewStatus(memories);

        return new MemoryCandidateListResponse(
                diary.getId(),
                reviewStatus,
                reviewStatus
                        == MemoryReviewStatus.PENDING,
                toResponses(memories)
        );
    }

    @Transactional
    public MemoryCandidateReviewResponse
    reviewCandidates(
            Long userId,
            Long diaryId,
            MemoryCandidateReviewRequest request
    ) {
        if (
                request == null
                        || request.approved() == null
        ) {
            throw new IllegalArgumentException(
                    "기억 후보 승인 여부는 필수입니다."
            );
        }

        Diary diary = findActiveOwnedDiary(
                userId,
                diaryId
        );

        List<UserMemoryItem> memories =
                findMemories(
                        userId,
                        diary.getId()
                );

        if (memories.isEmpty()) {
            throw new ProjectException(
                    ErrorCode
                            .MEMORY_CANDIDATE_NOT_FOUND
            );
        }

        boolean approved =
                Boolean.TRUE.equals(
                        request.approved()
                );

        MemoryReviewStatus currentStatus =
                resolveReviewStatus(memories);

        MemoryReviewStatus targetStatus =
                approved
                        ? MemoryReviewStatus.APPROVED
                        : MemoryReviewStatus.REJECTED;

        if (currentStatus == targetStatus) {
            /*
             * 중복 승인 요청에서도 캐시가 누락되거나
             * 오래된 경우를 복구할 수 있도록 재생성
             */
            if (approved) {
                aiMemoryProfileService
                        .rebuildProfile(userId);
            }

            return new MemoryCandidateReviewResponse(
                    diary.getId(),
                    currentStatus,
                    0,
                    toResponses(memories)
            );
        }

        if (
                currentStatus
                        != MemoryReviewStatus.PENDING
        ) {
            throw new ProjectException(
                    ErrorCode
                            .MEMORY_REVIEW_ALREADY_COMPLETED
            );
        }

        if (
                approved
                        && !diary
                        .getUser()
                        .isAiMemoryConsent()
        ) {
            throw new ProjectException(
                    ErrorCode
                            .AI_MEMORY_CONSENT_REQUIRED
            );
        }

        if (approved) {
            memories.forEach(
                    UserMemoryItem::approve
            );

            /*
             * 상태 변경 내용을 프로필 재조회 전에
             * DB에 반영
             */
            userMemoryItemRepository.flush();

            aiMemoryProfileService
                    .rebuildProfile(userId);

        } else {
            memories.forEach(
                    UserMemoryItem::reject
            );
        }

        return new MemoryCandidateReviewResponse(
                diary.getId(),
                targetStatus,
                memories.size(),
                toResponses(memories)
        );
    }

    private Diary findActiveOwnedDiary(
            Long userId,
            Long diaryId
    ) {
        return diaryRepository
                .findByIdAndUserIdAndDeletedFalse(
                        diaryId,
                        userId
                )
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.DIARY_NOT_FOUND
                        )
                );
    }

    private List<UserMemoryItem> findMemories(
            Long userId,
            Long diaryId
    ) {
        return userMemoryItemRepository
                .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                        userId,
                        diaryId
                );
    }

    private List<MemoryCandidateResponse> toResponses(
            List<UserMemoryItem> memories
    ) {
        return memories.stream()
                .map(
                        MemoryCandidateResponse::from
                )
                .toList();
    }

    private MemoryReviewStatus resolveReviewStatus(
            List<UserMemoryItem> memories
    ) {
        if (memories.isEmpty()) {
            return MemoryReviewStatus.NONE;
        }

        Set<UserMemoryStatus> statuses =
                memories.stream()
                        .map(
                                UserMemoryItem::getStatus
                        )
                        .collect(
                                Collectors.toSet()
                        );

        if (statuses.size() > 1) {
            return MemoryReviewStatus.MIXED;
        }

        UserMemoryStatus status =
                statuses.iterator().next();

        return switch (status) {
            case PENDING ->
                    MemoryReviewStatus.PENDING;

            case APPROVED ->
                    MemoryReviewStatus.APPROVED;

            case REJECTED ->
                    MemoryReviewStatus.REJECTED;

            case REVOKED ->
                    MemoryReviewStatus.REVOKED;
        };
    }
}