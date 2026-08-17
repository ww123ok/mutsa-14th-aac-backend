package mutsa.hackathon.repository;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class WritingHelpRecentDiaryRepositoryIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private WritingHelpRecentDiaryRepository
            writingHelpRecentDiaryRepository;

    @Test
    void 최근맥락은_삭제되지않고_개인화허용된_과거일기만_가져온다() {
        AppUser user =
                appUserRepository.save(
                        AppUser.createKakaoUser(
                                "writing-help-recent-user",
                                "데이빗",
                                null,
                                null
                        )
                );

        LocalDate today =
                LocalDate.of(2026, 8, 18);

        Diary allowed =
                diaryRepository.save(
                        Diary.create(
                                user,
                                "최근에 새로운 알바를 시작했다.",
                                today.minusDays(1),
                                true
                        )
                );

        diaryRepository.save(
                Diary.create(
                        user,
                        "개인화 사용을 허용하지 않은 최근 일기",
                        today.minusDays(2),
                        false
                )
        );

        Diary legacyAllowedCandidate =
                Diary.create(
                        user,
                        "기존 개인화 파이프라인을 통과한 일기",
                        today.minusDays(3),
                        true
                );

        ReflectionTestUtils.setField(
                legacyAllowedCandidate,
                "personalizationUsesDiaryContent",
                null
        );
        legacyAllowedCandidate.markMemoryApplied();

        /*
         * Diary는 현재 생성 시 version=0L을 가지므로 Spring Data JPA save()가
         * persist가 아니라 merge 경로를 사용할 수 있습니다. 이 경우 전달한 객체가
         * 아니라 save()가 반환한 managed instance에 생성 ID가 반영됩니다.
         * legacy 호환성 검증은 실제 저장된 엔티티 ID를 비교해야 하므로 반환값을 사용합니다.
         */
        Diary legacyAllowed =
                diaryRepository.save(
                        legacyAllowedCandidate
                );

        Diary deleted =
                Diary.create(
                        user,
                        "삭제된 최근 일기",
                        today.minusDays(4),
                        true
                );
        deleted.softDelete();
        diaryRepository.save(deleted);

        diaryRepository.save(
                Diary.create(
                        user,
                        "현재 DAYBIT 날짜의 일기",
                        today,
                        true
                )
        );

        List<Diary> result =
                writingHelpRecentDiaryRepository
                        .findRecentPersonalizationDiaries(
                                user.getId(),
                                today.minusDays(7),
                                today.minusDays(1),
                                PageRequest.of(0, 3)
                        );

        assertEquals(2, result.size());
        assertEquals(allowed.getId(), result.get(0).getId());
        assertTrue(
                result.stream()
                        .anyMatch(diary ->
                                diary.getId()
                                        .equals(
                                                legacyAllowed.getId()
                                        )
                        )
        );
    }
}
