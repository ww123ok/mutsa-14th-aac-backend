package mutsa.hackathon.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.SuccessCode;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DiaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
@Validated
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    public ResponseEntity<ApiResponse<DiaryResponse>> create(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody DiaryCreateRequest request
    ) {
        KakaoUserProfile profile = user.getKakaoUserProfile();
        DiaryResponse response = diaryService.create(profile.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess(SuccessCode.CREATED, response));
    }

    @GetMapping
    public ApiResponse<List<DiaryResponse>> getMonthlyDiaries(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestParam @Min(1) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        List<DiaryResponse> response = diaryService.getMonthlyDiaries(
                user.getKakaoUserProfile().id(), year, month
        );
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/{diaryId}")
    public ApiResponse<DiaryResponse> getDiary(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long diaryId
    ) {
        DiaryResponse response = diaryService.getDiary(user.getKakaoUserProfile().id(), diaryId);
        return ApiResponse.onSuccess(response);
    }

    @DeleteMapping("/{diaryId}")
    public ApiResponse<Void> deleteDiary(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long diaryId
    ) {
        diaryService.deleteDiary(user.getKakaoUserProfile().id(), diaryId);
        return ApiResponse.onSuccess(null);
    }
}