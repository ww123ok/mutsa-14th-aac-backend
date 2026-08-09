package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.RewardGenerationStatus;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryRewardCompletionService {

    private final DiaryRewardRepository
            diaryRewardRepository;

    /**
     * 생성이 성공한 경우 PENDING 보상을 완료함.
     * 동일 이벤트가 중복 처리되더라도 이미 처리된
     * 보상은 변경하지 않음.
     */
    @Transactional
    public void complete(
            Long rewardId,
            DiaryColorReward generatedReward
    ) {
        if (generatedReward == null) {
            throw new IllegalArgumentException(
                    "생성된 색 보상은 필수입니다."
            );
        }

        DiaryReward reward =
                findReward(rewardId);

        if (
                reward.getGenerationStatus()
                        != RewardGenerationStatus.PENDING
        ) {
            return;
        }

        reward.complete(
                generatedReward.colorHex(),
                generatedReward.colorName()
        );
    }

    /**
     * 생성이 실패한 경우 PENDING 보상을 실패 처리
     */
    @Transactional
    public void fail(
            Long rewardId,
            String failureReason
    ) {
        DiaryReward reward =
                findReward(rewardId);

        if (
                reward.getGenerationStatus()
                        != RewardGenerationStatus.PENDING
        ) {
            return;
        }

        reward.fail(failureReason);
    }

    private DiaryReward findReward(
            Long rewardId
    ) {
        return diaryRewardRepository
                .findById(rewardId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 색 보상입니다."
                        )
                );
    }
}