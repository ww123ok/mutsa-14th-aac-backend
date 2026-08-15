package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.dto.DevTodayDiaryResetResponse;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import mutsa.hackathon.repository.DiaryShareRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevDiaryResetServiceUserDayTest {

    @Mock
    private DiaryRepository
            diaryRepository;

    @Mock
    private DiaryRewardRepository
            diaryRewardRepository;

    @Mock
    private AiQuestionRepository
            aiQuestionRepository;

    @Mock
    private UserMemoryItemRepository
            userMemoryItemRepository;

    @Mock
    private DiaryShareRepository
            diaryShareRepository;

    @Mock
    private AiMemoryProfileService
            aiMemoryProfileService;

    @Mock
    private UserDayService
            userDayService;

    @InjectMocks
    private DevDiaryResetService
            devDiaryResetService;

    @Test
    void 개발용_오늘_초기화도_사용자_오늘_기준일을_사용한다() {
        LocalDate userDay =
                LocalDate.of(
                        2026,
                        8,
                        13
                );

        when(
                userDayService.currentDay(1L)
        ).thenReturn(
                userDay
        );

        when(
                aiQuestionRepository
                        .deleteAllByUserIdAndQuestionTypeAndAskedDate(
                                1L,
                                AiQuestionType.WRITING_HELP,
                                userDay
                        )
        ).thenReturn(2L);

        when(
                diaryRepository
                        .findByUserIdAndRecordedDate(
                                1L,
                                userDay
                        )
        ).thenReturn(
                Optional.empty()
        );

        DevTodayDiaryResetResponse response =
                devDiaryResetService.resetToday(
                        1L
                );

        assertFalse(
                response.deleted()
        );

        assertEquals(
                userDay,
                response.recordedDate()
        );

        assertEquals(
                2,
                response.deletedQuestionCount()
        );

        verify(
                aiQuestionRepository
        ).deleteAllByUserIdAndQuestionTypeAndAskedDate(
                1L,
                AiQuestionType.WRITING_HELP,
                userDay
        );

        verify(
                diaryRepository
        ).findByUserIdAndRecordedDate(
                1L,
                userDay
        );
    }
}
