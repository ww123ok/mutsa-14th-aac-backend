package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryAutoCompletionNotice;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryAutoCompletionNoticeRepository;
import mutsa.hackathon.repository.DiaryDraftRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryCreatePersistenceDraftFinalizationTest {

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
    private ApplicationEventPublisher
            eventPublisher;

    @Mock
    private DiaryDraftRepository
            diaryDraftRepository;

    @Mock
    private DiaryAutoCompletionNoticeRepository
            noticeRepository;

    private DiaryCreatePersistenceService
            persistenceService;

    private static final LocalDate RECORDED_DATE =
            LocalDate.of(
                    2026,
                    8,
                    18
            );

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse(
                        "2026-08-18T21:01:00Z"
                ),
                ZoneId.of(
                        "Asia/Seoul"
                )
        );

        persistenceService =
                new DiaryCreatePersistenceService(
                        diaryRepository,
                        diaryRewardRepository,
                        aiQuestionRepository,
                        appUserRepository,
                        eventPublisher,
                        diaryDraftRepository,
                        noticeRepository,
                        clock
                );

        AppUser user =
                AppUser.createKakaoUser(
                        "provider-1",
                        "사용자",
                        null,
                        null
                );
        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        when(
                appUserRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                1L,
                                RECORDED_DATE
                        )
        ).thenReturn(false);

        when(
                diaryRepository.saveAndFlush(
                        any(Diary.class)
                )
        ).thenAnswer(invocation -> {
            Diary diary = invocation.getArgument(0);
            ReflectionTestUtils.setField(
                    diary,
                    "id",
                    20L
            );
            return diary;
        });

        when(
                diaryRewardRepository.save(
                        any(DiaryReward.class)
                )
        ).thenAnswer(invocation -> {
            DiaryReward reward = invocation.getArgument(0);
            ReflectionTestUtils.setField(
                    reward,
                    "id",
                    30L
            );
            return reward;
        });

        when(
                aiQuestionRepository.save(
                        any(AiQuestion.class)
                )
        ).thenAnswer(invocation -> {
            AiQuestion question = invocation.getArgument(0);
            ReflectionTestUtils.setField(
                    question,
                    "id",
                    40L
            );
            return question;
        });
    }

    @Test
    void 수동완료도_같은날짜_임시저장본을_정리한다() {
        persistenceService.persist(
                1L,
                new DiaryCreateRequest(
                        "수동 완료",
                        true
                ),
                RECORDED_DATE,
                "성찰 질문",
                QuestionGenerationSource.FALLBACK
        );

        verify(
                diaryDraftRepository
        ).deleteByUserIdAndRecordedDate(
                1L,
                RECORDED_DATE
        );

        verify(
                noticeRepository,
                never()
        ).save(
                any(DiaryAutoCompletionNotice.class)
        );
    }

    @Test
    void 자동완료는_임시저장본_정리와_모달안내를_같은_저장경로에서_처리한다() {
        persistenceService.persistAutoCompleted(
                1L,
                new DiaryCreateRequest(
                        "자동 완료",
                        true
                ),
                RECORDED_DATE,
                "성찰 질문",
                QuestionGenerationSource.FALLBACK
        );

        verify(
                diaryDraftRepository
        ).deleteByUserIdAndRecordedDate(
                1L,
                RECORDED_DATE
        );

        verify(
                noticeRepository
        ).save(
                any(DiaryAutoCompletionNotice.class)
        );
    }
}
