package mutsa.hackathon.presentation;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.DevTodayDiaryResetResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DevDiaryResetService;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.security.core.annotation
        .AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/me/diaries")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.dev",
        name = "reset-enabled",
        havingValue = "true"
)
public class DevDiaryResetController {

    private final DevDiaryResetService
            devDiaryResetService;

    @DeleteMapping("/today")
    public ApiResponse<DevTodayDiaryResetResponse>
    resetToday(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        DevTodayDiaryResetResponse response =
                devDiaryResetService.resetToday(
                        user.getKakaoUserProfile()
                                .id()
                );

        return ApiResponse.onSuccess(
                response
        );
    }
}