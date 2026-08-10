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
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private AiQuestionRepository
            aiQuestionRepository;

    @Mock
    private AppUserRepository
            appUserRepository;

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
    void 개인화_반영을_거부해도_성찰질문은_오늘_일기내용을_사용한다() {

        prepareSuccessfulCreate(
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
                "오늘 기록에서 가장 의미 있었던 순간은 무엇인가요?"
        );

        diaryService.create(
                1L,
                new DiaryCreateRequest(
                        "오늘 팀원들과 프로젝트를 완성했다.",
                        false
                )
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
    }

    @Test
    void 개인화_반영을_선택하고_전역동의도_있으면_기억추출_이벤트를_발행한다() {

        prepareSuccessfulCreate(
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
                "오늘 어떤 순간이 가장 오래 기억에 남았나요?"
        );

        diaryService.create(
                1L,
                new DiaryCreateRequest(
                        "오늘 반려묘와 시간을 보냈다.",
                        true
                )
        );

        verify(
                eventPublisher
        ).publishEvent(
                any(
                        DiaryMemoryExtractionRequested.class
                )
        );
    }

    @Test
    void 개인화_반영을_거부하면_기억추출_이벤트를_발행하지_않는다() {

        prepareSuccessfulCreate(
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
                "오늘 어떤 순간이 가장 오래 기억에 남았나요?"
        );

        diaryService.create(
                1L,
                new DiaryCreateRequest(
                        "오늘 반려묘와 시간을 보냈다.",
                        false
                )
        );

        verify(
                eventPublisher,
                never()
        ).publishEvent(
                any(
                        DiaryMemoryExtractionRequested.class
                )
        );
    }

    @Test
    void 전역_AI기억동의가_없으면_개인화선택이_true여도_기억추출하지_않는다() {

        prepareSuccessfulCreate(
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
                "오늘 어떤 순간이 가장 오래 기억에 남았나요?"
        );

        diaryService.create(
                1L,
                new DiaryCreateRequest(
                        "오늘 반려묘와 시간을 보냈다.",
                        true
                )
        );

        verify(
                eventPublisher,
                never()
        ).publishEvent(
                any(
                        DiaryMemoryExtractionRequested.class
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

    private AppUser prepareSuccessfulCreate(
            boolean aiMemoryConsent
    ) {
        AppUser user =
                AppUser.createKakaoUser(
                        "reflection-preference-user-"
                                + System.nanoTime(),
                        "데이빗",
                        null,
                        null
                );

        user.updatePersonalSettings(
                "데이빗",
                "대학생",
                LocalTime.of(
                        21,
                        0
                ),
                aiMemoryConsent
        );

        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                anyLong(),
                                any(LocalDate.class)
                        )
        ).thenReturn(false);

        when(
                appUserRepository
                        .findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                diaryRepository
                        .saveAndFlush(
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