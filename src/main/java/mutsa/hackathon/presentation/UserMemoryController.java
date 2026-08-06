package mutsa.hackathon.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.MemoryCandidateListResponse;
import mutsa.hackathon.dto.MemoryCandidateReviewRequest;
import mutsa.hackathon.dto.MemoryCandidateReviewResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.UserMemoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/v1/diaries/{diaryId}/memory-candidates"
)
@RequiredArgsConstructor
public class UserMemoryController {

    private final UserMemoryService userMemoryService;

    @GetMapping
    public ApiResponse<MemoryCandidateListResponse>
    getCandidates(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @PathVariable
            Long diaryId
    ) {
        MemoryCandidateListResponse response =
                userMemoryService.getCandidates(
                        user
                                .getKakaoUserProfile()
                                .id(),
                        diaryId
                );

        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/review")
    public ApiResponse<MemoryCandidateReviewResponse>
    reviewCandidates(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @PathVariable
            Long diaryId,

            @Valid
            @RequestBody
            MemoryCandidateReviewRequest request
    ) {
        MemoryCandidateReviewResponse response =
                userMemoryService.reviewCandidates(
                        user
                                .getKakaoUserProfile()
                                .id(),
                        diaryId,
                        request
                );

        return ApiResponse.onSuccess(response);
    }
}