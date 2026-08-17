package mutsa.hackathon.presentation;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DevTestPasswordVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/access")
@RequiredArgsConstructor
@ConditionalOnExpression(
        "'${app.dev.dated-diary-enabled:false}' == 'true' "
                + "or "
                + "'${app.weekly-reward.manual-trigger-enabled:false}' == 'true'"
)
public class DevTestAccessController {

    private final DevTestPasswordVerifier
            devTestPasswordVerifier;

    @PostMapping("/verify")
    public ApiResponse<Boolean> verify(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @RequestHeader(
                    value = DevTestPasswordVerifier.HEADER_NAME,
                    required = false
            )
            String password
    ) {
        if (user == null) {
            throw new ProjectException(
                    ErrorCode.ACCESS_DENIED
            );
        }

        devTestPasswordVerifier.verify(password);

        return ApiResponse.onSuccess(true);
    }
}