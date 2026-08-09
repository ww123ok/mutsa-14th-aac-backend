package mutsa.hackathon.service;

/**
 * 성찰 질문 생성기가 참고할 수 있는 정보.
 * reflectionUsesDiaryContent가 false이면
 * diaryContent는 반드시 null로 전달
 */
public record DiaryReflectionPrompt(
        String nickname,
        String job,
        String memoryProfile,
        String diaryContent,
        boolean reflectionUsesDiaryContent
) {
}