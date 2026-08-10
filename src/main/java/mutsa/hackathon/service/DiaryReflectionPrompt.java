package mutsa.hackathon.service;

/**
 * 성찰 질문은 최신 기획에 따라
 * 오직 오늘 작성한 일기 내용만을 기반으로 생성
 */
public record DiaryReflectionPrompt(
        String diaryContent
) {

    public DiaryReflectionPrompt {

        if (
                diaryContent == null
                        || diaryContent.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "성찰 질문 생성을 위한 일기 내용은 필수입니다."
            );
        }

        diaryContent =
                diaryContent.trim();
    }
}