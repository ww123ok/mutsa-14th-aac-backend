package mutsa.hackathon.service;

/**
 * 일기에서 개인화 기억 후보를 추출할 때
 * AI에 전달하는 안전한 컨텍스트
 * diaryContent:
 * 오늘 작성한 원본 일기
 * job:
 * 온보딩에서 이미 알고 있는 현재 역할/직업
 * aiMemoryProfile:
 * 이미 승인되어 작성 도움 질문에 사용 중인
 * STABLE / RECENT 개인화 기억 JSON 캐시
 */
public record DiaryMemoryExtractionPrompt(
        String diaryContent,
        String job,
        String aiMemoryProfile
) {

    public DiaryMemoryExtractionPrompt {
        if (
                diaryContent == null
                        || diaryContent.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "기억 후보를 추출할 일기 내용은 필수입니다."
            );
        }

        diaryContent = diaryContent.trim();
        job = normalizeOptional(job);
        aiMemoryProfile = normalizeOptional(
                aiMemoryProfile
        );
    }

    private static String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}