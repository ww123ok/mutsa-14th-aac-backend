package mutsa.hackathon.presentation;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.DiaryAutoCompletionNoticeResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DiaryAutoCompletionNoticeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/v1/diaries/auto-completion-notices"
)
@RequiredArgsConstructor
public class DiaryAutoCompletionNoticeController {

    private final DiaryAutoCompletionNoticeService
            noticeService;

    @GetMapping("/pending")
    public ApiResponse<
            DiaryAutoCompletionNoticeResponse
            > getPending(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        return ApiResponse.onSuccess(
                noticeService.getPendingNotice(
                        user.getKakaoUserProfile()
                                .id()
                )
        );
    }

    @PatchMapping("/{noticeId}/view")
    public ApiResponse<
            DiaryAutoCompletionNoticeResponse
            > markViewed(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @PathVariable
            Long noticeId
    ) {
        return ApiResponse.onSuccess(
                noticeService.markViewed(
                        user.getKakaoUserProfile()
                                .id(),
                        noticeId
                )
        );
    }
}
