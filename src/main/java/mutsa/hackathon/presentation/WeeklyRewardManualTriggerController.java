package mutsa.hackathon.presentation;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.WeeklyRewardTriggerResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DevTestPasswordVerifier;
import mutsa.hackathon.service.WeeklyRewardBatchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

    private final DevTestPasswordVerifier
            devTestPasswordVerifier;

    @PostMapping("/generate")
    public ApiResponse<WeeklyRewardTriggerResponse>
    generate(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @RequestHeader(
                    value = DevTestPasswordVerifier.HEADER_NAME,
                    required = false
            )
            String password,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate weekStartDate
    ) {
        if (user == null) {
            throw new ProjectException(
                    ErrorCode.ACCESS_DENIED
            );
        }

        devTestPasswordVerifier.verify(password);

        return ApiResponse.onSuccess(
                weeklyRewardBatchService
                        .generateForUser(
                                user.getKakaoUserProfile()
                                        .id(),
                                weekStartDate
                        )
        );
    }
}