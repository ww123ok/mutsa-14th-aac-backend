package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private WritingHelpQuestionGenerator writingHelpQuestionGenerator;

    @InjectMocks
    private AiWritingHelpService aiWritingHelpService;

    @Test
    void generatesAndSavesTheFirstQuestionOfTheDay() {
        AppUser user = AppUser.createKakaoUser("provider-id", "하늘", null, null);
        when(aiQuestionRepository.countByUserIdAndQuestionTypeAndAskedDate(
                eq(1L),
                eq(AiQuestionType.WRITING_HELP),
                any(LocalDate.class)
        )).thenReturn(0L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(writingHelpQuestionGenerator.generate(any(WritingHelpPrompt.class)))
                .thenReturn("오늘 가장 오래 기억에 남을 장면은 무엇인가요?");
        when(aiQuestionRepository.save(any(AiQuestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WritingHelpQuestionResponse response = aiWritingHelpService.generateQuestion(1L);

        assertEquals(1, response.questionOrder());
        assertEquals(2, response.remainingCount());
        assertEquals("AI", response.generationSource());

        ArgumentCaptor<WritingHelpPrompt> promptCaptor = ArgumentCaptor.forClass(
                WritingHelpPrompt.class
        );
        verify(writingHelpQuestionGenerator).generate(promptCaptor.capture());
        assertEquals("하늘", promptCaptor.getValue().nickname());
    }

    @Test
    void doesNotCallAiAfterDailyLimitIsReached() {
        when(aiQuestionRepository.countByUserIdAndQuestionTypeAndAskedDate(
                eq(1L),
                eq(AiQuestionType.WRITING_HELP),
                any(LocalDate.class)
        )).thenReturn(3L);

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> aiWritingHelpService.generateQuestion(1L)
        );

        assertEquals(ErrorCode.WRITING_HELP_LIMIT_EXCEEDED, exception.getErrorCode());
        verify(writingHelpQuestionGenerator, never()).generate(any());
        verify(appUserRepository, never()).findById(any());
    }
}
