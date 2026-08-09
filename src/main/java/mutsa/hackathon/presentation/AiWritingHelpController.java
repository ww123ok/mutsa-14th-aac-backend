package mutsa.hackathon.presentation;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.AiWritingHelpService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/ai/writing-help")
@RequiredArgsConstructor
public class AiWritingHelpController {

    private final AiWritingHelpService aiWritingHelpService;

    @PostMapping("/questions")
    public ApiResponse<WritingHelpQuestionResponse> generateQuestion(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        WritingHelpQuestionResponse response = aiWritingHelpService.generateQuestion(
                user.getKakaoUserProfile().id()
        );

        return ApiResponse.onSuccess(response);
    }
}
