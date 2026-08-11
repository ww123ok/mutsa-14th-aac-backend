package mutsa.hackathon.service;

import mutsa.hackathon.domain.DiaryRewardPolicy;

import java.util.List;

/**
 * 색 보상 생성기가 반환하는 검증된 결과.
 * colorHex:
 * DAYBIT UI 예약 색상을 제외한 #RRGGBB 색상
 * keywords:
 * 일기와 색의 연결 단서를 보여주는 1~3개의 짧은 키워드
 */
public record DiaryColorReward(
        String colorHex,
        List<String> keywords
) {

    public DiaryColorReward {
        colorHex =
                DiaryRewardPolicy
                        .normalizeColorHex(
                                colorHex
                        );

        keywords =
                DiaryRewardPolicy
                        .normalizeKeywords(
                                keywords
                        );
    }
}