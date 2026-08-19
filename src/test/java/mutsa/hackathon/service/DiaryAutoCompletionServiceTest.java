package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.DiaryDraft;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryAutoCompletionServiceTest {

    @Mock
    private DiaryDraftRepository
            diaryDraftRepository;

    @Mock
    private DiaryRepository
            diaryRepository;

    @Mock
    private DiaryService
            diaryService;

    @Mock
    private AppUserRepository
            appUserRepository;

    private DiaryAutoCompletionService
            autoCompletionService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse(
                        "2026-08-18T21:01:00Z"
                ),
                ZoneId.of(
                        "Asia/Seoul"
                )
        );

        UserDayService userDayService =
                new UserDayService(
                        appUserRepository,
                        clock
                );

        autoCompletionService =
                new DiaryAutoCompletionService(
                        diaryDraftRepository,
                        diaryRepository,
                        diaryService,
                        userDayService,
                        clock
                );
    }

    @Test
    void 사용자_하루전환시간을_넘긴_임시저장본을_자동완료한다() {
        DiaryDraft draft = draft(
                LocalDate.of(
                        2026,
                        8,
                        18
                )
        );

        when(
                diaryDraftRepository
                        .findByIdWithUser(10L)
        ).thenReturn(
                Optional.of(draft)
        );

        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                1L,
                                LocalDate.of(
                                        2026,
                                        8,
                                        18
                                )
                        )
        ).thenReturn(false);

        when(
                diaryService
                        .autoCompleteForRecordedDate(
                                eq(1L),
                                eq(
                                        new DiaryCreateRequest(
                                                "작성 중이던 내용",
                                                true
                                        )
                                ),
                                eq(
                                        LocalDate.of(
                                                2026,
                                                8,
                                                18
                                        )
                                )
                        )
        ).thenReturn(
                new DiaryCreateResponse(
                        20L,
                        LocalDate.of(
                                2026,
                                8,
                                18
                        ),
                        null,
                        null,
                        null
                )
        );

        DiaryAutoCompletionService
                .AutoCompletionResult result =
                autoCompletionService
                        .autoCompleteIfDue(
                                10L
                        );

        assertTrue(
                result.autoCompleted()
        );
        assertTrue(
                result.processed()
        );
    }

    @Test
    void 하루경계를_넘겼어도_편집_heartbeat가_살아있으면_자동완료하지_않는다() {
        DiaryDraft draft = draft(
                LocalDate.of(2026, 8, 18)
        );
        draft.markEditingActiveUntil(
                LocalDateTime.of(2026, 8, 19, 6, 2)
        );

        when(
                diaryDraftRepository.findByIdWithUser(10L)
        ).thenReturn(Optional.of(draft));

        DiaryAutoCompletionService.AutoCompletionResult result =
                autoCompletionService.autoCompleteIfDue(10L);

        assertFalse(result.processed());
        verify(diaryService, never()).autoCompleteForRecordedDate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void 아직_같은_DAYBIT_날짜라면_자동완료하지_않는다() {
        DiaryDraft draft = draft(
                LocalDate.of(
                        2026,
                        8,
                        19
                )
        );

        when(
                diaryDraftRepository
                        .findByIdWithUser(10L)
        ).thenReturn(
                Optional.of(draft)
        );

        DiaryAutoCompletionService
                .AutoCompletionResult result =
                autoCompletionService
                        .autoCompleteIfDue(
                                10L
                        );

        assertFalse(
                result.processed()
        );

        verify(
                diaryService,
                never()
        ).autoCompleteForRecordedDate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void 이미_같은날짜_일기가_있으면_오래된_임시저장본만_제거한다() {
        DiaryDraft draft = draft(
                LocalDate.of(
                        2026,
                        8,
                        18
                )
        );

        when(
                diaryDraftRepository
                        .findByIdWithUser(10L)
        ).thenReturn(
                Optional.of(draft)
        );

        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                1L,
                                LocalDate.of(
                                        2026,
                                        8,
                                        18
                                )
                        )
        ).thenReturn(true);

        DiaryAutoCompletionService
                .AutoCompletionResult result =
                autoCompletionService
                        .autoCompleteIfDue(
                                10L
                        );

        assertTrue(
                result.staleDraftRemoved()
        );
        verify(
                diaryDraftRepository
        ).deleteById(
                10L
        );
        verify(
                diaryService,
                never()
        ).autoCompleteForRecordedDate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private DiaryDraft draft(
            LocalDate recordedDate
    ) {
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

        user.updatePersonalSettings(
                "사용자",
                "학생",
                LocalTime.of(
                        21,
                        0
                ),
                LocalTime.of(
                        6,
                        0
                ),
                false
        );

        DiaryDraft draft =
                DiaryDraft.create(
                        user,
                        recordedDate,
                        "작성 중이던 내용",
                        true
                );

        ReflectionTestUtils.setField(
                draft,
                "id",
                10L
        );

        return draft;
    }
}
