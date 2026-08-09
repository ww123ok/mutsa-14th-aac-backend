package mutsa.hackathon.service;

@FunctionalInterface
public interface DiaryColorRewardGenerator {

    DiaryColorReward generate(
            String diaryContent
    );
}