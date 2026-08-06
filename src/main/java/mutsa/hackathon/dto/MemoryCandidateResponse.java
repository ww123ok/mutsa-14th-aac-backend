package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;

import java.time.LocalDateTime;

public record MemoryCandidateResponse(
        Long memoryId,
        UserMemoryCategory category,
        String memoryText,
        UserMemoryStatus status,

        @JsonFormat(
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        LocalDateTime expiresAt
) {

    public static MemoryCandidateResponse from(
            UserMemoryItem memory
    ) {
        return new MemoryCandidateResponse(
                memory.getId(),
                memory.getCategory(),
                memory.getMemoryText(),
                memory.getStatus(),
                memory.getExpiresAt()
        );
    }
}