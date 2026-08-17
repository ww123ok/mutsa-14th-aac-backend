package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.WritingHelpQuestionRequest;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.dto.WritingHelpStatusResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.WritingHelpRecentDiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiWritingHelpServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AiQuestionRepository aiQuestionRepository;

    @Mock
    private WritingHelpRecentDiaryRepository
            writingHelpRecentDiaryRepository;

    @Mock
    private WritingHelpQuestionGenerator
            writingHelpQuestionGenerator;

    @Mock
    private WritingHelpGenericQuestionProvider
            writingHelpGenericQuestionProvider;

    @Mock
    private UserDayService userDayService;

    @InjectMocks
    private AiWritingHelpService aiWritingHelpService;

    private static final LocalDate USER_DAY =
            LocalDate.of(2026, 8, 18);

    @BeforeEach
    void setUpUserDay() {
        lenient()
                .when(
                        userDayService
                                .currentDay(anyLong())
                )
                .thenReturn(USER_DAY);
    }

    @Test
    void 오늘_한번_사용했다면_남은_횟수는_두번이다() {
        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                1L,
                                AiQuestionType.WRITING_HELP,
                                USER_DAY
                        )
        ).thenReturn(1L);

        WritingHelpStatusResponse response =
                aiWritingHelpService.getStatus(1L);

        assertEquals(3, response.dailyLimit());
        assertEquals(1, response.usedCount());
        assertEquals(2, response.remainingCount());
        assertTrue(response.available());
    }

    @Test
    void 오늘_세번_사용했다면_더_이상_질문을_생성할_수_없다() {
        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                1L,
                                AiQuestionType.WRITING_HELP,
                                USER_DAY
                        )
        ).thenReturn(3L);

        WritingHelpStatusResponse response =
                aiWritingHelpService.getStatus(1L);

        assertEquals(0, response.remainingCount());
        assertFalse(response.available());
    }

    @Test
    void 작성중인_내용이_있으면_프로필이나_최근일기보다_현재본문을_우선한다() {
        AppUser user = createUser(false);
        prepareGeneration(1L, user, 0L, List.of(), List.of());

        when(
                writingHelpQuestionGenerator
                        .generate(any(WritingHelpPrompt.class))
        ).thenReturn(
                "카페에서 가장 눈에 들어온 분위기나 인테리어는 어땠나요?"
        );

        WritingHelpQuestionResponse response =
                aiWritingHelpService.generateQuestion(
                        1L,
                        new WritingHelpQuestionRequest(
                                "카페에 갔다."
                        )
                );

        ArgumentCaptor<WritingHelpPrompt> promptCaptor =
                ArgumentCaptor.forClass(
                        WritingHelpPrompt.class
                );

        verify(
                writingHelpQuestionGenerator
        ).generate(promptCaptor.capture());

        WritingHelpPrompt prompt =
                promptCaptor.getValue();

        assertEquals(
                WritingHelpQuestionContextType.CURRENT_DRAFT,
                prompt.contextType()
        );
        assertEquals(
                "카페에 갔다.",
                prompt.currentContent()
        );
        assertTrue(prompt.recentDiaries().isEmpty());
        assertEquals("CURRENT_DRAFT", response.contextType());
        assertEquals("AI", response.generationSource());

        verify(
                writingHelpRecentDiaryRepository,
                never()
        ).findRecentPersonalizationDiaries(
                any(), any(), any(), any()
        );

        verify(
                writingHelpGenericQuestionProvider,
                never()
        ).nextQuestion(any(), any());
    }

    @Test
    void 현재본문이_없고_동의한_최근일기가_있으면_최근맥락_질문을_생성한다() {
        AppUser user = createUser(true);
        prepareGeneration(2L, user, 0L, List.of(), List.of());

        Diary recentDiary =
                Diary.create(
                        user,
                        "최근에 카페 아르바이트를 시작했다. 아직 주문 받는 게 낯설다.",
                        USER_DAY.minusDays(2)
                );
        recentDiary.markMemoryApplied();

        when(
                writingHelpRecentDiaryRepository
                        .findRecentPersonalizationDiaries(
                                eq(2L),
                                eq(USER_DAY.minusDays(7)),
                                eq(USER_DAY.minusDays(1)),
                                any()
                        )
        ).thenReturn(List.of(recentDiary));

        when(
                writingHelpQuestionGenerator
                        .generate(any(WritingHelpPrompt.class))
        ).thenReturn(
                "최근 시작한 카페 알바에는 조금씩 적응하고 있나요?"
        );

        WritingHelpQuestionResponse response =
                aiWritingHelpService.generateQuestion(
                        2L,
                        new WritingHelpQuestionRequest("   ")
                );

        ArgumentCaptor<WritingHelpPrompt> promptCaptor =
                ArgumentCaptor.forClass(
                        WritingHelpPrompt.class
                );

        verify(
                writingHelpQuestionGenerator
        ).generate(promptCaptor.capture());

        WritingHelpPrompt prompt =
                promptCaptor.getValue();

        assertEquals(
                WritingHelpQuestionContextType.RECENT_CONTEXT,
                prompt.contextType()
        );
        assertEquals(1, prompt.recentDiaries().size());
        assertEquals(
                USER_DAY.minusDays(2),
                prompt.recentDiaries()
                        .get(0)
                        .recordedDate()
        );
        assertEquals("RECENT_CONTEXT", response.contextType());
        assertEquals("AI", response.generationSource());
    }

    @Test
    void 작성전_세번째_최근맥락은_두번째_최근일기를_우선한다() {
        AppUser user = createUser(true);

        AiQuestion firstQuestion =
                AiQuestion.createWritingHelp(
                        user,
                        "최근 시작한 카페 알바에는 조금씩 적응하고 있나요?",
                        1,
                        USER_DAY,
                        QuestionGenerationSource.AI
                );

        AiQuestion secondQuestion =
                AiQuestion.createWritingHelp(
                        user,
                        "오늘 나도 모르게 웃었던 순간이 있었나요?",
                        2,
                        USER_DAY,
                        QuestionGenerationSource.PREDEFINED
                );

        prepareGeneration(
                22L,
                user,
                2L,
                List.of(
                        firstQuestion,
                        secondQuestion
                ),
                List.of()
        );

        Diary latestDiary =
                Diary.create(
                        user,
                        "최근에 카페 아르바이트를 시작했다.",
                        USER_DAY.minusDays(1)
                );
        latestDiary.markMemoryApplied();

        Diary secondLatestDiary =
                Diary.create(
                        user,
                        "팀 프로젝트 첫 회의를 하고 역할을 나눴다.",
                        USER_DAY.minusDays(2)
                );
        secondLatestDiary.markMemoryApplied();

        Diary thirdDiary =
                Diary.create(
                        user,
                        "새로운 운동 루틴을 시작해 보기로 했다.",
                        USER_DAY.minusDays(3)
                );
        thirdDiary.markMemoryApplied();

        when(
                writingHelpRecentDiaryRepository
                        .findRecentPersonalizationDiaries(
                                eq(22L),
                                eq(USER_DAY.minusDays(7)),
                                eq(USER_DAY.minusDays(1)),
                                any()
                        )
        ).thenReturn(
                List.of(
                        latestDiary,
                        secondLatestDiary,
                        thirdDiary
                )
        );

        when(
                writingHelpQuestionGenerator
                        .generate(any(WritingHelpPrompt.class))
        ).thenReturn(
                "그 뒤로 팀 프로젝트 역할 분담은 어떻게 진행되고 있나요?"
        );

        WritingHelpQuestionResponse response =
                aiWritingHelpService.generateQuestion(
                        22L,
                        null
                );

        ArgumentCaptor<WritingHelpPrompt> promptCaptor =
                ArgumentCaptor.forClass(
                        WritingHelpPrompt.class
                );

        verify(
                writingHelpQuestionGenerator
        ).generate(promptCaptor.capture());

        WritingHelpPrompt prompt =
                promptCaptor.getValue();

        assertEquals(
                WritingHelpQuestionContextType.RECENT_CONTEXT,
                prompt.contextType()
        );
        assertEquals(
                USER_DAY.minusDays(2),
                prompt.recentDiaries()
                        .get(0)
                        .recordedDate()
        );
        assertEquals(
                USER_DAY.minusDays(1),
                prompt.recentDiaries()
                        .get(2)
                        .recordedDate()
        );
        assertEquals(
                List.of(
                        firstQuestion.getQuestionText(),
                        secondQuestion.getQuestionText()
                ),
                prompt.previousQuestions()
        );
        assertEquals(
                "RECENT_CONTEXT",
                response.contextType()
        );
    }

    @Test
    void 전역동의가_꺼져있으면_과거일기를_읽지않고_범용질문을_준다() {
        AppUser user = createUser(false);
        prepareGeneration(3L, user, 0L, List.of(), List.of());

        when(
                writingHelpGenericQuestionProvider
                        .nextQuestion(any(), any())
        ).thenReturn(
                "오늘 가장 기억에 남는 순간은 언제였나요?"
        );

        WritingHelpQuestionResponse response =
                aiWritingHelpService.generateQuestion(
                        3L,
                        null
                );

        assertEquals("GENERIC", response.contextType());
        assertEquals("PREDEFINED", response.generationSource());
        assertEquals(
                "오늘 가장 기억에 남는 순간은 언제였나요?",
                response.questionText()
        );

        verify(
                writingHelpRecentDiaryRepository,
                never()
        ).findRecentPersonalizationDiaries(
                any(), any(), any(), any()
        );

        verify(
                writingHelpQuestionGenerator,
                never()
        ).generate(any());
    }

    @Test
    void 작성전_두번째질문은_최근맥락_조회없이_범용질문을_사용한다() {
        AppUser user = createUser(true);

        AiQuestion firstQuestion =
                AiQuestion.createWritingHelp(
                        user,
                        "최근 시작한 카페 알바에는 조금씩 적응하고 있나요?",
                        1,
                        USER_DAY,
                        QuestionGenerationSource.AI
                );

        AiQuestion earlierQuestion =
                AiQuestion.createWritingHelp(
                        user,
                        "오늘 가장 기억에 남는 순간은 언제였나요?",
                        1,
                        USER_DAY.minusDays(1),
                        QuestionGenerationSource.PREDEFINED
                );

        prepareGeneration(
                4L,
                user,
                1L,
                List.of(firstQuestion),
                List.of(earlierQuestion)
        );

        when(
                writingHelpGenericQuestionProvider
                        .nextQuestion(any(), any())
        ).thenReturn(
                "오늘 예상하지 못했던 일이 있었나요?"
        );

        WritingHelpQuestionResponse response =
                aiWritingHelpService.generateQuestion(
                        4L,
                        null
                );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> excludedCaptor =
                ArgumentCaptor.forClass(List.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> todayCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(
                writingHelpGenericQuestionProvider
        ).nextQuestion(
                excludedCaptor.capture(),
                todayCaptor.capture()
        );

        assertEquals(
                "GENERIC",
                response.contextType()
        );
        assertEquals(
                "PREDEFINED",
                response.generationSource()
        );
        assertTrue(
                excludedCaptor.getValue()
                        .contains(firstQuestion.getQuestionText())
        );
        assertTrue(
                excludedCaptor.getValue()
                        .contains(earlierQuestion.getQuestionText())
        );
        assertEquals(
                List.of(firstQuestion.getQuestionText()),
                todayCaptor.getValue()
        );

        verify(
                writingHelpRecentDiaryRepository,
                never()
        ).findRecentPersonalizationDiaries(
                any(), any(), any(), any()
        );

        verify(
                writingHelpQuestionGenerator,
                never()
        ).generate(any());
    }

    @Test
    void 두번째_현재본문_질문에는_오늘_첫번째_질문을_다양성_문맥으로_전달한다() {
        AppUser user = createUser(false);

        AiQuestion firstQuestion =
                AiQuestion.createWritingHelp(
                        user,
                        "카페에서 가장 눈에 들어온 인테리어는 어땠나요?",
                        1,
                        USER_DAY,
                        QuestionGenerationSource.AI
                );

        prepareGeneration(
                5L,
                user,
                1L,
                List.of(firstQuestion),
                List.of()
        );

        when(
                writingHelpQuestionGenerator
                        .generate(any(WritingHelpPrompt.class))
        ).thenReturn(
                "카페에서 먹거나 마신 게 있었다면 어떤 맛이었나요?"
        );

        aiWritingHelpService.generateQuestion(
                5L,
                new WritingHelpQuestionRequest(
                        "친구와 카페에 갔다. 창가 자리에 앉았다."
                )
        );

        ArgumentCaptor<WritingHelpPrompt> promptCaptor =
                ArgumentCaptor.forClass(
                        WritingHelpPrompt.class
                );

        verify(
                writingHelpQuestionGenerator
        ).generate(promptCaptor.capture());

        assertEquals(
                List.of(firstQuestion.getQuestionText()),
                promptCaptor.getValue()
                        .previousQuestions()
        );
        assertEquals(
                2,
                promptCaptor.getValue()
                        .questionOrder()
        );
    }

    @Test
    void 일일제한에_도달하면_AI와_범용질문을_모두_호출하지_않는다() {
        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                1L,
                                AiQuestionType.WRITING_HELP,
                                USER_DAY
                        )
        ).thenReturn(3L);

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                aiWritingHelpService
                                        .generateQuestion(
                                                1L,
                                                new WritingHelpQuestionRequest(
                                                        "카페에 갔다."
                                                )
                                        )
                );

        assertEquals(
                ErrorCode.WRITING_HELP_LIMIT_EXCEEDED,
                exception.getErrorCode()
        );

        verify(
                writingHelpQuestionGenerator,
                never()
        ).generate(any());

        verify(
                writingHelpGenericQuestionProvider,
                never()
        ).nextQuestion(any(), any());
    }

    private AppUser createUser(
            boolean aiMemoryConsent
    ) {
        AppUser user =
                AppUser.createKakaoUser(
                        "provider-id",
                        "하늘",
                        null,
                        null
                );

        user.updatePersonalSettings(
                "하늘",
                "대학생",
                LocalTime.of(21, 0),
                aiMemoryConsent
        );

        return user;
    }

    private void prepareGeneration(
            Long userId,
            AppUser user,
            long usedCount,
            List<AiQuestion> todayQuestions,
            List<AiQuestion> earlierQuestions
    ) {
        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                userId,
                                AiQuestionType.WRITING_HELP,
                                USER_DAY
                        )
        ).thenReturn(usedCount);

        when(
                appUserRepository.findById(userId)
        ).thenReturn(Optional.of(user));

        when(
                aiQuestionRepository
                        .findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
                                userId,
                                AiQuestionType.WRITING_HELP,
                                USER_DAY
                        )
        ).thenReturn(todayQuestions);

        lenient()
                .when(
                        aiQuestionRepository
                                .findTop12ByUserIdAndQuestionTypeAndAskedDateBeforeOrderByAskedDateDescQuestionOrderDesc(
                                        userId,
                                        AiQuestionType.WRITING_HELP,
                                        USER_DAY
                                )
                )
                .thenReturn(earlierQuestions);

        when(
                aiQuestionRepository.save(any(AiQuestion.class))
        ).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
