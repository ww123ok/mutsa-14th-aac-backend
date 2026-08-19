package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryAutoCompletionNotice;
import mutsa.hackathon.dto.DiaryAutoCompletionNoticeResponse;
import mutsa.hackathon.repository.DiaryAutoCompletionNoticeRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryAutoCompletionNoticeServiceTest {

    @Mock
    private DiaryAutoCompletionNoticeRepository
            noticeRepository;

    @Mock
    private DiaryRepository
            diaryRepository;

    private DiaryAutoCompletionNoticeService
            noticeService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse(
                        "2026-08-19T00:00:00Z"
                ),
                ZoneId.of(
                        "Asia/Seoul"
                )
        );

        noticeService =
                new DiaryAutoCompletionNoticeService(
                        noticeRepository,
                        diaryRepository,
                        clock
                );
    }

    @Test
    void 아직_보지_않은_자동완료_안내를_반환한다() {
        AppUser user = user();
        Diary diary = Diary.create(
                user,
                "자동 완료된 일기",
                LocalDate.of(
                        2026,
                        8,
                        18
                )
        );
        ReflectionTestUtils.setField(
                diary,
                "id",
                20L
        );

        DiaryAutoCompletionNotice notice =
                notice(user);

        when(
                noticeRepository
                        .findFirstByUserIdAndViewedAtIsNullOrderByAutoCompletedAtAscIdAsc(
                                1L
                        )
        ).thenReturn(
                Optional.of(notice)
        );

        when(
                diaryRepository
                        .findByIdAndUserIdAndDeletedFalse(
                                20L,
                                1L
                        )
        ).thenReturn(
                Optional.of(diary)
        );

        DiaryAutoCompletionNoticeResponse response =
                noticeService.getPendingNotice(
                        1L
                );

        assertFalse(
                response.viewed()
        );
    }

    @Test
    void 이미_삭제된_자동완료_일기의_안내는_건너뛴다() {
        DiaryAutoCompletionNotice notice =
                notice(user());

        when(
                noticeRepository
                        .findFirstByUserIdAndViewedAtIsNullOrderByAutoCompletedAtAscIdAsc(
                                1L
                        )
        ).thenReturn(
                Optional.of(notice),
                Optional.empty()
        );

        when(
                diaryRepository
                        .findByIdAndUserIdAndDeletedFalse(
                                20L,
                                1L
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertNull(
                noticeService.getPendingNotice(
                        1L
                )
        );
        assertTrue(
                notice.isViewed()
        );
    }

    private AppUser user() {
        AppUser user =
                AppUser.createKakaoUser(
                        "provider-1",
                        "사용자",
                        null,
                        null
                );
        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );
        return user;
    }

    private DiaryAutoCompletionNotice notice(
            AppUser user
    ) {
        DiaryAutoCompletionNotice notice =
                DiaryAutoCompletionNotice.create(
                        user,
                        20L,
                        LocalDate.of(
                                2026,
                                8,
                                18
                        ),
                        LocalDateTime.of(
                                2026,
                                8,
                                19,
                                6,
                                0
                        )
                );
        ReflectionTestUtils.setField(
                notice,
                "id",
                30L
        );
        return notice;
    }
}
