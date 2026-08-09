package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.dto.DiaryRewardResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryRewardService {

    private final DiaryRepository diaryRepository;

    private final DiaryRewardRepository
            diaryRewardRepository;

    @Transactional(readOnly = true)
    public DiaryRewardResponse getReward(
            Long userId,
            Long diaryId
    ) {
        Diary diary = diaryRepository
                .findByIdAndUserIdAndDeletedFalse(
                        diaryId,
                        userId
                )
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.DIARY_NOT_FOUND
                        )
                );

        DiaryReward reward =
                diaryRewardRepository
                        .findByDiaryId(
                                diary.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "저장된 일기에 색상 보상 정보가 없습니다."
                                )
                        );

        return DiaryRewardResponse.from(
                reward
        );
    }
}