package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.DiaryDraft;
import mutsa.hackathon.dto.DiaryDraftResponse;
import mutsa.hackathon.dto.DiaryDraftUpsertRequest;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryDraftRepository;
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
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryDraftServiceTest {

    @Mock
    private DiaryDraftRepository
            diaryDraftRepository;

    @Mock
    private DiaryRepository
            diaryRepository;

    @Mock
    private AppUserRepository
            appUserRepository;

    @Mock
    private UserDayService
            userDayService;

    private DiaryDraftService
            diaryDraftService;

    private static final LocalDate USER_DAY =
            LocalDate.of(
                    2026,
                    8,
                    19
            );

    @BeforeEach
    void setUp() {
        diaryDraftService =
                new DiaryDraftService(
                        diaryDraftRepository,
                        diaryRepository,
                        appUserRepository,
                        userDayService,
                        Clock.fixed(
                                Instant.parse("2026-08-19T00:00:00Z"),
                                ZoneId.of("Asia/Seoul")
                        )
                );

        ReflectionTestUtils.setField(
                diaryDraftService,
                "editingLeaseSeconds",
                90L
        );

        lenient().when(
                userDayService.currentDay(
                        1L
                )
        ).thenReturn(
                USER_DAY
        );
    }

    @Test
    void 새_임시저장본을_현재_DAYBIT_날짜로_저장한다() {
        AppUser user = user();

        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                1L,
                                USER_DAY
                        )
        ).thenReturn(false);

        when(
                diaryDraftRepository
                        .findByUserIdAndRecordedDate(
                                1L,
                                USER_DAY
                        )
        ).thenReturn(Optional.empty());

        when(
                appUserRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                diaryDraftRepository.save(
                        any(DiaryDraft.class)
                )
        ).thenAnswer(invocation -> {
            DiaryDraft draft =
                    invocation.getArgument(0);
            ReflectionTestUtils.setField(
                    draft,
                    "id",
                    10L
            );
            return draft;
        });

        DiaryDraftResponse response =
                diaryDraftService.saveCurrentDraft(
                        1L,
                        new DiaryDraftUpsertRequest(
                                "AM 11:47\n오늘 기록",
                                true
                        )
                );

        assertEquals(
                10L,
                response.draftId()
        );
        assertEquals(
                USER_DAY,
                response.recordedDate()
        );
        assertEquals(
                "AM 11:47\n오늘 기록",
                response.content()
        );
        assertTrue(
                response.personalizationUsesDiaryContent()
        );
    }

    @Test
    void 같은_날_임시저장본이_있으면_내용을_갱신한다() {
        DiaryDraft existing =
                DiaryDraft.create(
                        user(),
                        USER_DAY,
                        "기존 내용",
                        true
                );

        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                1L,
                                USER_DAY
                        )
        ).thenReturn(false);

        when(
                diaryDraftRepository
                        .findByUserIdAndRecordedDate(
                                1L,
                                USER_DAY
                        )
        ).thenReturn(
                Optional.of(existing)
        );

        when(
                diaryDraftRepository.save(
                        existing
                )
        ).thenReturn(
                existing
        );

        diaryDraftService.saveCurrentDraft(
                1L,
                new DiaryDraftUpsertRequest(
                        "새 내용",
                        false
                )
        );

        assertEquals(
                "새 내용",
                existing.getContent()
        );
        assertEquals(
                false,
                existing
                        .shouldUseDiaryContentForPersonalization()
        );
        verify(
                appUserRepository,
                never()
        ).findById(1L);
    }

    @Test
    void 오늘_일기가_이미_완료됐다면_임시저장을_거부한다() {
        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                1L,
                                USER_DAY
                        )
        ).thenReturn(true);

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                diaryDraftService
                                        .saveCurrentDraft(
                                                1L,
                                                new DiaryDraftUpsertRequest(
                                                        "이미 완료됨",
                                                        true
                                                )
                                        )
                );

        assertEquals(
                ErrorCode
                        .DIARY_ALREADY_WRITTEN_TODAY,
                exception.getErrorCode()
        );
    }

    @Test
    void draftId를_보내면_하루경계를_넘겨도_기존_recordedDate를_유지한다() {
        LocalDate previousDay = USER_DAY.minusDays(1);
        DiaryDraft existing = DiaryDraft.create(
                user(),
                previousDay,
                "경계 전 내용",
                true
        );
        ReflectionTestUtils.setField(existing, "id", 10L);

        when(
                diaryDraftRepository.findByIdAndUserId(
                        10L,
                        1L
                )
        ).thenReturn(Optional.of(existing));
        when(
                diaryRepository.existsByUserIdAndRecordedDate(
                        1L,
                        previousDay
                )
        ).thenReturn(false);
        when(diaryDraftRepository.save(existing))
                .thenReturn(existing);

        DiaryDraftResponse response = diaryDraftService.saveCurrentDraft(
                1L,
                new DiaryDraftUpsertRequest(
                        10L,
                        "경계 후에도 계속 작성",
                        true
                )
        );

        assertEquals(previousDay, response.recordedDate());
        assertEquals("경계 후에도 계속 작성", response.content());
        verify(userDayService, never()).currentDay(1L);
    }


    @Test
    void 완료준비는_draft의_기존날짜를_반환하고_작성lease를_연장한다() {
        LocalDate previousDay = USER_DAY.minusDays(1);
        DiaryDraft existing = DiaryDraft.create(
                user(),
                previousDay,
                "경계 전 내용",
                true
        );
        ReflectionTestUtils.setField(existing, "id", 10L);

        when(
                diaryDraftRepository.findByIdAndUserId(10L, 1L)
        ).thenReturn(Optional.of(existing));
        when(
                diaryRepository.existsByUserIdAndRecordedDate(
                        1L,
                        previousDay
                )
        ).thenReturn(false);

        LocalDate recordedDate = diaryDraftService.prepareForCompletion(
                1L,
                10L
        );

        assertEquals(previousDay, recordedDate);
        assertTrue(existing.getEditingActiveUntil() != null);
    }

    @Test
    void heartbeat는_작성중_lease를_연장한다() {
        DiaryDraft existing = DiaryDraft.create(
                user(),
                USER_DAY,
                "작성 중",
                true
        );
        ReflectionTestUtils.setField(existing, "id", 10L);

        when(
                diaryDraftRepository.findByIdAndUserId(10L, 1L)
        ).thenReturn(Optional.of(existing));

        DiaryDraftResponse response = diaryDraftService.keepEditing(
                1L,
                10L
        );

        assertTrue(response.editingActiveUntil() != null);
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
}
