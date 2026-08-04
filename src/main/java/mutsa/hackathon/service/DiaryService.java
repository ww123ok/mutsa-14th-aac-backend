package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.dto.DiaryResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private static final String FALLBACK_REFLECTION_QUESTION =
            "오늘의 기록에서 가장 오래 마음에 남은 순간은 무엇인가요?";

    private final DiaryRepository diaryRepository;
    private final DiaryRewardRepository diaryRewardRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public DiaryCreateResponse create(
            Long userId,
            DiaryCreateRequest request
    ) {
        LocalDate today = LocalDate.now(SERVICE_ZONE);

        validateDiaryNotWrittenToday(userId, today);

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new ProjectException(ErrorCode.USER_NOT_FOUND)
                );

        Diary diary = saveDiary(user, request.content(), today);

        /*
         * OpenAI 연동 전 임시 상태임.
         * AI 연동 후에는 PENDING 생성 -> AI 호출 -> complete(...) 순서로 변경 예정.
         */
        DiaryReward reward = diaryRewardRepository.save(
                DiaryReward.createPending(diary)
        );

        /*
         * 성찰 질문은 반드시 화면에 표시되어야 하므로
         * AI 연동 전이나 AI 장애 상황에도 fallback 질문을 저장한다.
         */
        AiQuestion reflectionQuestion = aiQuestionRepository.save(
                AiQuestion.createReflection(
                        user,
                        diary,
                        FALLBACK_REFLECTION_QUESTION,
                        today,
                        QuestionGenerationSource.FALLBACK
                )
        );

        return DiaryCreateResponse.from(
                diary,
                reward,
                reflectionQuestion
        );
    }

    @Transactional(readOnly = true)
    public List<DiaryResponse> getMonthlyDiaries(
            Long userId,
            int year,
            int month
    ) {
        YearMonth yearMonth = YearMonth.of(year, month);

        return diaryRepository
                .findAllByUserIdAndRecordedDateBetweenAndDeletedFalseOrderByRecordedDateAsc(
                        userId,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                )
                .stream()
                .map(DiaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiaryResponse getDiary(
            Long userId,
            Long diaryId
    ) {
        return DiaryResponse.from(
                findActiveDiary(userId, diaryId)
        );
    }

    @Transactional
    public void deleteDiary(
            Long userId,
            Long diaryId
    ) {
        findActiveDiary(userId, diaryId).softDelete();
    }

    private void validateDiaryNotWrittenToday(
            Long userId,
            LocalDate today
    ) {
        /*
         * 삭제된 일기도 포함해 검사한다.
         * 삭제 후 같은 날짜에 다시 쓸 수 없기 때문.
         */
        if (diaryRepository.existsByUserIdAndRecordedDate(
                userId,
                today
        )) {
            throw new ProjectException(
                    ErrorCode.DIARY_ALREADY_WRITTEN_TODAY
            );
        }
    }

    private Diary saveDiary(
            AppUser user,
            String content,
            LocalDate today
    ) {
        try {
            /*
             * saveAndFlush를 사용해 UNIQUE 제약 오류를
             * 현재 메서드 안에서 즉시 확인한다.
             *
             * 동시에 요청 두 개가 들어와 서비스 검사를 둘 다
             * 통과하더라도 DB UNIQUE 제약이 마지막으로 막는다.
             */
            return diaryRepository.saveAndFlush(
                    Diary.create(user, content, today)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ProjectException(
                    ErrorCode.DIARY_ALREADY_WRITTEN_TODAY
            );
        }
    }

    private Diary findActiveDiary(
            Long userId,
            Long diaryId
    ) {
        return diaryRepository
                .findByIdAndUserIdAndDeletedFalse(
                        diaryId,
                        userId
                )
                .orElseThrow(() ->
                        new ProjectException(ErrorCode.DIARY_NOT_FOUND)
                );
    }
}