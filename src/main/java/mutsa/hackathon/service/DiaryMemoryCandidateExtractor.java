package mutsa.hackathon.service;

import java.util.List;

/**
 * 하나의 일기에서 향후 작성 도움 질문에 활용할 수 있는
 * 개인화 기억 후보를 추출.
 * 구현체는 원본 일기를 그대로 장기 기억으로 저장하지 않고,
 * 안전하게 일반화된 짧은 기억 문장만 반환.
 */
public interface DiaryMemoryCandidateExtractor {

    List<DiaryMemoryCandidate> extract(
            String diaryContent
    );
}