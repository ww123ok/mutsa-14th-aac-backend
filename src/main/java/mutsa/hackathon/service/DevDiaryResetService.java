package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.dto.DevTodayDiaryResetResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import mutsa.hackathon.repository.DiaryShareRepository;
import mutsa.hackathon.repository.SharedDiaryLogRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.dev",
        name = "reset-enabled",
        havingValue = "true"
)
public class DevDiaryResetService {

    private final DiaryRepository
            diaryRepository;

    private final DiaryRewardRepository
            diaryRewardRepository;

    private final AiQuestionRepository
            aiQuestionRepository;

    private final UserMemoryItemRepository
            userMemoryItemRepository;

    private final DiaryShareRepository
            diaryShareRepository;

    private final SharedDiaryLogRepository
            sharedDiaryLogRepository;

    private final AiMemoryProfileService
            aiMemoryProfileService;

    private final UserDayService
            userDayService;

    @Transactional
    public DevTodayDiaryResetResponse resetToday(
            Long userId
    ) {
        LocalDate today =
                userDayService.currentDay(
                        userId
                );

        long deletedWritingHelpQuestionCount =
                aiQuestionRepository
                        .deleteAllByUserIdAndQuestionTypeAndAskedDate(
                                userId,
                                AiQuestionType.WRITING_HELP,
                                today
                        );

        Optional<Diary> diaryOptional =
                diaryRepository
                        .findByUserIdAndRecordedDate(
                                userId,
                                today
                        );

        if (diaryOptional.isEmpty()) {
            return DevTodayDiaryResetResponse
                    .notFound(
                            today,
                            deletedWritingHelpQuestionCount
                    );
        }

        Diary diary =
                diaryOptional.get();

        Long diaryId =
                diary.getId();

        /*
         * 공유 데이터와 크레딧 이력은 별도의 생명주기를
         * 가지므로 이미 전달되었거나 공유 보상 크레딧이
         * 지급된 경험조각은 개발용 초기화로 삭제하지 않는다.
         */
        deleteUnreceivedDiaryShare(
                diaryId
        );

        long deletedMemoryCount =
                userMemoryItemRepository
                        .deleteAllByUserIdAndSourceDiaryId(
                                userId,
                                diaryId
                        );

        long deletedReflectionQuestionCount =
                aiQuestionRepository
                        .deleteAllByDiaryId(
                                diaryId
                        );

        long deletedQuestionCount =
                deletedWritingHelpQuestionCount
                        + deletedReflectionQuestionCount;

        long deletedRewardCount =
                diaryRewardRepository
                        .deleteAllByDiaryId(
                                diaryId
                        );

        diaryRepository.delete(
                diary
        );

        /*
         * 기억 후보 및 일기 삭제를 DB에 먼저 반영한 뒤,
         * 남아 있는 APPROVED 기억만으로 프로필을 재생성
         */
        diaryRepository.flush();

        aiMemoryProfileService.rebuildProfile(
                userId
        );

        return DevTodayDiaryResetResponse.deleted(
                diaryId,
                today,
                deletedRewardCount,
                deletedQuestionCount,
                deletedMemoryCount
        );
    }

    /**
     * 개발용 초기화에서는 아직 다른 사용자에게 전달되지 않은
     * 경험조각만 함께 제거한다.
     *
     * 전달 이력 또는 공유 보상 크레딧이 있으면
     * 다른 테스트 데이터의 정합성을 위해 초기화를 차단한다.
     */
    private void deleteUnreceivedDiaryShare(
            Long diaryId
    ) {
        diaryShareRepository
                .findByDiaryId(
                        diaryId
                )
                .ifPresent(share -> {
                    boolean alreadyDelivered =
                            sharedDiaryLogRepository
                                    .existsByDiaryShareId(
                                            share.getId()
                                    );

                    boolean shareRewardGranted =
                            share.getEarnedCredit() > 0;

                    if (
                            alreadyDelivered
                                    || shareRewardGranted
                    ) {
                        throw new ProjectException(
                                ErrorCode
                                        .DEV_DIARY_RESET_SHARED_DIARY_BLOCKED
                        );
                    }

                    diaryShareRepository.delete(
                            share
                    );

                    diaryShareRepository.flush();
                });
    }
}
