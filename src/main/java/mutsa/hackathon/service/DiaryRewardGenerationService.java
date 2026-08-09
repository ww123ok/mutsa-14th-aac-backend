package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.RewardGenerationStatus;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryRewardGenerationService {

    private static final String
            FAILURE_REASON =
            "COLOR_GENERATION_FAILED";

    private final DiaryRewardRepository
            diaryRewardRepository;

    private final DiaryColorRewardGenerator
            diaryColorRewardGenerator;

    private final DiaryRewardCompletionService
            diaryRewardCompletionService;

    /**
     * OpenAI 등 외부 요청은 DB 트랜잭션 바깥에서 실행합니다.
     *
     * 외부 API 응답을 기다리는 동안 데이터베이스 연결을
     * 점유하지 않기 위한 구조입니다.
     */
    public void generate(Long rewardId) {
        DiaryReward reward =
                diaryRewardRepository
                        .findByIdWithDiary(rewardId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 색 보상입니다."
                                )
                        );

        if (
                reward.getGenerationStatus()
                        != RewardGenerationStatus.PENDING
        ) {
            return;
        }

        try {
            DiaryColorReward generatedReward =
                    diaryColorRewardGenerator
                            .generate(
                                    reward.getDiary()
                                            .getContent()
                            );

            diaryRewardCompletionService.complete(
                    rewardId,
                    generatedReward
            );

        } catch (RuntimeException exception) {
            /*
             * 일기 본문이나 OpenAI 응답 본문은 로그에
             * 기록하지 않습니다.
             */
            log.warn(
                    "Diary reward generation failed: rewardId={}, reason={}",
                    rewardId,
                    exception.getClass()
                            .getSimpleName()
            );

            diaryRewardCompletionService.fail(
                    rewardId,
                    FAILURE_REASON
            );
        }
    }
}