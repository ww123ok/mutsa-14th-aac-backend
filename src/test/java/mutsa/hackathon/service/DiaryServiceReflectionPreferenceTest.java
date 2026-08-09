package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryServiceReflectionPreferenceTest {

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DiaryRewardRepository
            diaryRewardRepository;

    @Mock
    private AiQuestionRepository
            aiQuestionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AiMemoryProfileService
            aiMemoryProfileService;

    @Mock
    private DiaryReflectionQuestionGenerator
            diaryReflectionQuestionGenerator;

    @Mock
    private ApplicationEventPublisher
            eventPublisher;

    @InjectMocks
    private DiaryService diaryService;

    @Test
    void 일기_반영을_선택하면_성찰질문_생성기에_일기내용을_전달한다() {
        AppUser user = prepareSuccessfulCreate();

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
                new DiaryCreateRequest(
                        "오늘 팀원들과 프로젝트를 완성했다.",
                        true
                )
        );

        ArgumentCaptor<DiaryReflectionPrompt> captor =
                ArgumentCaptor.forClass(
                        DiaryReflectionPrompt.class
                );

        verify(
                diaryReflectionQuestionGenerator
        ).generate(
                captor.capture()
        );

        DiaryReflectionPrompt prompt =
                captor.getValue();

        assertTrue(
                prompt.reflectionUsesDiaryContent()
        );

        assertEquals(
                "오늘 팀원들과 프로젝트를 완성했다.",
                prompt.diaryContent()
        );

        assertEquals(
                user.getNickname(),
                prompt.nickname()
        );
    }

    @Test
    void 일기_반영을_거부하면_성찰질문_생성기에_일기내용을_전달하지_않는다() {
        prepareSuccessfulCreate();

        when(
                diaryReflectionQuestionGenerator
                        .generate(
                                any(
                                        DiaryReflectionPrompt.class
                                )
                        )
        ).thenReturn(
                "오늘 하루를 돌아보면 어떤 순간이 가장 기억에 남나요?"
        );

        diaryService.create(
                1L,
                new DiaryCreateRequest(
                        "외부 AI에 전달하고 싶지 않은 일기 내용",
                        false
                )
        );

        ArgumentCaptor<DiaryReflectionPrompt> captor =
                ArgumentCaptor.forClass(
                        DiaryReflectionPrompt.class
                );

        verify(
                diaryReflectionQuestionGenerator
        ).generate(
                captor.capture()
        );

        DiaryReflectionPrompt prompt =
                captor.getValue();

        assertFalse(
                prompt.reflectionUsesDiaryContent()
        );

        assertNull(
                prompt.diaryContent()
        );
    }

    @Test
    void 기존_한개_인자_생성자는_일기내용_반영으로_처리한다() {
        DiaryCreateRequest request =
                new DiaryCreateRequest(
                        "기존 테스트 호환용 일기"
                );

        assertTrue(
                request.shouldUseDiaryContent()
        );
    }

    private AppUser prepareSuccessfulCreate() {
        AppUser user =
                AppUser.createKakaoUser(
                        "reflection-preference-user",
                        "데이빗",
                        null,
                        null
                );

        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                anyLong(),
                                any(LocalDate.class)
                        )
        ).thenReturn(false);

        when(
                appUserRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                diaryRepository.saveAndFlush(
                        any(Diary.class)
                )
        ).thenAnswer(invocation -> {
            Diary diary =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    diary,
                    "id",
                    100L
            );

            return diary;
        });

        when(
                diaryRewardRepository.save(
                        any(DiaryReward.class)
                )
        ).thenAnswer(invocation -> {
            DiaryReward reward =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    reward,
                    "id",
                    200L
            );

            return reward;
        });

        when(
                aiQuestionRepository.save(
                        any(AiQuestion.class)
                )
        ).thenAnswer(invocation -> {
            AiQuestion question =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    question,
                    "id",
                    300L
            );

            return question;
        });

        return user;
    }
}