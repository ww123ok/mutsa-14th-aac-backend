package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import mutsa.hackathon.dto.AiMemoryProfilePayload;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 승인된 사용자 기억을 OpenAI 프롬프트용
 * JSON 캐시로 변환.
 * STABLE 기억과 RECENT 기억을 분리하여
 * app_user.ai_memory_profile에 저장할
 * JSON 문자열을 생성.
 */
@Component
@RequiredArgsConstructor
public class AiMemoryProfileBuilder {

    private static final int
            MAX_STABLE_MEMORY_COUNT = 10;

    private static final int
            MAX_RECENT_CONTEXT_COUNT = 5;

    private static final int
            MAX_PROFILE_LENGTH = 3_000;

    private final JsonMapper jsonMapper;

    public String build(
            List<UserMemoryItem> memories
    ) {

        if (memories == null) {
            throw new IllegalArgumentException(
                    "기억 목록은 null일 수 없습니다."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * APPROVED 상태이면서 아직 유효한 기억만
         * 실제 질문 개인화 프로필에 포함
         */
        List<UserMemoryItem>
                activeApprovedMemories =
                memories.stream()
                        .filter(memory ->
                                memory.getStatus()
                                        == UserMemoryStatus
                                        .APPROVED
                        )
                        .filter(memory ->
                                !memory.isExpired(now)
                        )
                        .sorted(
                                memoryComparator()
                        )
                        .toList();

        /*
         * 장기간 유지되는 고정 프로필 성격의 기억
         */
        List<UserMemoryItem> stableMemories =
                new ArrayList<>(
                        activeApprovedMemories
                                .stream()
                                .filter(memory ->
                                        memory
                                                .getCategory()
                                                .isStable()
                                )
                                .limit(
                                        MAX_STABLE_MEMORY_COUNT
                                )
                                .toList()
                );

        /*
         * 최근 7일 정도만 사용하는
         * 최신 상황/맥락
         */
        List<UserMemoryItem> recentContexts =
                new ArrayList<>(
                        activeApprovedMemories
                                .stream()
                                .filter(memory ->
                                        memory
                                                .getCategory()
                                                .isRecent()
                                )
                                .limit(
                                        MAX_RECENT_CONTEXT_COUNT
                                )
                                .toList()
                );

        /*
         * 최종 JSON이 너무 커지면
         * 오래된 기억부터 제거.
         * 따라서 일기가 계속 쌓여도 프롬프트 크기가
         * 무한히 증가하지 않음.
         */
        while (true) {

            AiMemoryProfilePayload payload =
                    createPayload(
                            stableMemories,
                            recentContexts,
                            now
                    );

            String profileJson =
                    serialize(payload);

            if (
                    profileJson.length()
                            <= MAX_PROFILE_LENGTH
            ) {
                return profileJson;
            }

            if (
                    stableMemories.isEmpty()
                            && recentContexts.isEmpty()
            ) {
                return profileJson;
            }

            removeOldestMemory(
                    stableMemories,
                    recentContexts
            );
        }
    }

    private AiMemoryProfilePayload createPayload(
            List<UserMemoryItem> stableMemories,
            List<UserMemoryItem> recentContexts,
            LocalDateTime updatedAt
    ) {

        List<AiMemoryProfilePayload.StableMemory>
                stablePayloads =
                stableMemories.stream()
                        .map(memory ->
                                new AiMemoryProfilePayload
                                        .StableMemory(
                                        memory.getCategory(),
                                        memory.getMemoryText()
                                )
                        )
                        .toList();

        /*
         * 기존 JSON 필드명 ongoingTopics는
         * 기존 테스트 및 작성 도움 프롬프트 계약을
         * 깨뜨리지 않기 위해 이번 단계에서는 유지.
         * 내부 의미는 RECENT context 전체로 확장.
         */
        List<AiMemoryProfilePayload.OngoingTopic>
                recentPayloads =
                recentContexts.stream()
                        .map(memory ->
                                new AiMemoryProfilePayload
                                        .OngoingTopic(
                                        memory.getCategory(),
                                        memory.getMemoryText(),
                                        memory.getExpiresAt()
                                )
                        )
                        .toList();

        return new AiMemoryProfilePayload(
                AiMemoryProfilePayload
                        .CURRENT_SCHEMA_VERSION,
                stablePayloads,
                recentPayloads,
                updatedAt
        );
    }

    private String serialize(
            AiMemoryProfilePayload payload
    ) {

        try {
            return jsonMapper.writeValueAsString(
                    payload
            );

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "AI 기억 프로필 JSON 생성에 실패했습니다.",
                    exception
            );
        }
    }

    /**
     * 최근 승인된 기억이 앞에 오도록 정렬
     */
    private Comparator<UserMemoryItem>
    memoryComparator() {

        return Comparator
                .comparing(
                        UserMemoryItem::getApprovedAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
                .thenComparing(
                        UserMemoryItem::getId,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                );
    }

    /**
     * 고정 프로필과 최근 맥락의 마지막 항목 중
     * 더 오래된 기억을 제거
     */
    private void removeOldestMemory(
            List<UserMemoryItem> stableMemories,
            List<UserMemoryItem> recentContexts
    ) {

        if (stableMemories.isEmpty()) {
            recentContexts.remove(
                    recentContexts.size() - 1
            );

            return;
        }

        if (recentContexts.isEmpty()) {
            stableMemories.remove(
                    stableMemories.size() - 1
            );

            return;
        }

        UserMemoryItem oldestStable =
                stableMemories.get(
                        stableMemories.size() - 1
                );

        UserMemoryItem oldestRecent =
                recentContexts.get(
                        recentContexts.size() - 1
                );

        if (
                isOlder(
                        oldestStable,
                        oldestRecent
                )
        ) {
            stableMemories.remove(
                    stableMemories.size() - 1
            );

        } else {
            recentContexts.remove(
                    recentContexts.size() - 1
            );
        }
    }

    private boolean isOlder(
            UserMemoryItem first,
            UserMemoryItem second
    ) {

        LocalDateTime firstApprovedAt =
                first.getApprovedAt();

        LocalDateTime secondApprovedAt =
                second.getApprovedAt();

        if (firstApprovedAt == null) {
            return true;
        }

        if (secondApprovedAt == null) {
            return false;
        }

        int timeComparison =
                firstApprovedAt.compareTo(
                        secondApprovedAt
                );

        if (timeComparison != 0) {
            return timeComparison < 0;
        }

        if (first.getId() == null) {
            return true;
        }

        if (second.getId() == null) {
            return false;
        }

        return first.getId()
                .compareTo(
                        second.getId()
                ) < 0;
    }
}