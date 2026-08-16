package mutsa.hackathon.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.code.SuccessCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.DevDatedDiaryService;
import mutsa.hackathon.service.DevTestPasswordVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dev/me/diaries")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.dev",
        name = "dated-diary-enabled",
        havingValue = "true"
)
public class DevDatedDiaryController {

    private final DevDatedDiaryService
            devDatedDiaryService;

    private final DevTestPasswordVerifier
            devTestPasswordVerifier;

    @PostMapping
    public ResponseEntity<
            ApiResponse<DiaryCreateResponse>
            > create(
            @AuthenticationPrincipal
            CustomOAuth2User user,

            @RequestHeader(
                    value = DevTestPasswordVerifier.HEADER_NAME,
                    required = false
            )
            String password,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate recordedDate,

            @Valid
            @RequestBody
            DiaryCreateRequest request
    ) {
        if (user == null) {
            throw new ProjectException(
                    ErrorCode.ACCESS_DENIED
            );
        }

        devTestPasswordVerifier.verify(password);

        DiaryCreateResponse response =
                devDatedDiaryService.create(
                        user.getKakaoUserProfile().id(),
                        recordedDate,
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
}