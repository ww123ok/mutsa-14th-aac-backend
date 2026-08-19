package mutsa.hackathon.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.DiaryDraftResponse;
import mutsa.hackathon.dto.DiaryDraftUpsertRequest;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DiaryDraftService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diaries/draft")
@RequiredArgsConstructor
public class DiaryDraftController {

    private final DiaryDraftService
            diaryDraftService;

    @PutMapping
    public ApiResponse<DiaryDraftResponse> saveDraft(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @Valid
            @RequestBody
            DiaryDraftUpsertRequest request
    ) {
        return ApiResponse.onSuccess(
                diaryDraftService.saveCurrentDraft(
                        user.getKakaoUserProfile()
                                .id(),
                        request
                )
        );
    }

    @GetMapping
    public ApiResponse<DiaryDraftResponse> getDraft(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        return ApiResponse.onSuccess(
                diaryDraftService.getCurrentDraft(
                        user.getKakaoUserProfile()
                                .id()
                )
        );
    }

    @DeleteMapping
    public ApiResponse<Void> deleteDraft(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        diaryDraftService.deleteCurrentDraft(
                user.getKakaoUserProfile()
                        .id()
        );

        return ApiResponse.onSuccess(
                null
        );
    }
}
