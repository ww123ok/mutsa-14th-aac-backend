package mutsa.hackathon.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.WeeklyRewardArchiveResponse;
import mutsa.hackathon.dto.WeeklyRewardResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.WeeklyRewardQueryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weekly-rewards")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
public class WeeklyRewardController {

    private final WeeklyRewardQueryService weeklyRewardQueryService;

    @GetMapping
    public ApiResponse<WeeklyRewardArchiveResponse> getMonthlyArchive(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        return ApiResponse.onSuccess(
                weeklyRewardQueryService.getMonthlyArchive(
                        user.getKakaoUserProfile().id(),
                        year,
                        month
                )
        );
    }

    @GetMapping("/{weeklyRewardId}")
    public ApiResponse<WeeklyRewardResponse> getOne(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long weeklyRewardId
    ) {
        return ApiResponse.onSuccess(
                weeklyRewardQueryService.getOne(
                        user.getKakaoUserProfile().id(),
                        weeklyRewardId
                )
        );
    }

    @PatchMapping("/{weeklyRewardId}/view")
    public ApiResponse<WeeklyRewardResponse> markViewed(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long weeklyRewardId
    ) {
        return ApiResponse.onSuccess(
                weeklyRewardQueryService.markViewed(
                        user.getKakaoUserProfile().id(),
                        weeklyRewardId
                )
        );
    }
}
