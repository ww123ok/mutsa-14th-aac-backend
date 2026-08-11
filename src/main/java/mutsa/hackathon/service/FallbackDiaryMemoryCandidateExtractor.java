package mutsa.hackathon.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OpenAI 기억 추출 기능이 비활성화된 환경에서 사용하는
 * 안전한 fallback 구현체.
 * 기억 후보 생성은 일기 작성의 필수 기능이 아니므로
 * 기능이 꺼져 있을 때 일기 저장을 실패시키지 않고
 * 빈 후보 목록을 반환.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.openai",
        name = "memory-extraction-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class FallbackDiaryMemoryCandidateExtractor
        implements DiaryMemoryCandidateExtractor {

    @Override
    public List<DiaryMemoryCandidate> extract(
            DiaryMemoryExtractionPrompt prompt
    ) {
        return List.of();
    }
}