package mutsa.hackathon.service;

import org.springframework.stereotype.Component;

/**
 * 실제 OpenAI 성찰 질문 생성기를 연결하기 전까지 사용하는
 * 임시 생성기
 * DiaryService가 예외를 받아 FALLBACK 질문으로 저장하도록
 * 의도적으로 예외를 발생시킴.
 */
@Component
public class FallbackDiaryReflectionQuestionGenerator
        implements DiaryReflectionQuestionGenerator {

    @Override
    public String generate(
            DiaryReflectionPrompt prompt
    ) {
        throw new IllegalStateException(
                "OpenAI 성찰 질문 생성기가 아직 연결되지 않았습니다."
        );
    }
}
