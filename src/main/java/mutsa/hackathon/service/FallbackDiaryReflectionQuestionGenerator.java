package mutsa.hackathon.service;

import org.springframework.stereotype.Component;

/**
 * 깨진 머지 상태를 우선 복구하기 위한 임시 구현체.
 * DiaryService가 이 예외를 받아
 * FALLBACK 성찰 질문을 생성.
 * 다음 OpenAI 성찰 질문 체크포인트에서
 * OpenAiDiaryReflectionQuestionGenerator로 교체 예정.
 */
@Component
public class FallbackDiaryReflectionQuestionGenerator
        implements DiaryReflectionQuestionGenerator {

    @Override
    public String generate(String diaryContent) {
        throw new IllegalStateException(
                "OpenAI 성찰 질문 생성기가 아직 연결되지 않았습니다."
        );
    }
}
