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

    /**
     * 특정 일기에서 발견된 기억 후보들을 조회.
     * 후보가 없는 경우에도 오류를 발생시키지 않고
     * 빈 배열과 NONE 상태를 반환.
     */
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

    /**
     * 한 일기에서 발견된 모든 PENDING 기억 후보를
     * 한 번에 승인하거나 거절
     */
    @Transactional
    public MemoryCandidateReviewResponse reviewCandidates(
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

        /*
         * 같은 요청이 네트워크 재시도 등으로 반복된 경우,
         * 오류를 발생시키지 않고 멱등하게 처리
         */
        if (currentStatus == targetStatus) {
            return new MemoryCandidateReviewResponse(
                    diary.getId(),
                    currentStatus,
                    0,
                    toResponses(memories)
            );
        }

        /*
         * 이미 반대 결정으로 검토가 완료되었거나
         * 후보 상태가 섞여 있으면 결정을 변경하지 않음.
         */
        if (
                currentStatus
                        != MemoryReviewStatus.PENDING
        ) {
            throw new ProjectException(
                    ErrorCode
                            .MEMORY_REVIEW_ALREADY_COMPLETED
            );
        }

        /*
         * 거절은 언제든 가능하지만,
         * 향후 질문에 활용하는 승인은
         * 전역 AI 기억 동의가 켜져 있어야 함.
         */
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
                .map(MemoryCandidateResponse::from)
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
                        .map(UserMemoryItem::getStatus)
                        .collect(Collectors.toSet());

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