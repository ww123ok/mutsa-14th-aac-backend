package mutsa.hackathon.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.DiaryCommentCreateRequest;
import mutsa.hackathon.dto.DiaryCommentResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.SuccessCode;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DiaryCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
        "/api/v1/diaries/{diaryId}/comments"
)
@RequiredArgsConstructor
public class DiaryCommentController {

    private final DiaryCommentService
            diaryCommentService;

    @PostMapping
    public ResponseEntity<
            ApiResponse<DiaryCommentResponse>
            > addComment(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @PathVariable
            Long diaryId,

            @Valid
            @RequestBody
            DiaryCommentCreateRequest request
    ) {
        DiaryCommentResponse response =
                diaryCommentService.addComment(
                        user.getKakaoUserProfile()
                                .id(),
                        diaryId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.onSuccess(
                                SuccessCode.CREATED,
                                response
                        )
                );
    }

    @GetMapping
    public ApiResponse<List<DiaryCommentResponse>>
    getComments(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @PathVariable
            Long diaryId
    ) {
        return ApiResponse.onSuccess(
                diaryCommentService.getComments(
                        user.getKakaoUserProfile()
                                .id(),
                        diaryId
                )
        );
    }
}
