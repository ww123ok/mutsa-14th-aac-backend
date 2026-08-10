package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import mutsa.hackathon.util.MemoryHashGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * AI가 추출한 기억 후보를
     * PENDING 상태로 저장.
     * STABLE:
     * expiresAt = null
     * RECENT:
     * expiresAt = 현재 + 7일
     * 아직 PENDING이므로 실제 작성 도움 질문에는
     * 사용되지 않음.
     * 사용자가 승인한 후보만 향후 프로필에 들어감.
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

        /*
         * 삭제된 일기에서 새 기억을 만들지 않음
         */
        if (diary.isDeleted()) {
            return 0;
        }

        /*
         * 전역 AI 기억 활용에 동의하지 않은
         * 사용자에게는 기억 후보 자체를 저장하지 않음
         */
        if (
                !diary.getUser()
                        .isAiMemoryConsent()
        ) {
            return 0;
        }

        LocalDateTime now =
                LocalDateTime.now();

        int savedCount = 0;

        Set<String> hashesInCurrentRequest =
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
            String contentHash =
                    MemoryHashGenerator.generate(
                            candidate.category(),
                            candidate.memoryText()
                    );

            /*
             * AI가 같은 응답 안에서 중복 후보를 반환한
             * 경우에도 한 번만 저장
             */
            if (
                    !hashesInCurrentRequest.add(
                            contentHash
                    )
            ) {
                continue;
            }

            /*
             * 과거 일기에서 이미 발견된 동일 기억도
             * 중복 저장하지 않음
             */
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

            savedCount++;
        }

        return savedCount;
    }
}