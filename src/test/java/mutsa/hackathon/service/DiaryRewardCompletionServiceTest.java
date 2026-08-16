package mutsa.hackathon.service;

import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryRewardCompletionServiceTest {

    @Mock
    private DiaryRewardRepository diaryRewardRepository;

    @InjectMocks
    private DiaryRewardCompletionService diaryRewardCompletionService;

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
