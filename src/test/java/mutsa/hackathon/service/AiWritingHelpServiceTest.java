package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.dto.WritingHelpStatusResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
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
    private AppUserRepository
            appUserRepository;

    @Mock
    private AiQuestionRepository
            aiQuestionRepository;

    @Mock
    private DiaryRepository
            diaryRepository;

    @Mock
    private WritingHelpQuestionGenerator
            writingHelpQuestionGenerator;

    @Mock
    private UserDayService
            userDayService;

    @InjectMocks
    private AiWritingHelpService
            aiWritingHelpService;

    private static final LocalDate
            USER_DAY = LocalDate.of(
            2026,
            8,
            14
    );

    @BeforeEach
    void setUpUserDay() {
        lenient()
                .when(
                        userDayService
                                .currentDay(
                                        anyLong()
                                )
                )
                .thenReturn(
                        USER_DAY
                );
    }

    @Test
    void 오늘_한번_사용했다면_남은_횟수는_두번이다() {
        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                eq(1L),
                                eq(
                                        AiQuestionType
                                                .WRITING_HELP
                                ),
                                eq(USER_DAY)
                        )
        ).thenReturn(1L);

        WritingHelpStatusResponse response =
                aiWritingHelpService.getStatus(
                        1L
                );

        assertEquals(
                3,
                response.dailyLimit()
        );

        assertEquals(
                1,
                response.usedCount()
        );

        assertEquals(
                2,
                response.remainingCount()
        );

        assertTrue(
                response.available()
        );

        verify(
                writingHelpQuestionGenerator,
                never()
        ).generate(any());
    }

    @Test
    void 오늘_세번_사용했다면_더_이상_질문을_생성할_수_없다() {
        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                eq(1L),
                                eq(
                                        AiQuestionType
                                                .WRITING_HELP
                                ),
                                any(LocalDate.class)
                        )
        ).thenReturn(3L);

        WritingHelpStatusResponse response =
                aiWritingHelpService.getStatus(
                        1L
                );

        assertEquals(
                3,
                response.dailyLimit()
        );

        assertEquals(
                3,
                response.usedCount()
        );

        assertEquals(
                0,
                response.remainingCount()
        );

        assertFalse(
                response.available()
        );

        verify(
                writingHelpQuestionGenerator,
                never()
        ).generate(any());
    }

    @Test
    void 상태조회는_질문사용횟수를_소모하지_않는다() {
        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                eq(1L),
                                eq(
                                        AiQuestionType
                                                .WRITING_HELP
                                ),
                                any(LocalDate.class)
                        )
        ).thenReturn(0L);

        aiWritingHelpService.getStatus(
                1L
        );

        verify(
                aiQuestionRepository,
                never()
        ).save(any());

        verify(
                writingHelpQuestionGenerator,
                never()
        ).generate(any());
    }

    @Test
    void 존재하지_않는_사용자의_상태를_조회할_수_없다() {
        when(
                userDayService
                        .currentDay(999L)
        ).thenThrow(
                new ProjectException(
                        ErrorCode.USER_NOT_FOUND
                )
        );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                aiWritingHelpService
                                        .getStatus(999L)
                );

        assertEquals(
                ErrorCode.USER_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(
                aiQuestionRepository,
                never()
        ).countByUserIdAndQuestionTypeAndAskedDate(
                any(),
                any(),
                any()
        );
    }

    @Test
    void 오늘의_첫번째_작성도움_질문을_생성하고_저장한다() {
        AppUser user =
                AppUser.createKakaoUser(
                        "provider-id",
                        "하늘",
                        null,
                        null
                );

        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                eq(1L),
                                eq(
                                        AiQuestionType
                                                .WRITING_HELP
                                ),
                                eq(USER_DAY)
                        )
        ).thenReturn(0L);

        when(
                appUserRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                aiQuestionRepository
                        .findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
                                eq(1L),
                                eq(
                                        AiQuestionType
                                                .WRITING_HELP
                                ),
                                eq(USER_DAY)
                        )
        ).thenReturn(
                List.of()
        );

        when(
                writingHelpQuestionGenerator
                        .generate(
                                any(
                                        WritingHelpPrompt.class
                                )
                        )
        ).thenReturn(
                "오늘 가장 오래 기억에 남을 장면은 무엇인가요?"
        );

        when(
                aiQuestionRepository.save(
                        any(AiQuestion.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        WritingHelpQuestionResponse response =
                aiWritingHelpService
                        .generateQuestion(1L);

        assertEquals(
                1,
                response.questionOrder()
        );

        assertEquals(
                2,
                response.remainingCount()
        );

        assertEquals(
                "AI",
                response.generationSource()
        );

        ArgumentCaptor<AiQuestion>
                questionCaptor =
                ArgumentCaptor.forClass(
                        AiQuestion.class
                );

        verify(
                aiQuestionRepository
        ).save(
                questionCaptor.capture()
        );

        assertEquals(
                USER_DAY,
                questionCaptor
                        .getValue()
                        .getAskedDate()
        );

        ArgumentCaptor<WritingHelpPrompt> captor =
                ArgumentCaptor.forClass(
                        WritingHelpPrompt.class
                );

        verify(
                writingHelpQuestionGenerator
        ).generate(
                captor.capture()
        );

        WritingHelpPrompt prompt =
                captor.getValue();

        assertEquals(
                "하늘",
                prompt.nickname()
        );

        assertEquals(
                1,
                prompt.questionOrder()
        );

        assertTrue(
                prompt.previousQuestions()
                        .isEmpty()
        );
    }

    @Test
    void 두번째_작성도움_질문에는_첫번째_질문을_함께_전달한다() {
        AppUser user =
                AppUser.createKakaoUser(
                        "provider-id-2",
                        "민준",
                        null,
                        null
                );

        LocalDate today =
                LocalDate.now();

        AiQuestion firstQuestion =
                AiQuestion.createWritingHelp(
                        user,
                        "오늘 캠퍼스에서 가장 기억에 남는 순간은 언제였나요?",
                        1,
                        today,
                        QuestionGenerationSource.AI
                );

        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                eq(2L),
                                eq(
                                        AiQuestionType
                                                .WRITING_HELP
                                ),
                                any(LocalDate.class)
                        )
        ).thenReturn(1L);

        when(
                appUserRepository.findById(2L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                aiQuestionRepository
                        .findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
                                eq(2L),
                                eq(
                                        AiQuestionType
                                                .WRITING_HELP
                                ),
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of(firstQuestion)
        );

        when(
                writingHelpQuestionGenerator
                        .generate(
                                any(
                                        WritingHelpPrompt.class
                                )
                        )
        ).thenReturn(
                "오늘 누군가와 나눈 대화 중 다시 떠오르는 말이 있나요?"
        );

        when(
                aiQuestionRepository.save(
                        any(AiQuestion.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        WritingHelpQuestionResponse response =
                aiWritingHelpService
                        .generateQuestion(2L);

        assertEquals(
                2,
                response.questionOrder()
        );

        assertEquals(
                1,
                response.remainingCount()
        );

        ArgumentCaptor<WritingHelpPrompt> captor =
                ArgumentCaptor.forClass(
                        WritingHelpPrompt.class
                );

        verify(
                writingHelpQuestionGenerator
        ).generate(
                captor.capture()
        );

        WritingHelpPrompt prompt =
                captor.getValue();

        assertEquals(
                2,
                prompt.questionOrder()
        );

        assertEquals(
                List.of(
                        "오늘 캠퍼스에서 가장 기억에 남는 순간은 언제였나요?"
                ),
                prompt.previousQuestions()
        );
    }

    @Test
    void 일일제한에_도달하면_AI를_호출하지_않는다() {
        when(
                aiQuestionRepository
                        .countByUserIdAndQuestionTypeAndAskedDate(
                                eq(1L),
                                eq(
                                        AiQuestionType
                                                .WRITING_HELP
                                ),
                                any(LocalDate.class)
                        )
        ).thenReturn(3L);

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                aiWritingHelpService
                                        .generateQuestion(1L)
                );

        assertEquals(
                ErrorCode
                        .WRITING_HELP_LIMIT_EXCEEDED,
                exception.getErrorCode()
        );

        verify(
                writingHelpQuestionGenerator,
                never()
        ).generate(any());

        verify(
                appUserRepository,
                never()
        ).findById(any());
    }

    @Test
    void includesRecentDiaryAndQuestionHistoryInPersonalizationPrompt() {
        AppUser user = AppUser.createKakaoUser("provider-context", "하늘", null, null);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Diary recentDiary = Diary.create(
                user,
                "카페 알바에서 주문이 밀려 손님 응대가 부담스러웠다.",
                today.minusDays(1)
        );
        AiQuestion oldQuestion = AiQuestion.createWritingHelp(
                user,
                "최근 알바에서 기억에 남은 순간은 무엇인가요?",
                1,
                today.minusDays(10),
                QuestionGenerationSource.AI
        );

        when(aiQuestionRepository.countByUserIdAndQuestionTypeAndAskedDate(
                eq(3L), eq(AiQuestionType.WRITING_HELP), any(LocalDate.class))).thenReturn(0L);
        when(appUserRepository.findById(3L)).thenReturn(Optional.of(user));
        when(aiQuestionRepository.findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
                eq(3L), eq(AiQuestionType.WRITING_HELP), any(LocalDate.class))).thenReturn(List.of());
        when(aiQuestionRepository.findTop12ByUserIdAndQuestionTypeOrderByAskedDateDescQuestionOrderDesc(
                3L, AiQuestionType.WRITING_HELP)).thenReturn(List.of(oldQuestion));
        when(diaryRepository.findByUserIdAndRecordedDateBeforeAndDeletedFalseOrderByRecordedDateDescCreatedAtDesc(
                eq(3L), any(LocalDate.class), any())).thenReturn(List.of(recentDiary));
        when(writingHelpQuestionGenerator.generate(any(WritingHelpPrompt.class)))
                .thenReturn("알바에서 가장 오래 남은 순간은 무엇인가요?");
        when(aiQuestionRepository.save(any(AiQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        aiWritingHelpService.generateQuestion(3L);

        ArgumentCaptor<WritingHelpPrompt> captor = ArgumentCaptor.forClass(WritingHelpPrompt.class);
        verify(writingHelpQuestionGenerator).generate(captor.capture());
        assertEquals(List.of("최근 알바에서 기억에 남은 순간은 무엇인가요?"),
                captor.getValue().recentQuestionHistory());
        assertEquals(List.of(today.minusDays(1) + ": 카페 알바에서 주문이 밀려 손님 응대가 부담스러웠다."),
                captor.getValue().recentDiaryContexts());
    }
}
