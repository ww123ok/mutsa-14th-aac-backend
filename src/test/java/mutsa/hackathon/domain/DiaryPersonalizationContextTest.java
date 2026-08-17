package mutsa.hackathon.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiaryPersonalizationContextTest {

    @Test
    void 신규일기는_작성당시_개인화선택을_그대로_저장한다() {
        AppUser user =
                AppUser.createKakaoUser(
                        "provider",
                        "데이빗",
                        null,
                        null
                );

        Diary allowed =
                Diary.create(
                        user,
                        "개인화에 사용할 일기",
                        LocalDate.of(2026, 8, 17),
                        true
                );

        Diary denied =
                Diary.create(
                        user,
                        "개인화에 사용하지 않을 일기",
                        LocalDate.of(2026, 8, 18),
                        false
                );

        assertTrue(
                allowed.canUseDiaryContentForPersonalization()
        );
        assertFalse(
                denied.canUseDiaryContentForPersonalization()
        );
    }

    @Test
    void 기존DB행은_명시적선택이_null이면_memoryAppliedAt으로_보수적으로_호환한다() {
        AppUser user =
                AppUser.createKakaoUser(
                        "legacy-provider",
                        "데이빗",
                        null,
                        null
                );

        Diary legacyDiary =
                Diary.create(
                        user,
                        "기존 일기",
                        LocalDate.of(2026, 8, 16),
                        true
                );

        ReflectionTestUtils.setField(
                legacyDiary,
                "personalizationUsesDiaryContent",
                null
        );

        assertFalse(
                legacyDiary.canUseDiaryContentForPersonalization()
        );

        legacyDiary.markMemoryApplied();

        assertTrue(
                legacyDiary.canUseDiaryContentForPersonalization()
        );
    }
}
