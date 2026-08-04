package mutsa.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.KakaoUserProfile;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.SuccessCode;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DiaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    public ResponseEntity<ApiResponse<DiaryCreateResponse>> create(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody DiaryCreateRequest request
    ) {
        DiaryCreateResponse response = diaryService.create(
                user.getKakaoUserProfile().id(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess(SuccessCode.CREATED, response));
    }
}