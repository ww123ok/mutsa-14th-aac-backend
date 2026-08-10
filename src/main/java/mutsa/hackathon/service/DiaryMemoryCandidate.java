package mutsa.hackathon.service;

import mutsa.hackathon.domain.UserMemoryCategory;

/**
 * 한 일기에서 발견된 개인화 기억 후보.
 * 원본 일기 전체를 장기 기억으로 저장하지 않고,
 * 질문 개인화에 필요한 일반화된 한 문장만
 * 기억 후보로 전달.
 */
public record DiaryMemoryCandidate(
        UserMemoryCategory category,
        String memoryText
) {

    private static final int
            MAX_MEMORY_TEXT_LENGTH = 500;

    public DiaryMemoryCandidate {

        if (category == null) {
            throw new IllegalArgumentException(
                    "기억 후보 분류는 필수입니다."
            );
        }

        if (
                memoryText == null
                        || memoryText.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "기억 후보 내용은 필수입니다."
            );
        }

        memoryText =
                memoryText.trim();

        if (
                memoryText.length()
                        > MAX_MEMORY_TEXT_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "기억 후보 내용은 500자 이하여야 합니다."
            );
        }
    }
}