package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryRewardGenerationServiceTest {

    @Mock
    private DiaryRewardRepository
            diaryRewardRepository;

    @Mock
    private DiaryColorRewardGenerator
            diaryColorRewardGenerator;

    @Mock
    private DiaryRewardCompletionService
            diaryRewardCompletionService;

    @InjectMocks
    private DiaryRewardGenerationService
            diaryRewardGenerationService;

    @Test
    void PENDING_보상의_색과_키워드를_생성하고_완료_처리한다() {
        DiaryReward reward =
                createPendingReward();

        DiaryColorReward generatedReward =
                new DiaryColorReward(
                        "#73D8B4",
                        List.of(
                                "해결",
                                "성취",
                                "안도"
                        ),
                        "문제를 해결해서 뿌듯하셨군요."
                );

        when(
                diaryRewardRepository
                        .findByIdWithDiary(200L)
        ).thenReturn(
                Optional.of(reward)
        );

        when(
                diaryColorRewardGenerator
                        .generate(
                                "오늘은 팀원들과 문제를 해결해서 뿌듯했다."
                        )
        ).thenReturn(generatedReward);

        diaryRewardGenerationService.generate(
                200L
        );

        verify(
                diaryRewardCompletionService
        ).complete(
                200L,
                generatedReward
        );

        verify(
                diaryRewardCompletionService,
                never()
        ).fail(
                200L,
                "COLOR_GENERATION_FAILED"
        );
    }

    @Test
    void 색_생성에_실패하면_FAILED_처리를_요청한다() {
        DiaryReward reward =
                createPendingReward();

        when(
                diaryRewardRepository
                        .findByIdWithDiary(200L)
        ).thenReturn(
                Optional.of(reward)
        );

        when(
                diaryColorRewardGenerator
                        .generate(
                                "오늘은 팀원들과 문제를 해결해서 뿌듯했다."
                        )
        ).thenThrow(
                new IllegalStateException(
                        "OpenAI 호출 실패"
                )
        );

        diaryRewardGenerationService.generate(
                200L
        );

        verify(
                diaryRewardCompletionService
        ).fail(
                200L,
                "COLOR_GENERATION_FAILED"
        );

        verify(
                diaryRewardCompletionService,
                never()
        ).complete(
                200L,
                null
        );
    }

    @Test
    void 영구삭제되어_이미_없는_보상의_비동기_이벤트는_무시한다() {
        when(
                diaryRewardRepository
                        .findByIdWithDiary(200L)
        ).thenReturn(
                Optional.empty()
        );

        diaryRewardGenerationService.generate(
                200L
        );

        verifyNoInteractions(
                diaryColorRewardGenerator,
                diaryRewardCompletionService
        );
    }

    @Test
    void 이미_완료된_보상은_다시_생성하지_않는다() {
        DiaryReward reward =
                createPendingReward();

        reward.complete(
                "#73D8B4",
                List.of(
                        "해결",
                        "안도"
                ),
                "오류 해결 장면이 또렷하게 이어져 선명한 방향의 색을 골랐습니다."
        );

        when(
                diaryRewardRepository
                        .findByIdWithDiary(200L)
        ).thenReturn(
                Optional.of(reward)
        );

        diaryRewardGenerationService.generate(
                200L
        );

        verifyNoInteractions(
                diaryColorRewardGenerator,
                diaryRewardCompletionService
        );
    }

    private DiaryReward createPendingReward() {
        AppUser user =
                AppUser.createKakaoUser(
                        "reward-generation-test",
                        "데이빗",
                        null,
                        null
                );

        Diary diary =
                Diary.create(
                        user,
                        "오늘은 팀원들과 문제를 해결해서 뿌듯했다.",
                        LocalDate.now()
                );

        ReflectionTestUtils.setField(
                diary,
                "id",
                100L
        );

        DiaryReward reward =
                DiaryReward.createPending(
                        diary
                );

        ReflectionTestUtils.setField(
                reward,
                "id",
                200L
        );

        return reward;
    }
}