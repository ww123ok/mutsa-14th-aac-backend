package mutsa.hackathon.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.ExperienceFragmentResponse;
import mutsa.hackathon.dto.ExperienceFragmentReviewResponse;
import mutsa.hackathon.dto.ExperienceFragmentFeedbackRequest;
import mutsa.hackathon.dto.ExperienceFragmentFeedbackResponse;
import mutsa.hackathon.dto.ExperienceMatchRequest;
import mutsa.hackathon.dto.ExperienceMatchResponse;
import mutsa.hackathon.dto.ExperienceFragmentInboxResponse;
import mutsa.hackathon.dto.ReceivedExperienceFragmentResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.ExperienceFragmentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/experience-fragments")
@RequiredArgsConstructor
public class  ExperienceFragmentController {
    private final ExperienceFragmentService experienceFragmentService;

    @PostMapping("/diaries/{diaryId}")
    public ApiResponse<ExperienceFragmentResponse> request(
            @AuthenticationPrincipal CustomOAuth2User user, @PathVariable Long diaryId) {
        return ApiResponse.onSuccess(experienceFragmentService.request(user.getKakaoUserProfile().id(), diaryId));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ExperienceFragmentResponse>> mine(@AuthenticationPrincipal CustomOAuth2User user) {
        return ApiResponse.onSuccess(experienceFragmentService.mine(user.getKakaoUserProfile().id()));
    }

    @GetMapping("/inbox")
    public ApiResponse<List<ExperienceFragmentInboxResponse>> inbox(@AuthenticationPrincipal CustomOAuth2User user) {
        return ApiResponse.onSuccess(experienceFragmentService.inbox(user.getKakaoUserProfile().id()));
    }
    @GetMapping("/{shareId}/review")
    public ApiResponse<ExperienceFragmentReviewResponse> review(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long shareId
    ) {
        return ApiResponse.onSuccess(experienceFragmentService.review(user.getKakaoUserProfile().id(), shareId));
    }

    @PostMapping("/{shareId}/approve")
    public ApiResponse<ExperienceFragmentResponse> approve(@AuthenticationPrincipal CustomOAuth2User user,
                                                            @PathVariable Long shareId) {
        return ApiResponse.onSuccess(experienceFragmentService.approve(user.getKakaoUserProfile().id(), shareId));
    }

    @PostMapping("/{shareId}/reject")
    public ApiResponse<ExperienceFragmentResponse> reject(@AuthenticationPrincipal CustomOAuth2User user,
                                                           @PathVariable Long shareId) {
        return ApiResponse.onSuccess(experienceFragmentService.reject(user.getKakaoUserProfile().id(), shareId));
    }

    @PostMapping("/matches")
    public ApiResponse<Optional<ExperienceMatchResponse>> findMatch(@AuthenticationPrincipal CustomOAuth2User user,
                                                                      @Valid @RequestBody ExperienceMatchRequest request) {
        return ApiResponse.onSuccess(experienceFragmentService.findBestMatch(user.getKakaoUserProfile().id(), request.diaryId()));
    }

    @PostMapping("/matches/{shareId}/receive")
    public ApiResponse<ReceivedExperienceFragmentResponse> receive(@AuthenticationPrincipal CustomOAuth2User user,
                                                                    @PathVariable Long shareId) {
        return ApiResponse.onSuccess(experienceFragmentService.receive(user.getKakaoUserProfile().id(), shareId));
    }

    @PostMapping("/inbox/{arrivalId}/receive")
    public ApiResponse<ReceivedExperienceFragmentResponse> receiveFromInbox(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long arrivalId
    ) {
        return ApiResponse.onSuccess(experienceFragmentService.receiveFromInbox(user.getKakaoUserProfile().id(), arrivalId));
    }
    @PostMapping("/deliveries/{deliveryId}/feedback")
public ApiResponse<ExperienceFragmentFeedbackResponse> submitFeedback(
        @AuthenticationPrincipal CustomOAuth2User user,
        @PathVariable Long deliveryId,
        @Valid @RequestBody ExperienceFragmentFeedbackRequest request
) {
    return ApiResponse.onSuccess(
            experienceFragmentService.submitFeedback(
                    user.getKakaoUserProfile().id(),
                    deliveryId,
                    request)
    );
}

    @GetMapping("/{shareId}/feedbacks")
public ApiResponse<List<ExperienceFragmentFeedbackResponse>> feedbacks(
        @AuthenticationPrincipal CustomOAuth2User user,
        @PathVariable Long shareId
) {
    return ApiResponse.onSuccess(
            experienceFragmentService.firstThreeFeedbacks(
                    user.getKakaoUserProfile().id(),
                    shareId
            )
    );
}
}
