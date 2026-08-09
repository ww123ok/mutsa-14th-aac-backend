package mutsa.hackathon.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OpenAI 성찰 질문 기능이 비활성화되어 있을 때 사용하는 생성기.
 * DiaryService가 이 예외를 받아 기본 성찰 질문을 FALLBACK 출처로
 * 저장하도록 의도적으로 예외를 발생시킴.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.openai",
        name = "reflection-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class FallbackDiaryReflectionQuestionGenerator
        implements DiaryReflectionQuestionGenerator {

    @Override
    public String generate(
            DiaryReflectionPrompt prompt
    ) {
        throw new IllegalStateException(
                "OpenAI 성찰 질문 생성 기능이 비활성화되어 있습니다."
        );
    }
}
