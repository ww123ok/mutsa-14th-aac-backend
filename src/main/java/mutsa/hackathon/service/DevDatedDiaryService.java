package mutsa.hackathon.service;

import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 개발·QA 환경에서 실제 저장 흐름을 이용하여
 * 날짜가 지정된 일기를 생성합니다.
 *
 * 더미 데이터나 별도 저장 로직을 만들지 않고
 * DiaryService의 기존 성찰 질문·일간 보상 흐름을 재사용합니다.
 */
@Service
@ConditionalOnProperty(
        prefix = "app.dev",
        name = "dated-diary-enabled",
        havingValue = "true"
)
public class DevDatedDiaryService {

    private static final ZoneId SERVICE_ZONE =
            ZoneId.of("Asia/Seoul");

    private final DiaryService diaryService;
    private final long allowedUserId;
    private final long maxPastDays;

    public DevDatedDiaryService(
            DiaryService diaryService,
            @Value("${app.dev.dated-diary-allowed-user-id:-1}")
            long allowedUserId,
            @Value("${app.dev.dated-diary-max-past-days:3650}")
            long maxPastDays
    ) {
        this.diaryService = diaryService;
        this.allowedUserId = allowedUserId;
        this.maxPastDays = maxPastDays;
    }

    public DiaryCreateResponse create(
            Long userId,
            LocalDate recordedDate,
            DiaryCreateRequest request
    ) {
        validateAllowedUser(userId);
        validateRecordedDate(recordedDate);

        return diaryService.createForRecordedDate(
                userId,
                request,
                recordedDate
        );
    }

    private void validateAllowedUser(Long userId) {
        if (
                userId == null
                        || allowedUserId < 1
                        || userId.longValue() != allowedUserId
        ) {
            throw new ProjectException(
                    ErrorCode.ACCESS_DENIED
            );
        }
    }

    private void validateRecordedDate(
            LocalDate recordedDate
    ) {
        if (recordedDate == null) {
            throw new IllegalArgumentException(
                    "일기 작성 날짜는 필수입니다."
            );
        }

        if (maxPastDays < 1) {
            throw new IllegalStateException(
                    "DEV_DATED_DIARY_MAX_PAST_DAYS는 1 이상이어야 합니다."
            );
        }

        LocalDate today =
                LocalDate.now(SERVICE_ZONE);

        if (recordedDate.isAfter(today)) {
            throw new IllegalArgumentException(
                    "미래 날짜의 일기는 작성할 수 없습니다."
            );
        }

        LocalDate earliestAllowedDate =
                today.minusDays(maxPastDays);

        if (recordedDate.isBefore(earliestAllowedDate)) {
            throw new IllegalArgumentException(
                    "설정된 과거 날짜 허용 범위를 벗어났습니다."
            );
        }
    }
}
