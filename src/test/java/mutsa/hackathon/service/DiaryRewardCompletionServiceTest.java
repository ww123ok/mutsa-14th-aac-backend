package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryRewardCompletionServiceTest {

    @Mock
    private DiaryRewardRepository diaryRewardRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DiaryRewardCompletionService diaryRewardCompletionService;

    @Test
    void 색보상_완료후_주간보상_재검사용_이벤트를_발행한다() {
        AppUser user = AppUser.createKakaoUser(
                "provider-1",
                "사용자",
                null,
                null
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        Diary diary = Diary.create(
                user,
                "일요일 기록",
                LocalDate.of(2026, 8, 16),
                true
        );
        ReflectionTestUtils.setField(diary, "id", 10L);

        DiaryReward reward = DiaryReward.createPending(diary);
        ReflectionTestUtils.setField(reward, "id", 20L);

        when(diaryRewardRepository.findById(20L))
                .thenReturn(Optional.of(reward));

        diaryRewardCompletionService.complete(
                20L,
                new DiaryColorReward(
                        "#5577AA",
                        List.of("기록"),
                        "차분한 기록이 이어졌습니다."
                )
        );

        ArgumentCaptor<DiaryRewardCompletedEvent> captor =
                ArgumentCaptor.forClass(DiaryRewardCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        DiaryRewardCompletedEvent event = captor.getValue();
        assertEquals(1L, event.userId());
        assertEquals(10L, event.diaryId());
        assertEquals(20L, event.rewardId());
        assertEquals(LocalDate.of(2026, 8, 16), event.recordedDate());
    }

    @Test
    void 영구삭제되어_보상이_사라진_뒤_완료_콜백이_와도_무시한다() {
        when(diaryRewardRepository.findById(200L))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                diaryRewardCompletionService.complete(
                        200L,
                        new DiaryColorReward(
                                "#5577AA",
                                List.of("기록"),
                                "차분한 기록이 이어졌습니다."
                        )
                )
        );
    }

    @Test
    void 영구삭제되어_보상이_사라진_뒤_실패_콜백이_와도_무시한다() {
        when(diaryRewardRepository.findById(200L))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                diaryRewardCompletionService.fail(
                        200L,
                        "COLOR_GENERATION_FAILED"
                )
        );
    }
}
