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
                        .orElse(null);

        /*
         * 휴지통에서 영구 삭제된 직후 비동기 이벤트가
         * 늦게 도착할 수 있으므로 이미 제거된 보상은
         * 정상적인 취소 상태로 간주합니다.
         */
        if (reward == null) {
            return;
        }

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