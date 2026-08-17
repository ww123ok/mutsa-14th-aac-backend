package mutsa.hackathon.service;

/**
 * 작성 도움 질문이 어떤 문맥을 기반으로 만들어졌는지 나타냄.
 *
 * CURRENT_DRAFT:
 * 사용자가 지금 작성 중인 일기 본문을 기반으로 한 실시간 후속 질문.
 *
 * RECENT_CONTEXT:
 * 사용자가 과거에 개인화 활용에 동의했던 최근 일기 내용을 기반으로 한 질문.
 *
 * GENERIC:
 * 사용자 정보를 사용하지 않는 사전 작성 범용 질문.
 */
public enum WritingHelpQuestionContextType {
    CURRENT_DRAFT,
    RECENT_CONTEXT,
    GENERIC
}
