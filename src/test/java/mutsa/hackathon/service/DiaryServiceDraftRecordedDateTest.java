package mutsa.hackathon.service;

import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiaryServiceDraftRecordedDateTest {

    @Test
    void draftId로_완료하면_현재_DAYBIT날짜가_바뀌어도_draft의_기존날짜로_저장한다() {
        DiaryRepository diaryRepository = mock(DiaryRepository.class);
        DiaryRewardRepository diaryRewardRepository = mock(DiaryRewardRepository.class);
        AiQuestionRepository aiQuestionRepository = mock(AiQuestionRepository.class);
        AiMemoryProfileService aiMemoryProfileService = mock(AiMemoryProfileService.class);
        DiaryReflectionQuestionGenerator reflectionGenerator = mock(DiaryReflectionQuestionGenerator.class);
        DiaryCreatePersistenceService persistenceService = mock(DiaryCreatePersistenceService.class);
        UserDayService userDayService = mock(UserDayService.class);
        DiaryDraftService draftService = mock(DiaryDraftService.class);

        DiaryService service = new DiaryService(
                diaryRepository,
                diaryRewardRepository,
                aiQuestionRepository,
                aiMemoryProfileService,
                reflectionGenerator,
                persistenceService,
                userDayService,
                draftService
        );

        LocalDate sunday = LocalDate.of(2026, 8, 16);
        when(draftService.prepareForCompletion(1L, 10L))
                .thenReturn(sunday);
        when(reflectionGenerator.generate(any()))
                .thenReturn("성찰 질문");

        DiaryCreateRequest request = new DiaryCreateRequest(
                "월요일 01:10에 완료 버튼을 누름",
                true,
                10L
        );

        service.create(1L, request);

        verify(draftService).prepareForCompletion(1L, 10L);
        verify(persistenceService).validateCanCreate(1L, sunday);
        verify(persistenceService).persist(
                1L,
                request,
                sunday,
                "성찰 질문",
                mutsa.hackathon.domain.QuestionGenerationSource.AI
        );
    }
}
