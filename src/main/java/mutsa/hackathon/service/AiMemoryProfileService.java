package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 사용자 AI 기억의 생명주기를 관리합니다.
 *
 * - 승인된 기억으로 ai_memory_profile 재생성
 * - 전역 동의 철회 시 기억 일괄 폐기
 * - 일기 삭제 시 해당 일기 출처 기억 폐기
 */
@Service
@RequiredArgsConstructor
public class AiMemoryProfileService {

    private static final Set<UserMemoryStatus>
            USABLE_MEMORY_STATUSES =
            Set.of(
                    UserMemoryStatus.PENDING,
                    UserMemoryStatus.APPROVED
            );

    private final AppUserRepository appUserRepository;

    private final UserMemoryItemRepository
            userMemoryItemRepository;

    private final AiMemoryProfileBuilder
            aiMemoryProfileBuilder;

    /**
     * 현재 유효한 APPROVED 기억만 사용하여
     * ai_memory_profile을 다시 생성합니다.
     */
    @Transactional
    public void rebuildProfile(Long userId) {
        AppUser user = findUser(userId);

        /*
         * 전역 동의가 꺼진 사용자의 기억 프로필은
         * 어떤 경우에도 유지하지 않습니다.
         */
        if (!user.isAiMemoryConsent()) {
            user.clearAiMemoryProfile();
            return;
        }

        List<UserMemoryItem> approvedMemories =
                userMemoryItemRepository
                        .findActiveApprovedMemories(
                                userId,
                                UserMemoryStatus.APPROVED,
                                LocalDateTime.now()
                        );

        if (approvedMemories.isEmpty()) {
            user.clearAiMemoryProfile();
            return;
        }

        String profileJson =
                aiMemoryProfileBuilder.build(
                        approvedMemories
                );

        user.updateAiMemoryProfile(profileJson);
    }

    /**
     * AI 기억 활용 전역 동의 철회 시 호출합니다.
     *
     * 아직 검토하지 않은 후보와 이미 승인한 기억을
     * 모두 REVOKED로 변경합니다.
     */
    @Transactional
    public int revokeAllUsableMemories(
            Long userId
    ) {
        AppUser user = findUser(userId);

        List<UserMemoryItem> memories =
                userMemoryItemRepository
                        .findAllByUserIdAndStatusIn(
                                userId,
                                USABLE_MEMORY_STATUSES
                        );

        memories.forEach(
                UserMemoryItem::revoke
        );

        /*
         * 변경된 상태가 이후 조회에 즉시 반영되도록
         * 명시적으로 flush합니다.
         */
        userMemoryItemRepository.flush();

        user.clearAiMemoryProfile();

        return memories.size();
    }

    /**
     * 일기가 삭제되면 해당 일기에서 만들어진
     * PENDING / APPROVED 기억을 REVOKED로 변경합니다.
     *
     * 다른 일기에서 승인된 기억은 유지한 채
     * 프로필을 다시 생성합니다.
     */
    @Transactional
    public int revokeMemoriesFromDiary(
            Long userId,
            Long diaryId
    ) {
        AppUser user = findUser(userId);

        List<UserMemoryItem> memories =
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdAndStatusIn(
                                userId,
                                diaryId,
                                USABLE_MEMORY_STATUSES
                        );

        memories.forEach(
                UserMemoryItem::revoke
        );

        userMemoryItemRepository.flush();

        rebuildProfileInternal(user);

        return memories.size();
    }

    private void rebuildProfileInternal(
            AppUser user
    ) {
        if (!user.isAiMemoryConsent()) {
            user.clearAiMemoryProfile();
            return;
        }

        List<UserMemoryItem> approvedMemories =
                userMemoryItemRepository
                        .findActiveApprovedMemories(
                                user.getId(),
                                UserMemoryStatus.APPROVED,
                                LocalDateTime.now()
                        );

        if (approvedMemories.isEmpty()) {
            user.clearAiMemoryProfile();
            return;
        }

        user.updateAiMemoryProfile(
                aiMemoryProfileBuilder.build(
                        approvedMemories
                )
        );
    }

    private AppUser findUser(Long userId) {
        return appUserRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }
}