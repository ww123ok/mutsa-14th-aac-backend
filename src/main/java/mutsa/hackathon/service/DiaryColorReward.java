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
 * 일기 속 시각적 단서와 명시된 감정을 바탕으로 오늘의 색이 선택된 이유를
 * 2~3개의 짧은 한국어 문장으로 설명하며 마지막은 반드시 온점(.)으로 종료
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
                        .normalizeColorCommentSummary(
                                commentSummary
                        );
    }
}
