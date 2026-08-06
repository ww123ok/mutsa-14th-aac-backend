package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.UserMemoryCategory;
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
 * 승인된 사용자 기억을 OpenAI 프롬프트용 JSON 캐시로 변환.
 * 이 클래스는 DB를 직접 조회하거나 변경하지 않음.
 * 전달받은 기억 목록을 정렬하고 분류한 뒤
 * app_user.ai_memory_profile에 저장할 JSON 문자열을 생성
 */
@Component
@RequiredArgsConstructor
public class AiMemoryProfileBuilder {

    private static final int MAX_STABLE_MEMORY_COUNT = 10;
    private static final int MAX_ONGOING_TOPIC_COUNT = 5;
    private static final int MAX_PROFILE_LENGTH = 3_000;

    private final JsonMapper jsonMapper;

    public String build(
            List<UserMemoryItem> memories
    ) {
        if (memories == null) {
            throw new IllegalArgumentException(
                    "기억 목록은 null일 수 없습니다."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        /*
         * 승인되었고 아직 만료되지 않은 기억만 사용.
         * 최근 승인된 기억이 앞에 오도록 정렬.
         */
        List<UserMemoryItem> activeApprovedMemories =
                memories.stream()
                        .filter(memory ->
                                memory.getStatus()
                                        == UserMemoryStatus.APPROVED
                        )
                        .filter(memory ->
                                !memory.isExpired(now)
                        )
                        .sorted(memoryComparator())
                        .toList();

        /*
         * ONGOING_TOPIC을 제외한 안정적인 기억
         */
        List<UserMemoryItem> stableMemories =
                new ArrayList<>(
                        activeApprovedMemories.stream()
                                .filter(memory ->
                                        memory.getCategory()
                                                != UserMemoryCategory
                                                .ONGOING_TOPIC
                                )
                                .limit(
                                        MAX_STABLE_MEMORY_COUNT
                                )
                                .toList()
                );

        /*
         * 일정 기간만 유효한 최근 진행 주제
         */
        List<UserMemoryItem> ongoingTopics =
                new ArrayList<>(
                        activeApprovedMemories.stream()
                                .filter(memory ->
                                        memory.getCategory()
                                                == UserMemoryCategory
                                                .ONGOING_TOPIC
                                )
                                .limit(
                                        MAX_ONGOING_TOPIC_COUNT
                                )
                                .toList()
                );

        /*
         * 생성된 JSON이 최대 길이를 넘으면
         * 가장 오래된 기억부터 하나씩 제거.
         * 최근 기억을 우선 보존.
         */
        while (true) {
            AiMemoryProfilePayload payload =
                    createPayload(
                            stableMemories,
                            ongoingTopics,
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
                            && ongoingTopics.isEmpty()
            ) {
                return profileJson;
            }

            removeOldestMemory(
                    stableMemories,
                    ongoingTopics
            );
        }
    }

    private AiMemoryProfilePayload createPayload(
            List<UserMemoryItem> stableMemories,
            List<UserMemoryItem> ongoingTopics,
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

        List<AiMemoryProfilePayload.OngoingTopic>
                ongoingPayloads =
                ongoingTopics.stream()
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
                ongoingPayloads,
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
     * 안정적인 기억과 진행 중 주제의 마지막 항목 중
     * 더 오래된 항목을 제거
     */
    private void removeOldestMemory(
            List<UserMemoryItem> stableMemories,
            List<UserMemoryItem> ongoingTopics
    ) {
        if (stableMemories.isEmpty()) {
            ongoingTopics.remove(
                    ongoingTopics.size() - 1
            );
            return;
        }

        if (ongoingTopics.isEmpty()) {
            stableMemories.remove(
                    stableMemories.size() - 1
            );
            return;
        }

        UserMemoryItem oldestStable =
                stableMemories.get(
                        stableMemories.size() - 1
                );

        UserMemoryItem oldestOngoing =
                ongoingTopics.get(
                        ongoingTopics.size() - 1
                );

        if (
                isOlder(
                        oldestStable,
                        oldestOngoing
                )
        ) {
            stableMemories.remove(
                    stableMemories.size() - 1
            );
        } else {
            ongoingTopics.remove(
                    ongoingTopics.size() - 1
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
                .compareTo(second.getId()) < 0;
    }
}