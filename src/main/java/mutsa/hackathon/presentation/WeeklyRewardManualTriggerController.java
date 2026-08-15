package mutsa.hackathon.presentation;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.WeeklyRewardTriggerResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.WeeklyRewardBatchService;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnExpression;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation
        .AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 실제 사용자가 이미 작성한 일기로
 * 주간 보상 생성 흐름을 확인하는
 * 로컬·스테이징 전용 수동 트리거입니다.
 *
 * 더미 일기를 만들지 않습니다.
 */
@RestController
@RequestMapping("/api/dev/me/weekly-rewards")
@RequiredArgsConstructor
@ConditionalOnExpression(
        "'${app.weekly-reward.enabled:false}' == 'true' "
                + "and "
                + "'${app.weekly-reward.manual-trigger-enabled:false}' == 'true'"
)
public class WeeklyRewardManualTriggerController {

    private final WeeklyRewardBatchService
            weeklyRewardBatchService;

    @PostMapping("/generate")
    public ApiResponse<WeeklyRewardTriggerResponse>
    generate(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate weekStartDate
    ) {
        return ApiResponse.onSuccess(
                weeklyRewardBatchService
                        .generateForUser(
                                user
                                        .getKakaoUserProfile()
                                        .id(),
                                weekStartDate
                        )
        );
    }
}