package mutsa.hackathon.domain;

/**
 * 사용자 개인화 기억의 보존 성격
 * STABLE:
 * 비교적 오래 유지되는 개인 특징으로 취급
 * 별도의 자동 만료 시각을 두지 않음
 * RECENT:
 * 최근 상황이나 진행 중인 맥락으로 취급
 * 현재 MVP에서는 7일 동안만 질문 개인화에 사용
 */
public enum UserMemoryRetention {

    STABLE,

    RECENT
}