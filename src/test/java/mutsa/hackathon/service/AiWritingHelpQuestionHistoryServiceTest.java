package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.WritingHelpQuestionHistoryResponse;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.WritingHelpRecentDiaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiWritingHelpQuestionHistoryServiceTest {

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

    @Test
    void 오늘_질문목록은_현재사용자와_DAYBIT날짜로만_조회한다() {
        Long userId = 101L;
        LocalDate userDay =
                LocalDate.of(2026, 8, 20);

        AiQuestion firstQuestion =
                mock(AiQuestion.class);
        AiQuestion secondQuestion =
                mock(AiQuestion.class);

        when(firstQuestion.getId())
                .thenReturn(11L);
        when(firstQuestion.getAskedDate())
                .thenReturn(userDay);
        when(firstQuestion.getQuestionOrder())
                .thenReturn(1);
        when(firstQuestion.getQuestionText())
                .thenReturn("오늘 가장 기억에 남는 순간은 언제였나요?");
        when(firstQuestion.getGenerationSource())
                .thenReturn(
                        QuestionGenerationSource.PREDEFINED
                );

        when(secondQuestion.getId())
                .thenReturn(12L);
        when(secondQuestion.getAskedDate())
                .thenReturn(userDay);
        when(secondQuestion.getQuestionOrder())
                .thenReturn(2);
        when(secondQuestion.getQuestionText())
                .thenReturn("그 순간에 가장 먼저 든 생각은 무엇이었나요?");
        when(secondQuestion.getGenerationSource())
                .thenReturn(
                        QuestionGenerationSource.AI
                );

        when(
                userDayService.currentDay(userId)
        ).thenReturn(userDay);

        when(
                aiQuestionRepository
                        .findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
                                userId,
                                AiQuestionType.WRITING_HELP,
                                userDay
                        )
        ).thenReturn(
                List.of(
                        firstQuestion,
                        secondQuestion
                )
        );

        List<WritingHelpQuestionHistoryResponse> result =
                aiWritingHelpService
                        .getTodayQuestionHistory(
                                userId
                        );

        assertEquals(2, result.size());
        assertEquals(11L, result.get(0).questionId());
        assertEquals(1, result.get(0).questionOrder());
        assertEquals(
                "오늘 가장 기억에 남는 순간은 언제였나요?",
                result.get(0).questionText()
        );
        assertEquals(
                "PREDEFINED",
                result.get(0).generationSource()
        );
        assertEquals(12L, result.get(1).questionId());
        assertEquals(2, result.get(1).questionOrder());
        assertEquals("AI", result.get(1).generationSource());

        verify(
                aiQuestionRepository
        ).findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
                userId,
                AiQuestionType.WRITING_HELP,
                userDay
        );
    }
}
