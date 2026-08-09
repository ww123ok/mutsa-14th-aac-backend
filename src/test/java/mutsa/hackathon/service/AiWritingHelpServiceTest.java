package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.dto.WritingHelpStatusResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private WritingHelpQuestionGenerator
            writingHelpQuestionGenerator;

    @InjectMocks
    private AiWritingHelpService
            aiWritingHelpService;

    @Test
    void 오늘_한번_사용했다면_남은_횟수는_두번이다() {
        when(
                appUserRepository
                        .existsById(1L)
        ).thenReturn(true);

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
                appUserRepository
                        .existsById(1L)
        ).thenReturn(true);

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
                appUserRepository
                        .existsById(1L)
        ).thenReturn(true);

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
                appUserRepository
                        .existsById(999L)
        ).thenReturn(false);

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
                                any(LocalDate.class)
                        )
        ).thenReturn(0L);

        when(
                appUserRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
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

        ArgumentCaptor<WritingHelpPrompt> captor =
                ArgumentCaptor.forClass(
                        WritingHelpPrompt.class
                );

        verify(
                writingHelpQuestionGenerator
        ).generate(
                captor.capture()
        );

        assertEquals(
                "하늘",
                captor.getValue()
                        .nickname()
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
}
