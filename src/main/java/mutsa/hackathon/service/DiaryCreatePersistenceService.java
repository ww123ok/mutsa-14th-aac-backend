package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 일기 작성 과정에서 DB 작업만 담당.
 * 외부 OpenAI 호출은 이 서비스에 두지 않음.
 * 따라서 트랜잭션이 열린 상태에서 외부 API 응답을
 * 기다리지 않도록 일기 작성 흐름을 분리.
 */
@Service
@RequiredArgsConstructor
public class DiaryCreatePersistenceService {

    private final DiaryRepository
            diaryRepository;

    private final DiaryRewardRepository
            diaryRewardRepository;

    private final AiQuestionRepository
            aiQuestionRepository;

    private final AppUserRepository
            appUserRepository;

    private final ApplicationEventPublisher
            eventPublisher;

    /**
     * OpenAI 호출 전에 불필요한 API 요청을 피하기 위해
     * 사용자와 오늘 일기 작성 가능 여부를 먼저 확인.
     * 이 read-only transaction은 메서드가 끝나는 순간 종료.
     */
    @Transactional(readOnly = true)
    public void validateCanCreate(
            Long userId,
            LocalDate recordedDate
    ) {
        validateDiaryNotWritten(
                userId,
                recordedDate
        );

        if (
                userId == null
                        || !appUserRepository
                        .existsById(userId)
        ) {
            throw new ProjectException(
                    ErrorCode.USER_NOT_FOUND
            );
        }
    }

    /**
     * 이미 생성된 성찰 질문 결과와 함께
     * 일기/보상/성찰 질문을 하나의 짧은 transaction에서 저장.
     * 이 메서드 안에서는 외부 API를 호출하지 않음.
     */
    @Transactional
    public DiaryCreateResponse persist(
            Long userId,
            DiaryCreateRequest request,
            LocalDate recordedDate,
            String reflectionQuestionText,
            QuestionGenerationSource
                    reflectionGenerationSource
    ) {
        /*
         * OpenAI 호출 사이에 다른 요청이 먼저 일기를
         * 생성했을 가능성이 있으므로 한 번 더 검증
         */
        validateDiaryNotWritten(
                userId,
                recordedDate
        );

        AppUser user =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode.USER_NOT_FOUND
                                )
                        );

        Diary diary =
                saveDiary(
                        user,
                        request.content(),
                        recordedDate
                );

        DiaryReward reward =
                diaryRewardRepository.save(
                        DiaryReward.createPending(
                                diary
                        )
                );

        AiQuestion reflectionQuestion =
                aiQuestionRepository.save(
                        AiQuestion.createReflection(
                                user,
                                diary,
                                reflectionQuestionText,
                                recordedDate,
                                reflectionGenerationSource
                        )
                );

        /*
         * AFTER_COMMIT listener가 사용하는 이벤트이므로
         * 반드시 이 transaction 내부에서 발행
         */
        eventPublisher.publishEvent(
                new DiaryRewardGenerationRequested(
                        reward.getId()
                )
        );

        eventPublisher.publishEvent(
                new ExperienceFragmentMatchingRequested(
                        diary.getId()
                )
        );

        if (
                request
                        .shouldUseDiaryContentForPersonalization()
                        && user.isAiMemoryConsent()
        ) {
            eventPublisher.publishEvent(
                    new DiaryMemoryExtractionRequested(
                            diary.getId()
                    )
            );
        }

        return DiaryCreateResponse.from(
                diary,
                reward,
                reflectionQuestion
        );
    }

    private void validateDiaryNotWritten(
            Long userId,
            LocalDate recordedDate
    ) {
        if (
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                userId,
                                recordedDate
                        )
        ) {
            throw new ProjectException(
                    ErrorCode
                            .DIARY_ALREADY_WRITTEN_TODAY
            );
        }
    }

    private Diary saveDiary(
            AppUser user,
            String content,
            LocalDate recordedDate
    ) {
        try {
            return diaryRepository
                    .saveAndFlush(
                            Diary.create(
                                    user,
                                    content,
                                    recordedDate
                            )
                    );

        } catch (
                DataIntegrityViolationException exception
        ) {
            /*
             * validate와 실제 INSERT 사이에 발생할 수 있는
             * 동시 요청도 DB unique constraint로 최종 방어
             */
            throw new ProjectException(
                    ErrorCode
                            .DIARY_ALREADY_WRITTEN_TODAY
            );
        }
    }
}
