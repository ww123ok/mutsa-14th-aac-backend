package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.UserMemoryCategory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * app_user.ai_memory_profile에 JSON으로 저장되는
 * 질문 생성용 AI 기억 캐시 구조.
 * 원본 일기나 내부 식별자를 포함하지 않음.
 */
public record AiMemoryProfilePayload(
        int schemaVersion,

        List<StableMemory> stableMemories,

        List<OngoingTopic> ongoingTopics,

        @JsonFormat(
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        LocalDateTime updatedAt
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public record StableMemory(
            UserMemoryCategory category,
            String text
    ) {
    }

    public record OngoingTopic(
            UserMemoryCategory category,
            String text,

            @JsonFormat(
                    pattern = "yyyy-MM-dd'T'HH:mm:ss"
            )
            LocalDateTime expiresAt
    ) {
    }
}