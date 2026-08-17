package mutsa.hackathon.service;

/**
 * 범용 작성 도움 질문의 주제 범주.
 *
 * 신규 사용자처럼 최근 맥락을 사용할 수 없는 경우에도
 * 하루 3개의 질문이 같은 결의 질문으로 몰리지 않도록
 * 카테고리 단위 다양성 제어에 사용.
 */
public enum WritingHelpGenericQuestionCategory {
    SCENE,
    SMALL_MOMENT,
    PEOPLE,
    FEELING_THOUGHT,
    WRAP_UP,
    PLAYFUL
}
