package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiaryMemoryDuplicateGuardTest {

    private final DiaryMemoryDuplicateGuard guard =
            new DiaryMemoryDuplicateGuard();

    @Test
    void 온보딩_직업이_대학생이면_대학생임이라는_WORK_STUDY_후보는_중복이다() {

        AppUser user = createUser(
                "대학생"
        );

        boolean duplicate =
                guard.isDuplicate(
                        user,
                        new DiaryMemoryCandidate(
                                UserMemoryCategory.WORK_STUDY,
                                "대학생임"
                        ),
                        List.of()
                );

        assertTrue(duplicate);
    }

    @Test
    void 기존_기억과_문장부호만_다른_후보는_중복이다() {

        AppUser user = createUser(
                "대학생"
        );

        UserMemoryItem existingMemory =
                mock(UserMemoryItem.class);

        when(
                existingMemory.getMemoryText()
        ).thenReturn(
                "러닝을 취미로 즐김"
        );

        boolean duplicate =
                guard.isDuplicate(
                        user,
                        new DiaryMemoryCandidate(
                                UserMemoryCategory.HOBBY,
                                "러닝을 취미로 즐김."
                        ),
                        List.of(existingMemory)
                );

        assertTrue(duplicate);
    }

    @Test
    void 다른_새로운_사실은_중복으로_판단하지_않는다() {

        AppUser user = createUser(
                "대학생"
        );

        UserMemoryItem existingMemory =
                mock(UserMemoryItem.class);

        when(
                existingMemory.getMemoryText()
        ).thenReturn(
                "러닝을 취미로 즐김"
        );

        boolean duplicate =
                guard.isDuplicate(
                        user,
                        new DiaryMemoryCandidate(
                                UserMemoryCategory.PET,
                                "반려묘와 함께 생활함"
                        ),
                        List.of(existingMemory)
                );

        assertFalse(duplicate);
    }

    @Test
    void 게임처럼_임으로_끝나는_일반단어를_잘못_잘라내지_않는다() {

        AppUser user = createUser(
                "대학생"
        );

        String firstKey =
                guard.createCandidateKey(
                        new DiaryMemoryCandidate(
                                UserMemoryCategory.INTEREST,
                                "게임"
                        )
                );

        String secondKey =
                guard.createCandidateKey(
                        new DiaryMemoryCandidate(
                                UserMemoryCategory.INTEREST,
                                "게"
                        )
                );

        assertFalse(
                firstKey.equals(secondKey)
        );
    }

    private AppUser createUser(
            String job
    ) {
        AppUser user =
                AppUser.createKakaoUser(
                        "duplicate-guard-user",
                        "데이빗",
                        null,
                        null
                );

        user.updatePersonalSettings(
                "데이빗",
                job,
                LocalTime.of(21, 0),
                true
        );

        return user;
    }
}