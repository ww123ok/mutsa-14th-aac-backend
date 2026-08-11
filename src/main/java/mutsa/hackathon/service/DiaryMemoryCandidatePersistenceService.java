package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import mutsa.hackathon.util.MemoryHashGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DiaryMemoryCandidatePersistenceService {

    /**
     * 최신 기획에서 최근 맥락은
     * 약 1주일을 기준으로 질문에 활용
     */
    private static final int
            RECENT_MEMORY_TTL_DAYS = 7;

    /**
     * 한 일기에서 너무 많은 개인정보를
     * 기억 후보로 만들지 않도록 제한
     */
    private static final int
            MAX_CANDIDATE_COUNT_PER_DIARY = 5;

    private final DiaryRepository
            diaryRepository;

    private final UserMemoryItemRepository
            userMemoryItemRepository;

    private final DiaryMemoryDuplicateGuard
            diaryMemoryDuplicateGuard;

    /**
     * AI가 추출한 기억 후보를 PENDING 상태로 저장.
     * STABLE:
     * expiresAt = null
     * RECENT:
     * expiresAt = 현재 + 7일
     * 저장 직전에 온보딩 직업 및 기존 승인 기억과
     * 명백하게 중복되는 후보를 한 번 더 차단.
     */
    @Transactional
    public int saveCandidates(
            Long diaryId,
            List<DiaryMemoryCandidate> candidates
    ) {

        if (
                candidates == null
                        || candidates.isEmpty()
        ) {
            return 0;
        }

        Diary diary =
                diaryRepository
                        .findByIdWithUser(
                                diaryId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 일기입니다."
                                )
                        );

        if (diary.isDeleted()) {
            return 0;
        }

        if (
                !diary.getUser()
                        .isAiMemoryConsent()
        ) {
            return 0;
        }

        LocalDateTime now =
                LocalDateTime.now();

        List<UserMemoryItem> existingMemories =
                new ArrayList<>(
                        userMemoryItemRepository
                                .findActiveApprovedMemories(
                                        diary.getUser()
                                                .getId(),
                                        UserMemoryStatus.APPROVED,
                                        now
                                )
                );

        int savedCount = 0;

        Set<String> hashesInCurrentRequest =
                new HashSet<>();

        Set<String> comparisonKeysInCurrentRequest =
                new HashSet<>();

        List<DiaryMemoryCandidate> limitedCandidates =
                candidates.stream()
                        .limit(
                                MAX_CANDIDATE_COUNT_PER_DIARY
                        )
                        .toList();

        for (
                DiaryMemoryCandidate candidate
                : limitedCandidates
        ) {
            String comparisonKey =
                    diaryMemoryDuplicateGuard
                            .createCandidateKey(
                                    candidate
                            );

            /*
             * 같은 AI 응답 안에서 공백이나 문장부호만
             * 달라진 후보도 중복으로 봄
             */
            if (
                    !comparisonKeysInCurrentRequest
                            .add(comparisonKey)
            ) {
                continue;
            }

            /*
             * 온보딩 직업 또는 기존 승인 기억과
             * 명백하게 같은 사실이면 저장하지 않음
             */
            if (
                    diaryMemoryDuplicateGuard
                            .isDuplicate(
                                    diary.getUser(),
                                    candidate,
                                    existingMemories
                            )
            ) {
                continue;
            }

            String contentHash =
                    MemoryHashGenerator.generate(
                            candidate.category(),
                            candidate.memoryText()
                    );

            /*
             * 기존 exact hash 중복 방어도 유지
             */
            if (
                    !hashesInCurrentRequest.add(
                            contentHash
                    )
            ) {
                continue;
            }

            if (
                    userMemoryItemRepository
                            .existsByUserIdAndContentHash(
                                    diary.getUser()
                                            .getId(),
                                    contentHash
                            )
            ) {
                continue;
            }

            UserMemoryCategory category =
                    candidate.category();

            LocalDateTime expiresAt =
                    category.isRecent()
                            ? now.plusDays(
                            RECENT_MEMORY_TTL_DAYS
                    )
                            : null;

            UserMemoryItem savedMemory =
                    userMemoryItemRepository.save(
                            UserMemoryItem.createCandidate(
                                    diary.getUser(),
                                    diary,
                                    category,
                                    candidate.memoryText(),
                                    contentHash,
                                    expiresAt
                            )
                    );

            /*
             * 같은 요청 뒤쪽 후보가 방금 저장한 후보의
             * 사실상 중복인 경우도 잡을 수 있게 목록에 추가.
             * 아직 PENDING이어도 이 요청 안에서는 비교 대상으로 사용.
             */
            existingMemories.add(savedMemory);

            savedCount++;
        }

        return savedCount;
    }
}