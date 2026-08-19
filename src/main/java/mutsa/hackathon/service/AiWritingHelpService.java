package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.WritingHelpQuestionHistoryResponse;
import mutsa.hackathon.dto.WritingHelpQuestionRequest;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.dto.WritingHelpStatusResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.WritingHelpRecentDiaryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiWritingHelpService {

    private static final int DAILY_LIMIT = 3;

    /**
     * 최근 맥락은 현재 DAYBIT 날짜를 제외한 최근 1주일을 기준으로 사용.
     */
    private static final int RECENT_CONTEXT_DAYS = 7;

    private final AppUserRepository
            appUserRepository;

    private final AiQuestionRepository
            aiQuestionRepository;

    private final WritingHelpRecentDiaryRepository
            writingHelpRecentDiaryRepository;

    private final WritingHelpQuestionGenerator
            writingHelpQuestionGenerator;

    private final WritingHelpGenericQuestionProvider
            writingHelpGenericQuestionProvider;

    private final UserDayService
            userDayService;

    @Transactional(readOnly = true)
    public WritingHelpStatusResponse getStatus(
            Long userId
    ) {
        LocalDate today =
                userDayService.currentDay(
                        userId
                );

        long usedCount =
                countTodayQuestions(
                        userId,
                        today
                );

        return WritingHelpStatusResponse.of(
                DAILY_LIMIT,
                usedCount
        );
    }

    /**
     * 현재 사용자가 오늘 이미 받은 작성 도움 질문을
     * 생성 순서대로 조회한다.
     *
     * userId + WRITING_HELP + 현재 DAYBIT 날짜 조건으로 조회하므로
     * 계정 전환 시 다른 사용자의 질문이 반환되지 않는다.
     */
    @Transactional(readOnly = true)
    public List<WritingHelpQuestionHistoryResponse>
    getTodayQuestionHistory(
            Long userId
    ) {
        LocalDate today =
                userDayService.currentDay(
                        userId
                );

        return aiQuestionRepository
                .findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
                        userId,
                        AiQuestionType.WRITING_HELP,
                        today
                )
                .stream()
                .map(WritingHelpQuestionHistoryResponse::from)
                .toList();
    }

    /**
     * Request Body를 보내지 않는 기존 내부 호출 호환용.
     */
    public WritingHelpQuestionResponse generateQuestion(
            Long userId
    ) {
        return generateQuestion(
                userId,
                null
        );
    }

    /**
     * 작성 도움 질문 선택 정책.
     *
     * 1. currentContent가 있으면 질문 순서와 관계없이 항상 CURRENT_DRAFT
     *    - 버튼을 누른 순간까지 사용자가 직접 작성한 현재 본문을 최우선으로 사용
     *
     * 2. currentContent가 없으면 작성 전 3회 질문의 결이 한쪽으로 몰리지 않도록
     *    질문 순서에 따라 분산
     *    - 최근 맥락이 있으면 1회 RECENT_CONTEXT / 2회 GENERIC / 3회 RECENT_CONTEXT
     *    - 최근 맥락이 없으면 1~3회 모두 GENERIC
     *
     * 3. GENERIC이 여러 번 선택되는 경우에는 같은 날 이미 사용한 범주를 우선 피함
     *
     * 4. 1회와 3회가 모두 RECENT_CONTEXT일 때 최근 일기가 2개 이상이면
     *    3회차에서는 두 번째 최근 일기를 우선 맥락으로 배치하여 같은 사건만
     *    반복해서 묻는 가능성을 낮춤
     *
     * 닉네임/직업/고정 프로필(UserMemoryItem/AiMemoryProfile)은
     * 작성 도움 질문 생성에 사용하지 않음.
     *
     * OpenAI 응답을 기다리는 동안 DB transaction을 유지하지 않기 위해
     * 이 메서드 자체에는 @Transactional을 사용하지 않음.
     */
    public WritingHelpQuestionResponse generateQuestion(
            Long userId,
            WritingHelpQuestionRequest request
    ) {
        LocalDate today =
                userDayService.currentDay(
                        userId
                );

        long usedCount =
                countTodayQuestions(
                        userId,
                        today
                );

        if (usedCount >= DAILY_LIMIT) {
            throw new ProjectException(
                    ErrorCode
                            .WRITING_HELP_LIMIT_EXCEEDED
            );
        }

        AppUser user =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode.USER_NOT_FOUND
                                )
                        );

        List<String> previousQuestions =
                findTodayQuestions(
                        userId,
                        today
                );

        int questionOrder =
                Math.toIntExact(
                        usedCount + 1
                );

        QuestionSelection selection =
                selectQuestion(
                        userId,
                        user,
                        today,
                        request,
                        questionOrder,
                        previousQuestions
                );

        AiQuestion question =
                aiQuestionRepository.save(
                        AiQuestion.createWritingHelp(
                                user,
                                selection.questionText(),
                                questionOrder,
                                today,
                                selection.generationSource()
                        )
                );

        return WritingHelpQuestionResponse.from(
                question,
                DAILY_LIMIT,
                selection.contextType()
        );
    }

    private QuestionSelection selectQuestion(
            Long userId,
            AppUser user,
            LocalDate today,
            WritingHelpQuestionRequest request,
            int questionOrder,
            List<String> previousQuestions
    ) {
        String currentContent =
                request == null
                        ? null
                        : WritingHelpContentSanitizer
                        .sanitize(
                                request
                                        .normalizedCurrentContent()
                        );

        /*
         * 사용자가 실제로 쓰기 시작한 순간부터는 사전 배분보다 현재 초안이 우선.
         * 예: 1회 최근맥락을 본 뒤 본문을 작성하고 2회 버튼을 누르면 GENERIC이 아니라
         * CURRENT_DRAFT 질문을 반환.
         */
        if (
                currentContent != null
                        && !currentContent.isBlank()
        ) {
            return generateCurrentDraftQuestion(
                    currentContent,
                    questionOrder,
                    previousQuestions
            );
        }

        /*
         * 작성 전 두 번째 질문은 최근 맥락 유무와 관계없이 범용 질문으로 고정하여
         * RECENT_CONTEXT가 3회 연속 노출되는 것을 방지.
         * 또한 불필요한 최근 일기 DB 조회/OpenAI 호출도 하지 않음.
         */
        if (questionOrder == 2) {
            return generateGenericQuestion(
                    userId,
                    today,
                    previousQuestions
            );
        }

        List<WritingHelpRecentDiary> recentDiaries =
                findRecentDiaries(
                        userId,
                        user,
                        today
                );

        if (!recentDiaries.isEmpty()) {
            return generateRecentContextQuestion(
                    recentDiaries,
                    questionOrder,
                    previousQuestions
            );
        }

        return generateGenericQuestion(
                userId,
                today,
                previousQuestions
        );
    }

    private QuestionSelection generateCurrentDraftQuestion(
            String currentContent,
            int questionOrder,
            List<String> previousQuestions
    ) {
        String questionText =
                writingHelpQuestionGenerator
                        .generate(
                                new WritingHelpPrompt(
                                        WritingHelpQuestionContextType
                                                .CURRENT_DRAFT,
                                        currentContent,
                                        List.of(),
                                        questionOrder,
                                        previousQuestions
                                )
                        );

        return new QuestionSelection(
                questionText,
                QuestionGenerationSource.AI,
                WritingHelpQuestionContextType
                        .CURRENT_DRAFT
        );
    }

    private QuestionSelection generateRecentContextQuestion(
            List<WritingHelpRecentDiary> recentDiaries,
            int questionOrder,
            List<String> previousQuestions
    ) {
        List<WritingHelpRecentDiary> prioritizedRecentDiaries =
                prioritizeRecentDiaries(
                        recentDiaries,
                        questionOrder
                );

        String questionText =
                writingHelpQuestionGenerator
                        .generate(
                                new WritingHelpPrompt(
                                        WritingHelpQuestionContextType
                                                .RECENT_CONTEXT,
                                        null,
                                        prioritizedRecentDiaries,
                                        questionOrder,
                                        previousQuestions
                                )
                        );

        return new QuestionSelection(
                questionText,
                QuestionGenerationSource.AI,
                WritingHelpQuestionContextType
                        .RECENT_CONTEXT
        );
    }

    private QuestionSelection generateGenericQuestion(
            Long userId,
            LocalDate today,
            List<String> previousQuestions
    ) {
        List<String> excludedGenericQuestions =
                new ArrayList<>(
                        previousQuestions
                );

        /*
         * 범용 질문은 고정 풀이므로 최근 12개의 과거 질문까지 정확한 문구 중복 후보에서
         * 제외합니다. 다만 카테고리 다양성은 오늘 질문만 기준으로 계산하여 과거에 사용한
         * 범주 전체가 며칠 동안 막히는 일은 방지.
         */
        excludedGenericQuestions.addAll(
                findEarlierQuestions(
                        userId,
                        today
                )
        );

        String questionText =
                writingHelpGenericQuestionProvider
                        .nextQuestion(
                                excludedGenericQuestions,
                                previousQuestions
                        );

        return new QuestionSelection(
                questionText,
                QuestionGenerationSource.PREDEFINED,
                WritingHelpQuestionContextType.GENERIC
        );
    }

    /**
     * 최근 일기는 Repository에서 최신순으로 최대 3개가 들어옴.
     *
     * 1회차 최근맥락: 최신 일기를 최우선
     * 3회차 최근맥락: 최근 일기가 2개 이상이면 두 번째 최신 일기를 최우선
     *
     * 나머지 일기도 뒤에 유지해서 우선 일기에 자연스럽게 이어 물을 맥락이 거의 없는 경우
     * 모델이 다른 최근 일기로 안전하게 fallback할 수 있도록 함.
     */
    private List<WritingHelpRecentDiary> prioritizeRecentDiaries(
            List<WritingHelpRecentDiary> recentDiaries,
            int questionOrder
    ) {
        if (
                recentDiaries == null
                        || recentDiaries.size() <= 1
                        || questionOrder != 3
        ) {
            return recentDiaries == null
                    ? List.of()
                    : List.copyOf(recentDiaries);
        }

        List<WritingHelpRecentDiary> prioritized =
                new ArrayList<>(
                        recentDiaries.size()
                );

        prioritized.add(
                recentDiaries.get(1)
        );

        for (
                int index = 2;
                index < recentDiaries.size();
                index++
        ) {
            prioritized.add(
                    recentDiaries.get(index)
            );
        }

        prioritized.add(
                recentDiaries.get(0)
        );

        return List.copyOf(prioritized);
    }

    private List<WritingHelpRecentDiary>
    findRecentDiaries(
            Long userId,
            AppUser user,
            LocalDate today
    ) {
        /*
         * 최근 과거 일기의 직접 재사용은 개인화 기능이므로
         * 현재 전역 동의가 꺼져 있으면 절대 조회/전달하지 않음.
         *
         * CURRENT_DRAFT는 사용자가 해당 요청에서 직접 보내는 데이터이므로
         * 이 전역 기억 동의와 별개로 즉시 질문 생성에만 사용할 수 있음.
         */
        if (!user.isAiMemoryConsent()) {
            return List.of();
        }

        LocalDate startDate =
                today.minusDays(
                        RECENT_CONTEXT_DAYS
                );

        LocalDate endDate =
                today.minusDays(1);

        if (endDate.isBefore(startDate)) {
            return List.of();
        }

        return writingHelpRecentDiaryRepository
                .findRecentPersonalizationDiaries(
                        userId,
                        startDate,
                        endDate,
                        PageRequest.of(
                                0,
                                3
                        )
                )
                .stream()
                .map(this::toRecentDiary)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<WritingHelpRecentDiary> toRecentDiary(
            Diary diary
    ) {
        String content =
                WritingHelpContentSanitizer
                        .sanitize(
                                diary.getContent()
                        );

        if (content.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(
                new WritingHelpRecentDiary(
                        diary.getRecordedDate(),
                        content
                )
        );
    }

    private List<String> findTodayQuestions(
            Long userId,
            LocalDate today
    ) {
        return aiQuestionRepository
                .findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
                        userId,
                        AiQuestionType.WRITING_HELP,
                        today
                )
                .stream()
                .map(AiQuestion::getQuestionText)
                .toList();
    }

    private List<String> findEarlierQuestions(
            Long userId,
            LocalDate today
    ) {
        return aiQuestionRepository
                .findTop12ByUserIdAndQuestionTypeAndAskedDateBeforeOrderByAskedDateDescQuestionOrderDesc(
                        userId,
                        AiQuestionType.WRITING_HELP,
                        today
                )
                .stream()
                .map(AiQuestion::getQuestionText)
                .toList();
    }

    private long countTodayQuestions(
            Long userId,
            LocalDate today
    ) {
        return aiQuestionRepository
                .countByUserIdAndQuestionTypeAndAskedDate(
                        userId,
                        AiQuestionType.WRITING_HELP,
                        today
                );
    }

    private record QuestionSelection(
            String questionText,
            QuestionGenerationSource generationSource,
            WritingHelpQuestionContextType contextType
    ) {
    }
}
