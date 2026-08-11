package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryMemoryApplicationService {

    private final DiaryRepository
            diaryRepository;

    private final UserMemoryItemRepository
            userMemoryItemRepository;

    private final AiMemoryProfileService
            aiMemoryProfileService;

    /**
     * 특정 일기에서 안전하게 추출된 PENDING 기억 후보를
     * 승인하고 사용자 개인화 프로필을 다시 생성.
     * 해당 일기에 대해 이미 반영이 끝났다면
     * 아무 작업도 하지 않아 이벤트 중복 처리에도 안전함.
     */
    @Transactional
    public int apply(
            Long diaryId
    ) {
        Diary diary =
                diaryRepository
                        .findByIdWithUser(
                                diaryId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 일기입니다."
                                )
                        );

        if (diary.isDeleted()) {
            return 0;
        }

        if (
                diary.getMemoryAppliedAt()
                        != null
        ) {
            return 0;
        }

        if (
                !diary.getUser()
                        .isAiMemoryConsent()
        ) {
            return 0;
        }

        List<UserMemoryItem> memories =
                userMemoryItemRepository
                        .findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
                                diary.getUser()
                                        .getId(),
                                diary.getId()
                        );

        int approvedCount = 0;

        for (
                UserMemoryItem memory
                : memories
        ) {
            if (
                    memory.getStatus()
                            != UserMemoryStatus.PENDING
            ) {
                continue;
            }

            memory.approve();
            approvedCount++;
        }

        /*
         * profile 재조회 전에 APPROVED 변경사항을
         * DB에 먼저 반영
         */
        userMemoryItemRepository.flush();

        aiMemoryProfileService.rebuildProfile(
                diary.getUser()
                        .getId()
        );

        /*
         * 후보가 0개였더라도 AI 추출 자체가 정상 완료된
         * 일기라는 사실은 기록
         */
        diary.markMemoryApplied();

        return approvedCount;
    }
}