package mutsa.hackathon.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.WritingHelpQuestionHistoryResponse;
import mutsa.hackathon.dto.WritingHelpQuestionRequest;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.dto.WritingHelpStatusResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.AiWritingHelpService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * 오늘 이미 생성된 작성 도움 질문 목록을 조회.
     * 현재 인증된 사용자와 현재 DAYBIT 날짜를 기준으로만 조회하므로
     * 다른 계정의 질문이 섞이지 않는다.
     * 이 API는 질문 사용 횟수를 소모하지 않음.
     */
    @GetMapping("/questions")
    public ApiResponse<List<WritingHelpQuestionHistoryResponse>>
    getQuestions(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        List<WritingHelpQuestionHistoryResponse> response =
                aiWritingHelpService
                        .getTodayQuestionHistory(
                                user.getKakaoUserProfile()
                                        .id()
                        );

        return ApiResponse.onSuccess(
                response
        );
    }

    /**
     * 작성 도움 질문을 한 개 생성.
     * currentContent가 있으면 현재 작성 중인 일기의 구체적인 내용을
     * 확장하는 실시간 후속 질문을 생성.
     * currentContent가 비어 있으면 사용자가 개인화 활용에 동의했던
     * 최근 일기를 우선 사용하고, 사용할 최근 맥락이 없으면
     * 사전 작성 범용 질문을 제공.
     * 기존 클라이언트 호환을 위해 Request Body 자체는 생략 가능.
     */
    @PostMapping("/questions")
    public ApiResponse<WritingHelpQuestionResponse>
    generateQuestion(
            @AuthenticationPrincipal
            CustomOAuth2User user,
            @Valid
            @RequestBody(required = false)
            WritingHelpQuestionRequest request
    ) {
        WritingHelpQuestionResponse response =
                aiWritingHelpService
                        .generateQuestion(
                                user.getKakaoUserProfile()
                                        .id(),
                                request
                        );

        return ApiResponse.onSuccess(
                response
        );
    }
}
