package mutsa.hackathon.service;

import mutsa.hackathon.domain.DiaryRewardPolicy;

import java.util.List;

/**
 * 색 보상 생성기가 반환하는 검증된 결과.
 * colorHex:
 * DAYBIT UI 예약 색상을 제외한 #RRGGBB 색상
 * keywords:
 * 일기의 감정·감각·분위기를 보여주는 1~3개의 짧은 키워드
 * commentSummary:
 * 일기에서 비중이 큰 사실 1~2개를 의미 확장 없이 압축한 공감체 한 문장
 */
public record DiaryColorReward(
        String colorHex,
        List<String> keywords,
        String commentSummary
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

        commentSummary =
                DiaryRewardPolicy
                        .normalizeCommentSummary(
                                commentSummary
                        );
    }
}