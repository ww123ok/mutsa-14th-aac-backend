package mutsa.hackathon.domain;

/**
 * AI가 일기에서 발견할 수 있는 개인화 기억의 분류.
 * 각 분류는 장기간 유지되는 STABLE 기억인지,
 * 일정 기간만 사용하는 RECENT 맥락인지 함께 정의.
 */
public enum UserMemoryCategory {

    /**
     * 반려동물과 관련된 비교적 안정적인 정보
     */
    PET(
            UserMemoryRetention.STABLE
    ),

    /**
     * 현재 직업, 학교, 전공 등
     * 비교적 안정적인 역할 정보
     */
    WORK_STUDY(
            UserMemoryRetention.STABLE
    ),

    /**
     * 취미
     */
    HOBBY(
            UserMemoryRetention.STABLE
    ),

    /**
     * 관심 분야
     */
    INTEREST(
            UserMemoryRetention.STABLE
    ),

    /**
     * 사용자가 직접 드러낸
     * 비교적 지속적인 특징이나 성향
     */
    TRAIT(
            UserMemoryRetention.STABLE
    ),

    /**
     * 실명 없이 일반화한 가족 관계 정보
     */
    FAMILY(
            UserMemoryRetention.STABLE
    ),

    /**
     * 실명 없이 일반화한 친구, 연인,
     * 동료 등의 관계 정보
     */
    RELATIONSHIP(
            UserMemoryRetention.STABLE
    ),

    /**
     * 반복되는 생활 방식이나 습관
     */
    ROUTINE(
            UserMemoryRetention.STABLE
    ),

    /**
     * 장기간 이어지는 목표
     */
    GOAL(
            UserMemoryRetention.STABLE
    ),

    /**
     * 최근 이어지는 고민이나 걱정거리
     */
    CONCERN(
            UserMemoryRetention.RECENT
    ),

    /**
     * 시험, 프로젝트, 갈등, 일정 등
     * 최근 진행 중인 주제
     */
    ONGOING_TOPIC(
            UserMemoryRetention.RECENT
    ),

    /**
     * 명확히 장기 특징으로 판단하기 어려운
     * 안전한 최근 맥락.
     * 불확실한 정보를 영구 프로필로 남기지 않도록
     * 기본적으로 RECENT로 취급.
     */
    OTHER(
            UserMemoryRetention.RECENT
    );

    private final UserMemoryRetention
            retention;

    UserMemoryCategory(
            UserMemoryRetention retention
    ) {
        this.retention = retention;
    }

    public UserMemoryRetention retention() {
        return retention;
    }

    public boolean isStable() {
        return retention
                == UserMemoryRetention.STABLE;
    }

    public boolean isRecent() {
        return retention
                == UserMemoryRetention.RECENT;
    }
}