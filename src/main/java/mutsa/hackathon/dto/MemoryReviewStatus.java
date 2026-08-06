package mutsa.hackathon.dto;

/**
 * 특정 일기에서 생성된 기억 후보들의 전체 검토 상태
 */
public enum MemoryReviewStatus {

    /**
     * 생성된 기억 후보가 없음
     */
    NONE,

    /**
     * 사용자의 검토를 기다리는 중
     */
    PENDING,

    /**
     * 모든 후보가 승인됨
     */
    APPROVED,

    /**
     * 모든 후보가 거절됨
     */
    REJECTED,

    /**
     * 동의 철회나 일기 삭제로 사용 중지됨
     */
    REVOKED,

    /**
     * 서로 다른 상태가 섞여 있는 비정상 또는 예외 상태
     */
    MIXED
}