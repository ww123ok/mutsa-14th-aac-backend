package mutsa.hackathon.presentation;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.dto.WritingHelpStatusResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.AiWritingHelpService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/writing-help")
@RequiredArgsConstructor
public class AiWritingHelpController {

    private final AiWritingHelpService
            aiWritingHelpService;

    /**
     * 오늘의 작성 도움 질문 사용 상태를 조회.
     * 이 API는 질문 사용 횟수를 소모하지 않음.
     */
    @GetMapping("/status")
    public ApiResponse<WritingHelpStatusResponse>
    getStatus(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        WritingHelpStatusResponse response =
                aiWritingHelpService.getStatus(
                        user.getKakaoUserProfile()
                                .id()
                );

        return ApiResponse.onSuccess(
                response
        );
    }

    /**
     * 작성 도움 질문을 한 개 생성.
     * 성공하면 오늘 사용 횟수가 한 번 증가.
     */
    @PostMapping("/questions")
    public ApiResponse<WritingHelpQuestionResponse>
    generateQuestion(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        WritingHelpQuestionResponse response =
                aiWritingHelpService
                        .generateQuestion(
                                user.getKakaoUserProfile()
                                        .id()
                        );

        return ApiResponse.onSuccess(
                response
        );
    }
}
