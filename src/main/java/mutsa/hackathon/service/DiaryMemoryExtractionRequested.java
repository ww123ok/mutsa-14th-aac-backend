package mutsa.hackathon.service;

/**
 * 일기 트랜잭션이 성공적으로 커밋된 뒤
 * 개인화 기억 추출을 시작하기 위한 이벤트
 */
public record DiaryMemoryExtractionRequested(
        Long diaryId
) {

    public DiaryMemoryExtractionRequested {

        if (diaryId == null) {
            throw new IllegalArgumentException(
                    "기억을 추출할 일기 ID는 필수입니다."
            );
        }
    }
}