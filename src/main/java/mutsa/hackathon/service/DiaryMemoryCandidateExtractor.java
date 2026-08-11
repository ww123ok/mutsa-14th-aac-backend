package mutsa.hackathon.service;

import java.util.List;

/**
 * 하나의 일기에서 향후 작성 도움 질문에 활용할 수 있는
 * 개인화 기억 후보를 추출
 * 이미 알고 있는 직업/개인화 기억도 함께 전달하여
 * 같은 사실을 표현만 바꾸어 반복 추출하지 않도록 함.
 */
public interface DiaryMemoryCandidateExtractor {

    List<DiaryMemoryCandidate> extract(
            DiaryMemoryExtractionPrompt prompt
    );

    /**
     * 기존 단위/Live 테스트와 내부 호출 호환용 편의 메서드.
     * 중복 방지가 필요한 실제 서비스 흐름에서는
     * DiaryMemoryExtractionPrompt 버전을 사용.
     */
    default List<DiaryMemoryCandidate> extract(
            String diaryContent
    ) {
        return extract(
                new DiaryMemoryExtractionPrompt(
                        diaryContent,
                        null,
                        null
                )
        );
    }
}