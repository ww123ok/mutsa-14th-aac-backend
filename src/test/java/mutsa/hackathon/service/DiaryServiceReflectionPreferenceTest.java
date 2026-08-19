package mutsa.hackathon.service;

import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryServiceReflectionPreferenceTest {

    @Mock
    private DiaryRepository
            diaryRepository;

    @Mock
    private DiaryRewardRepository
            diaryRewardRepository;

    @Mock
    private AiMemoryProfileService
            aiMemoryProfileService;

    @Mock
    private DiaryReflectionQuestionGenerator
            diaryReflectionQuestionGenerator;

    @Mock
    private DiaryCreatePersistenceService
            diaryCreatePersistenceService;

    @Mock
    private UserDayService
            userDayService;

    @InjectMocks
    private DiaryService diaryService;

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
    void 개인화_반영을_거부해도_성찰질문은_오늘_일기내용을_사용한다() {

        DiaryCreateRequest request =
                new DiaryCreateRequest(
                        "오늘 팀원들과 프로젝트를 완성했다.",
                        false
                );

        when(
                diaryReflectionQuestionGenerator
                        .generate(
                                any(
                                        DiaryReflectionPrompt.class
                                )
                        )
        ).thenReturn(
                "오늘 기록에서 가장 의미 있었던 순간은 무엇인가요?"
        );

        diaryService.create(
                1L,
                request
        );

        ArgumentCaptor<DiaryReflectionPrompt>
                captor =
                ArgumentCaptor.forClass(
                        DiaryReflectionPrompt.class
                );

        verify(
                diaryReflectionQuestionGenerator
        ).generate(
                captor.capture()
        );

        assertEquals(
                "오늘 팀원들과 프로젝트를 완성했다.",
                captor
                        .getValue()
                        .diaryContent()
        );

        verify(
                diaryCreatePersistenceService
        ).persist(
                eq(1L),
                eq(request),
                any(),
                eq(
                        "오늘 기록에서 가장 의미 있었던 순간은 무엇인가요?"
                ),
                eq(
                        QuestionGenerationSource.AI
                )
        );
    }

    @Test
    void 성찰질문_AI가_실패하면_기본질문을_FALLBACK으로_저장한다() {

        DiaryCreateRequest request =
                new DiaryCreateRequest(
                        "오늘 하루를 기록했다.",
                        true
                );

        when(
                diaryReflectionQuestionGenerator
                        .generate(
                                any(
                                        DiaryReflectionPrompt.class
                                )
                        )
        ).thenThrow(
                new IllegalStateException(
                        "AI unavailable"
                )
        );

        diaryService.create(
                1L,
                request
        );

        verify(
                diaryCreatePersistenceService
        ).persist(
                eq(1L),
                eq(request),
                any(),
                eq(
                        "오늘의 기록에서 가장 오래 마음에 남은 순간은 무엇인가요?"
                ),
                eq(
                        QuestionGenerationSource.FALLBACK
                )
        );
    }

    @Test
    void 일기작성이_불가능하면_OpenAI를_호출하지_않는다() {

        DiaryCreateRequest request =
                new DiaryCreateRequest(
                        "오늘 하루를 기록했다.",
                        true
                );

        doThrow(
                new ProjectException(
                        ErrorCode
                                .DIARY_ALREADY_WRITTEN_TODAY
                )
        ).when(
                diaryCreatePersistenceService
        ).validateCanCreate(
                eq(1L),
                any()
        );

        try {
            diaryService.create(
                    1L,
                    request
            );
        } catch (ProjectException ignored) {
        }

        verify(
                diaryReflectionQuestionGenerator,
                never()
        ).generate(any());

        verify(
                diaryCreatePersistenceService,
                never()
        ).persist(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void 사용자_오늘_기준일을_일기_저장에_전달한다() {
        DiaryCreateRequest request =
                new DiaryCreateRequest(
                        "새벽에 작성한 일기",
                        true
                );

        when(
                diaryReflectionQuestionGenerator
                        .generate(
                                any(
                                        DiaryReflectionPrompt.class
                                )
                        )
        ).thenReturn(
                "새벽 기록에서 가장 기억나는 장면은 무엇인가요?"
        );

        diaryService.create(
                1L,
                request
        );

        verify(
                diaryCreatePersistenceService
        ).validateCanCreate(
                1L,
                USER_DAY
        );

        verify(
                diaryCreatePersistenceService
        ).persist(
                eq(1L),
                eq(request),
                eq(USER_DAY),
                eq(
                        "새벽 기록에서 가장 기억나는 장면은 무엇인가요?"
                ),
                eq(
                        QuestionGenerationSource.AI
                )
        );
    }

    @Test
    void 일기생성요청은_개인화반영여부를_명확히_판단한다() {

        DiaryCreateRequest useRequest =
                new DiaryCreateRequest(
                        "개인화 사용",
                        true
                );

        DiaryCreateRequest skipRequest =
                new DiaryCreateRequest(
                        "개인화 미사용",
                        false
                );

        assertTrue(
                useRequest
                        .shouldUseDiaryContentForPersonalization()
        );

        assertFalse(
                skipRequest
                        .shouldUseDiaryContentForPersonalization()
        );
    }
    @Test
    void 자동완료_일기는_자동완료_전용_저장경로를_사용한다() {
        DiaryCreateRequest request =
                new DiaryCreateRequest(
                        "하루 전환 전에 남긴 임시 저장",
                        true
                );

        when(
                diaryReflectionQuestionGenerator
                        .generate(
                                any(
                                        DiaryReflectionPrompt.class
                                )
                        )
        ).thenReturn(
                "기록에서 가장 기억에 남는 순간은 무엇인가요?"
        );

        diaryService.autoCompleteForRecordedDate(
                1L,
                request,
                USER_DAY
        );

        verify(
                diaryCreatePersistenceService
        ).persistAutoCompleted(
                eq(1L),
                eq(request),
                eq(USER_DAY),
                eq(
                        "기록에서 가장 기억에 남는 순간은 무엇인가요?"
                ),
                eq(
                        QuestionGenerationSource.AI
                )
        );

        verify(
                diaryCreatePersistenceService,
                never()
        ).persist(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

}