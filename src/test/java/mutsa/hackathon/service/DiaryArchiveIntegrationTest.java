package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.DiaryDetailResponse;
import mutsa.hackathon.dto.DiaryResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DiaryArchiveIntegrationTest {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private AppUserRepository
            appUserRepository;

    @Autowired
    private DiaryRepository
            diaryRepository;

    @Autowired
    private DiaryRewardRepository
            diaryRewardRepository;

    @Autowired
    private AiQuestionRepository
            aiQuestionRepository;

    @Test
    void 월간_아카이브는_해당월의_일기와_색보상을_날짜순으로_한번에_반환한다() {
        AppUser user =
                saveUser("archive-month-user");

        AppUser otherUser =
                saveUser("archive-other-user");

        Diary firstDiary =
                saveDiary(
                        user,
                        LocalDate.of(
                                2026,
                                8,
                                3
                        ),
                        "8월 3일 일기"
                );

        Diary secondDiary =
                saveDiary(
                        user,
                        LocalDate.of(
                                2026,
                                8,
                                15
                        ),
                        "8월 15일 일기"
                );

        Diary thirdDiary =
                saveDiary(
                        user,
                        LocalDate.of(
                                2026,
                                8,
                                28
                        ),
                        "8월 28일 일기"
                );

        /*
         * 범위 밖 일기.
         * 8월 조회에 포함되면 안 됩니다.
         */
        saveDiary(
                user,
                LocalDate.of(
                        2026,
                        7,
                        31
                ),
                "7월 일기"
        );

        saveDiary(
                user,
                LocalDate.of(
                        2026,
                        9,
                        1
                ),
                "9월 일기"
        );

        /*
         * 다른 사용자의 같은 달 일기.
         * 현재 사용자의 Archive에 노출되면 안 됩니다.
         */
        saveDiary(
                otherUser,
                LocalDate.of(
                        2026,
                        8,
                        10
                ),
                "다른 사용자의 일기"
        );

        saveCompletedReward(
                firstDiary,
                "#D99A7A",
                List.of(
                        "아침",
                        "산책"
                )
        );

        /*
         * 비동기 보상이 아직 생성 중인 상황도
         * Archive에서 안전하게 표현되어야 합니다.
         */
        savePendingReward(
                secondDiary
        );

        saveCompletedReward(
                thirdDiary,
                "#73D8B4",
                List.of(
                        "집중",
                        "마무리",
                        "안도"
                )
        );

        List<DiaryResponse> responses =
                diaryService
                        .getMonthlyDiaries(
                                user.getId(),
                                2026,
                                8
                        );

        assertEquals(
                3,
                responses.size()
        );

        assertEquals(
                List.of(
                        LocalDate.of(
                                2026,
                                8,
                                3
                        ),
                        LocalDate.of(
                                2026,
                                8,
                                15
                        ),
                        LocalDate.of(
                                2026,
                                8,
                                28
                        )
                ),
                responses.stream()
                        .map(
                                DiaryResponse::recordedDate
                        )
                        .toList()
        );

        DiaryResponse first =
                responses.get(0);

        assertEquals(
                firstDiary.getId(),
                first.diaryId()
        );

        assertEquals(
                "8월 3일 일기",
                first.content()
        );

        assertNotNull(
                first.createdAt()
        );

        assertNotNull(
                first.reward()
        );

        assertEquals(
                "COMPLETED",
                first.reward()
                        .status()
        );

        assertEquals(
                "#D99A7A",
                first.reward()
                        .colorHex()
        );

        assertEquals(
                List.of(
                        "아침",
                        "산책"
                ),
                first.reward()
                        .keywords()
        );

        DiaryResponse second =
                responses.get(1);

        assertEquals(
                secondDiary.getId(),
                second.diaryId()
        );

        assertEquals(
                "PENDING",
                second.reward()
                        .status()
        );

        assertNull(
                second.reward()
                        .colorHex()
        );

        assertTrue(
                second.reward()
                        .keywords()
                        .isEmpty()
        );

        DiaryResponse third =
                responses.get(2);

        assertEquals(
                thirdDiary.getId(),
                third.diaryId()
        );

        assertEquals(
                "#73D8B4",
                third.reward()
                        .colorHex()
        );

        assertEquals(
                List.of(
                        "집중",
                        "마무리",
                        "안도"
                ),
                third.reward()
                        .keywords()
        );
    }

    @Test
    void 상세_아카이브는_일기_색보상_성찰질문을_함께_반환하고_미답변은_null이다() {
        AppUser user =
                saveUser(
                        "archive-detail-unanswered"
                );

        Diary diary =
                saveDiary(
                        user,
                        LocalDate.of(
                                2026,
                                8,
                                12
                        ),
                        "오늘은 오래 고민하던 문제를 정리했다."
                );

        saveCompletedReward(
                diary,
                "#C58A73",
                List.of(
                        "정리",
                        "고민",
                        "차분한"
                )
        );

        AiQuestion reflectionQuestion =
                saveReflectionQuestion(
                        user,
                        diary,
                        "문제를 정리하고 난 뒤 무엇이 가장 마음에 남았나요?"
                );

        DiaryDetailResponse response =
                diaryService.getDiary(
                        user.getId(),
                        diary.getId()
                );

        assertEquals(
                diary.getId(),
                response.diaryId()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        8,
                        12
                ),
                response.recordedDate()
        );

        assertEquals(
                "오늘은 오래 고민하던 문제를 정리했다.",
                response.content()
        );

        assertNotNull(
                response.createdAt()
        );

        assertNotNull(
                response.reward()
        );

        assertEquals(
                "COMPLETED",
                response.reward()
                        .status()
        );

        assertEquals(
                "#C58A73",
                response.reward()
                        .colorHex()
        );

        assertEquals(
                List.of(
                        "정리",
                        "고민",
                        "차분한"
                ),
                response.reward()
                        .keywords()
        );

        assertNotNull(
                response.reflection()
        );

        assertEquals(
                reflectionQuestion.getId(),
                response.reflection()
                        .questionId()
        );

        assertEquals(
                "문제를 정리하고 난 뒤 무엇이 가장 마음에 남았나요?",
                response.reflection()
                        .questionText()
        );

        assertEquals(
                "AI",
                response.reflection()
                        .generationSource()
        );

        /*
         * 성찰 답변은 선택사항이므로
         * 답하지 않은 일기도 완전한 Archive입니다.
         */
        assertNull(
                response.reflection()
                        .answerText()
        );

        assertNull(
                response.reflection()
                        .answeredAt()
        );
    }

    @Test
    void 성찰에_답한_일기_상세에서는_답변과_답변시각도_함께_반환한다() {
        AppUser user =
                saveUser(
                        "archive-detail-answered"
                );

        Diary diary =
                saveDiary(
                        user,
                        LocalDate.of(
                                2026,
                                8,
                                11
                        ),
                        "오늘 팀원들과 마지막 오류를 해결했다."
                );

        saveCompletedReward(
                diary,
                "#D6A45C",
                List.of(
                        "오류해결",
                        "테스트성공",
                        "연결"
                )
        );

        AiQuestion reflectionQuestion =
                AiQuestion.createReflection(
                        user,
                        diary,
                        "오늘 해결한 일 중 가장 기억에 남는 순간은 무엇인가요?",
                        diary.getRecordedDate(),
                        QuestionGenerationSource.AI
                );

        reflectionQuestion
                .submitReflectionAnswer(
                        "모든 테스트가 처음으로 통과한 순간이다."
                );

        aiQuestionRepository.saveAndFlush(
                reflectionQuestion
        );

        DiaryDetailResponse response =
                diaryService.getDiary(
                        user.getId(),
                        diary.getId()
                );

        assertNotNull(
                response.reflection()
        );

        assertEquals(
                "모든 테스트가 처음으로 통과한 순간이다.",
                response.reflection()
                        .answerText()
        );

        assertNotNull(
                response.reflection()
                        .answeredAt()
        );

        assertEquals(
                "오늘 해결한 일 중 가장 기억에 남는 순간은 무엇인가요?",
                response.reflection()
                        .questionText()
        );
    }

    @Test
    void 다른_사용자는_아카이브_상세에_접근할_수_없다() {
        AppUser owner =
                saveUser(
                        "archive-owner"
                );

        AppUser otherUser =
                saveUser(
                        "archive-intruder"
                );

        Diary diary =
                saveDiary(
                        owner,
                        LocalDate.of(
                                2026,
                                8,
                                10
                        ),
                        "소유자만 볼 수 있는 일기"
                );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                diaryService.getDiary(
                                        otherUser.getId(),
                                        diary.getId()
                                )
                );

        assertEquals(
                ErrorCode.DIARY_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    @Test
    void 삭제한_일기는_상세조회가_불가능하고_월간_아카이브에서도_사라진다() {
        AppUser user =
                saveUser(
                        "archive-delete-user"
                );

        Diary deletedDiary =
                saveDiary(
                        user,
                        LocalDate.of(
                                2026,
                                8,
                                5
                        ),
                        "삭제할 일기"
                );

        Diary remainedDiary =
                saveDiary(
                        user,
                        LocalDate.of(
                                2026,
                                8,
                                6
                        ),
                        "남아 있을 일기"
                );

        savePendingReward(
                deletedDiary
        );

        savePendingReward(
                remainedDiary
        );

        diaryService.deleteDiary(
                user.getId(),
                deletedDiary.getId()
        );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                diaryService.getDiary(
                                        user.getId(),
                                        deletedDiary.getId()
                                )
                );

        assertEquals(
                ErrorCode.DIARY_NOT_FOUND,
                exception.getErrorCode()
        );

        List<DiaryResponse> responses =
                diaryService
                        .getMonthlyDiaries(
                                user.getId(),
                                2026,
                                8
                        );

        assertEquals(
                1,
                responses.size()
        );

        assertEquals(
                remainedDiary.getId(),
                responses.get(0)
                        .diaryId()
        );

        assertFalse(
                responses.stream()
                        .anyMatch(response ->
                                response.diaryId()
                                        .equals(
                                                deletedDiary
                                                        .getId()
                                        )
                        )
        );
    }

    private AppUser saveUser(
            String providerIdPrefix
    ) {
        return appUserRepository
                .saveAndFlush(
                        AppUser.createKakaoUser(
                                providerIdPrefix
                                        + "-"
                                        + System.nanoTime(),
                                "테스트사용자",
                                null,
                                null
                        )
                );
    }

    private Diary saveDiary(
            AppUser user,
            LocalDate recordedDate,
            String content
    ) {
        return diaryRepository
                .saveAndFlush(
                        Diary.create(
                                user,
                                content,
                                recordedDate
                        )
                );
    }

    private DiaryReward savePendingReward(
            Diary diary
    ) {
        return diaryRewardRepository
                .saveAndFlush(
                        DiaryReward
                                .createPending(
                                        diary
                                )
                );
    }

    private DiaryReward saveCompletedReward(
            Diary diary,
            String colorHex,
            List<String> keywords
    ) {
        DiaryReward reward =
                DiaryReward.createPending(
                        diary
                );

        reward.complete(
                colorHex,
                keywords
        );

        return diaryRewardRepository
                .saveAndFlush(
                        reward
                );
    }

    private AiQuestion saveReflectionQuestion(
            AppUser user,
            Diary diary,
            String questionText
    ) {
        return aiQuestionRepository
                .saveAndFlush(
                        AiQuestion.createReflection(
                                user,
                                diary,
                                questionText,
                                diary.getRecordedDate(),
                                QuestionGenerationSource.AI
                        )
                );
    }
}