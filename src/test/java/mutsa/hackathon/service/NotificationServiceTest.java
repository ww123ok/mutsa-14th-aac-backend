package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Notification;
import mutsa.hackathon.domain.NotificationType;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository
            notificationRepository;

    @Mock
    private AppUserRepository
            appUserRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse(
                        "2026-08-18T12:30:00Z"
                ),
                ZoneId.of(
                        "Asia/Seoul"
                )
        );

        service = new NotificationService(
                notificationRepository,
                appUserRepository,
                clock
        );
    }

    @Test
    void createsNotificationOnlyOncePerDedupKey() {
        AppUser user = user(1L);
        InAppNotificationRequested event =
                InAppNotificationRequested
                        .experienceFragmentArrived(
                                1L,
                                20L
                        );

        when(
                notificationRepository
                        .existsByDedupKey(
                                event.dedupKey()
                        )
        ).thenReturn(false);
        when(
                appUserRepository.findById(1L)
        ).thenReturn(Optional.of(user));

        service.create(event);

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(
                        Notification.class
                );
        verify(notificationRepository)
                .save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(
                NotificationType
                        .EXPERIENCE_FRAGMENT_ARRIVED,
                saved.getType()
        );
        assertEquals(20L, saved.getReferenceId());
        assertEquals(
                event.dedupKey(),
                saved.getDedupKey()
        );

        when(
                notificationRepository
                        .existsByDedupKey(
                                event.dedupKey()
                        )
        ).thenReturn(true);

        service.create(event);

        verify(
                appUserRepository
        ).findById(1L);
    }

    @Test
    void marksOwnedNotificationAsRead() {
        AppUser user = user(1L);
        Notification notification =
                Notification.create(
                        user,
                        NotificationType.DIARY_REMINDER,
                        "알림",
                        null,
                        "DIARY_REMINDER:1:"
                                + LocalDate.of(
                                2026,
                                8,
                                18
                        )
                );
        ReflectionTestUtils.setField(
                notification,
                "id",
                10L
        );

        when(
                notificationRepository
                        .findByIdAndUserId(
                                10L,
                                1L
                        )
        ).thenReturn(
                Optional.of(notification)
        );

        var response =
                service.markRead(
                        1L,
                        10L
                );

        assertTrue(response.read());
        assertNotNull(
                notification.getReadAt()
        );
    }

    private AppUser user(Long id) {
        AppUser user =
                AppUser.createKakaoUser(
                        "provider-" + id,
                        "사용자",
                        null,
                        null
                );
        ReflectionTestUtils.setField(
                user,
                "id",
                id
        );
        return user;
    }
}
