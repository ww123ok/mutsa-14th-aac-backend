package mutsa.hackathon.presentation;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.dto.NotificationReadAllResponse;
import mutsa.hackathon.dto.NotificationResponse;
import mutsa.hackathon.dto.NotificationUnreadCountResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.security.CustomOAuth2User;
import mutsa.hackathon.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService
            notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>>
    notifications(
            @AuthenticationPrincipal
            CustomOAuth2User user,
            @RequestParam(
                    required = false
            )
            Integer limit
    ) {
        return ApiResponse.onSuccess(
                notificationService.findMine(
                        user.getKakaoUserProfile()
                                .id(),
                        limit
                )
        );
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountResponse>
    unreadCount(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        return ApiResponse.onSuccess(
                notificationService.unreadCount(
                        user.getKakaoUserProfile()
                                .id()
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse>
    markRead(
            @AuthenticationPrincipal
            CustomOAuth2User user,
            @PathVariable
            Long notificationId
    ) {
        return ApiResponse.onSuccess(
                notificationService.markRead(
                        user.getKakaoUserProfile()
                                .id(),
                        notificationId
                )
        );
    }

    @PatchMapping("/read-all")
    public ApiResponse<NotificationReadAllResponse>
    markAllRead(
            @AuthenticationPrincipal
            CustomOAuth2User user
    ) {
        return ApiResponse.onSuccess(
                notificationService.markAllRead(
                        user.getKakaoUserProfile()
                                .id()
                )
        );
    }
}
