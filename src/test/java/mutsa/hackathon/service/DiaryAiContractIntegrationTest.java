package mutsa.hackathon.service;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.dto.DiaryRewardResponse;
import mutsa.hackathon.dto.ReflectionAnswerRequest;
import mutsa.hackathon.dto.ReflectionAnswerResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class DiaryAiContractIntegrationTest {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private DiaryRewardService diaryRewardService;

    @Autowired
    private DiaryReflectionService diaryReflectionService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AiQuestionRepository aiQuestionRepository;

    @Test
    void 생성된_일기의_색상_보상은_PENDING으로_조회된다() {
        AppUser user = saveUser();

        DiaryCreateResponse created =
                diaryService.create(
                        user.getId(),
                        new DiaryCreateRequest(
                                "오늘은 차분하게 하루를 보냈다."
                        )
                );

        DiaryRewardResponse reward =
                diaryRewardService.getReward(
                        user.getId(),
                        created.diaryId()
                );

        assertEquals(
                created.diaryId(),
                reward.diaryId()
        );

        assertEquals(
                "PENDING",
                reward.status()
        );

        assertNull(
                reward.colorHex()
        );

        assertNull(
                reward.colorName()
        );
    }

    @Test
    void 성찰_답변을_최초_한_번_저장한다() {
        AppUser user = saveUser();

        DiaryCreateResponse created =
                diaryService.create(
                        user.getId(),
                        new DiaryCreateRequest(
                                "오늘 팀원들과 프로젝트를 진행했다."
                        )
                );

        ReflectionAnswerResponse response =
                diaryReflectionService.submitAnswer(
                        user.getId(),
                        created.diaryId(),
                        new ReflectionAnswerRequest(
                                "함께 문제를 해결한 순간이 가장 기억에 남는다."
                        )
                );

        assertEquals(
                created.diaryId(),
                response.diaryId()
        );

        assertEquals(
                created.reflectionQuestion()
                        .questionId(),
                response.questionId()
        );

        assertEquals(
                "함께 문제를 해결한 순간이 가장 기억에 남는다.",
                response.answerText()
        );

        assertNotNull(
                response.answeredAt()
        );

        AiQuestion savedQuestion =
                aiQuestionRepository
                        .findByDiaryIdAndQuestionType(
                                created.diaryId(),
                                AiQuestionType.REFLECTION
                        )
                        .orElseThrow();

        assertEquals(
                response.answerText(),
                savedQuestion.getAnswerText()
        );

        assertNotNull(
                savedQuestion.getAnsweredAt()
        );
    }

    @Test
    void 같은_성찰_질문에_답변을_두_번_저장할_수_없다() {
        AppUser user = saveUser();

        DiaryCreateResponse created =
                diaryService.create(
                        user.getId(),
                        new DiaryCreateRequest(
                                "오늘은 중요한 결정을 내렸다."
                        )
                );

        ReflectionAnswerRequest request =
                new ReflectionAnswerRequest(
                        "내가 중요하게 생각하는 기준을 확인했다."
                );

        diaryReflectionService.submitAnswer(
                user.getId(),
                created.diaryId(),
                request
        );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                diaryReflectionService
                                        .submitAnswer(
                                                user.getId(),
                                                created.diaryId(),
                                                request
                                        )
                );

        assertEquals(
                ErrorCode
                        .REFLECTION_ANSWER_ALREADY_SUBMITTED,
                exception.getErrorCode()
        );
    }

    @Test
    void 다른_사용자는_일기_보상을_조회할_수_없다() {
        AppUser owner = saveUser();
        AppUser otherUser = saveUser();

        DiaryCreateResponse created =
                diaryService.create(
                        owner.getId(),
                        new DiaryCreateRequest(
                                "오늘은 개인적인 기록을 남겼다."
                        )
                );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                diaryRewardService
                                        .getReward(
                                                otherUser.getId(),
                                                created.diaryId()
                                        )
                );

        assertEquals(
                ErrorCode.DIARY_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    @Test
    void 다른_사용자는_성찰_답변을_저장할_수_없다() {
        AppUser owner = saveUser();
        AppUser otherUser = saveUser();

        DiaryCreateResponse created =
                diaryService.create(
                        owner.getId(),
                        new DiaryCreateRequest(
                                "오늘은 나만의 생각을 정리했다."
                        )
                );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                diaryReflectionService
                                        .submitAnswer(
                                                otherUser.getId(),
                                                created.diaryId(),
                                                new ReflectionAnswerRequest(
                                                        "다른 사용자의 답변"
                                                )
                                        )
                );

        assertEquals(
                ErrorCode.DIARY_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    private AppUser saveUser() {
        return appUserRepository.save(
                AppUser.createKakaoUser(
                        "diary-ai-contract-"
                                + System.nanoTime(),
                        "테스트 사용자",
                        null,
                        null
                )
        );
    }
}