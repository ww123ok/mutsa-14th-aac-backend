package mutsa.hackathon.domain;

/**
 * AI가 일기에서 발견할 수 있는 장기 기억의 분류.
 * 민감정보나 일시적인 감정 자체는 기억으로 저장하지 않고,
 * 향후 질문 개인화에 도움이 되는 안전한 정보만 분류함.
 */
public enum UserMemoryCategory {

    /**
     * 반려동물과 관련된 비교적 안정적인 정보
     */
    PET,

    /**
     * 학교, 직장, 프로젝트, 취업 준비 등
     */
    WORK_STUDY,

    /**
     * 취미와 관심 분야
     */
    INTEREST,

    /**
     * 반복되는 생활 방식이나 습관
     */
    ROUTINE,

    /**
     * 사용자가 꾸준히 진행 중인 목표
     */
    GOAL,

    /**
     * 실명 없이 일반화된 관계 정보
     */
    RELATIONSHIP,

    /**
     * 일정 기간 이어지는 최근 주제
     */
    ONGOING_TOPIC,

    /**
     * 위 분류에 포함되지 않는 안전한 기억
     */
    OTHER
}