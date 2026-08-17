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

    /*
     * 양수이면 비밀번호와 사용자 ID를 모두 검사합니다.
     * 0 이하이면 비밀번호를 통과한 모든 로그인 사용자를
     * 허용합니다.
     */
    private final long allowedUserId;

    private final long maxPastDays;

    public DevDatedDiaryService(
            DiaryService diaryService,

            @Value(
                    "${app.dev.dated-diary-allowed-user-id:-1}"
            )
            long allowedUserId,

            @Value(
                    "${app.dev.dated-diary-max-past-days:3650}"
            )
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
        if (userId == null) {
            throw new ProjectException(
                    ErrorCode.ACCESS_DENIED
            );
        }

        /*
         * 양수 ID가 설정된 경우에만 추가 제한합니다.
         * -1 또는 0이면 비밀번호를 통과한 모든
         * 로그인 사용자를 허용합니다.
         */
        if (
                allowedUserId > 0
                        && userId.longValue()
                        != allowedUserId
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

        if (
                recordedDate.isBefore(
                        earliestAllowedDate
                )
        ) {
            throw new IllegalArgumentException(
                    "설정된 과거 날짜 허용 범위를 벗어났습니다."
            );
        }
    }
}