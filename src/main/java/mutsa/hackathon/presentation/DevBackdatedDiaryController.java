package mutsa.hackathon.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.code.SuccessCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DiaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/dev/me/diaries")
@RequiredArgsConstructor
@ConditionalOnExpression(
        "'${app.dev.backdated-diary-enabled:false}' == 'true' "
                + "and '${app.weekly-reward.enabled:false}' == 'true' "
                + "and '${app.weekly-reward.manual-trigger-enabled:false}' == 'true'"
)
public class DevBackdatedDiaryController {

    private static final ZoneId SERVICE_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final long MAX_PAST_DAYS =
            60L;

    private final DiaryService diaryService;

    /**
     * 테스트 API를 사용할 수 있는 사용자 ID.
     * -1이면 모든 사용자의 접근을 차단합니다.
     */
    @Value(
            "${app.dev.backdated-diary-allowed-user-id:-1}"
    )
    private Long allowedUserId;

    @PostMapping("/backdated")
    public ResponseEntity<
            ApiResponse<DiaryCreateResponse>
            > createBackdatedDiary(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate recordedDate,

            @Valid
            @RequestBody
            DiaryCreateRequest request
    ) {
        if (user == null) {
            throw new ProjectException(
                    ErrorCode.ACCESS_DENIED
            );
        }

        Long userId =
                user.getKakaoUserProfile()
                        .id();

        validateAllowedUser(
                userId
        );

        validateRecordedDate(
                recordedDate
        );

        DiaryCreateResponse response =
                diaryService
                        .createForRecordedDate(
                                userId,
                                request,
                                recordedDate
                        );

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        ApiResponse.onSuccess(
                                SuccessCode.CREATED,
                                response
                        )
                );
    }

    private void validateAllowedUser(
            Long userId
    ) {
        if (
                userId == null
                        || allowedUserId == null
                        || allowedUserId < 1
                        || !allowedUserId.equals(
                        userId
                )
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
                    "테스트 일기 날짜는 필수입니다."
            );
        }

        LocalDate today =
                LocalDate.now(
                        SERVICE_ZONE
                );

        if (
                !recordedDate.isBefore(
                        today
                )
        ) {
            throw new IllegalArgumentException(
                    "테스트 일기는 오늘보다 이전 날짜로만 작성할 수 있습니다."
            );
        }

        LocalDate earliestAllowedDate =
                today.minusDays(
                        MAX_PAST_DAYS
                );

        if (
                recordedDate.isBefore(
                        earliestAllowedDate
                )
        ) {
            throw new IllegalArgumentException(
                    "테스트 일기는 최근 60일 이내 날짜로만 작성할 수 있습니다."
            );
        }
    }
}