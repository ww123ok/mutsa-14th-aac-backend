package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DiaryServiceIntegrationTest {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void 오늘_일기를_작성하면_보상대기와_성찰질문을_반환한다() {
        AppUser user = saveUser();

        DiaryCreateResponse response = diaryService.create(
                user.getId(),
                new DiaryCreateRequest("오늘은 비가 왔다.")
        );

        assertNotNull(response.diaryId());
        assertNotNull(response.recordedDate());

        assertEquals(
                "PENDING",
                response.reward().status()
        );

        assertNull(response.reward().colorHex());

        assertNotNull(
                response.reflectionQuestion().questionId()
        );

        assertFalse(
                response.reflectionQuestion().answerRequired()
        );

        assertTrue(
                response.reflectionQuestion().generationSource()
                        .equals("AI")
                        || response.reflectionQuestion().generationSource()
                        .equals("FALLBACK")
        );
    }

    @Test
    void 같은_날_일기를_두_번_작성할_수_없다() {
        AppUser user = saveUser();

        diaryService.create(
                user.getId(),
                new DiaryCreateRequest("첫 번째 일기")
        );

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> diaryService.create(
                        user.getId(),
                        new DiaryCreateRequest("두 번째 일기")
                )
        );

        assertEquals(
                ErrorCode.DIARY_ALREADY_WRITTEN_TODAY,
                exception.getErrorCode()
        );
    }

    @Test
    void 일기를_삭제해도_같은_날_다시_작성할_수_없다() {
        AppUser user = saveUser();

        DiaryCreateResponse created = diaryService.create(
                user.getId(),
                new DiaryCreateRequest("삭제할 일기")
        );

        diaryService.deleteDiary(
                user.getId(),
                created.diaryId()
        );

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> diaryService.create(
                        user.getId(),
                        new DiaryCreateRequest("다시 쓰는 일기")
                )
        );

        assertEquals(
                ErrorCode.DIARY_ALREADY_WRITTEN_TODAY,
                exception.getErrorCode()
        );
    }

    private AppUser saveUser() {
        return appUserRepository.save(
                AppUser.createKakaoUser(
                        "test-kakao-id-" + System.nanoTime(),
                        "테스트 사용자",
                        null,
                        null
                )
        );
    }
}
