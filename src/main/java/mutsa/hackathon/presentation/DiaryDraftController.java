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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diaries/draft")
@RequiredArgsConstructor
public class DiaryDraftController {

    private final DiaryDraftService diaryDraftService;

    @PutMapping
    public ApiResponse<DiaryDraftResponse> saveDraft(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody DiaryDraftUpsertRequest request
    ) {
        return ApiResponse.onSuccess(
                diaryDraftService.saveCurrentDraft(
                        user.getKakaoUserProfile().id(),
                        request
                )
        );
    }

    @GetMapping
    public ApiResponse<DiaryDraftResponse> getDraft(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        return ApiResponse.onSuccess(
                diaryDraftService.getCurrentDraft(
                        user.getKakaoUserProfile().id()
                )
        );
    }

    /**
     * 편집 화면이 살아 있는 동안 프론트가 주기적으로 호출한다.
     * 이 lease가 유효하면 하루 경계를 넘겨도 자동완료를 보류한다.
     */
    @PatchMapping("/{draftId}/editing/heartbeat")
    public ApiResponse<DiaryDraftResponse> heartbeat(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long draftId
    ) {
        return ApiResponse.onSuccess(
                diaryDraftService.keepEditing(
                        user.getKakaoUserProfile().id(),
                        draftId
                )
        );
    }

    /**
     * 정상적인 화면 이탈 시 best-effort로 호출한다.
     * 호출되지 않아도 heartbeat lease 만료 후 자동완료된다.
     */
    @PatchMapping("/{draftId}/editing/stop")
    public ApiResponse<Void> stopEditing(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long draftId
    ) {
        diaryDraftService.stopEditing(
                user.getKakaoUserProfile().id(),
                draftId
        );
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/{draftId}")
    public ApiResponse<Void> deleteDraft(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long draftId
    ) {
        diaryDraftService.deleteDraft(
                user.getKakaoUserProfile().id(),
                draftId
        );
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteCurrentDraft(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        diaryDraftService.deleteCurrentDraft(
                user.getKakaoUserProfile().id()
        );
        return ApiResponse.onSuccess(null);
    }
}
