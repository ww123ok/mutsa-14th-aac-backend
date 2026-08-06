package mutsa.hackathon.domain;

public enum UserMemoryStatus {

    /**
     * AI가 발견했지만 아직 사용자가 검토하지 않은 상태
     */
    PENDING,

    /**
     * 사용자가 향후 질문에 활용하도록 승인한 상태
     */
    APPROVED,

    /**
     * 사용자가 저장을 거절한 상태
     */
    REJECTED,

    /**
     * 과거에 사용되었거나 대기 중이었지만
     * 동의 철회, 일기 삭제 등으로 사용 중지된 상태
     */
    REVOKED
}