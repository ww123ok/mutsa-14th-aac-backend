package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.dto.DiaryDetailResponse;
import mutsa.hackathon.dto.DiaryHiddenResponse;
import mutsa.hackathon.dto.DiaryResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class DiaryVisibilityIntegrationTest {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private DiaryTrashService diaryTrashService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private DiaryRewardRepository diaryRewardRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 숨긴_일기는_월간_아카이브에서_제외되고_숨김_목록에서는_보상과_함께_조회된다() {
        AppUser user = saveUser("hidden-archive-user");

        Diary visibleDiary = saveDiary(
                user,
                LocalDate.of(2026, 8, 10),
                "보이는 일기"
        );

        Diary hiddenDiary = saveDiary(
                user,
                LocalDate.of(2026, 8, 11),
                "숨길 일기"
        );

        saveCompletedReward(
                hiddenDiary,
                "#8297C8",
                List.of("차분함", "밤")
        );

        diaryService.hideDiary(
                user.getId(),
                hiddenDiary.getId()
        );
        diaryRepository.flush();

        List<DiaryResponse> monthly =
                diaryService.getMonthlyDiaries(
                        user.getId(),
                        2026,
                        8
                );

        assertEquals(1, monthly.size());
        assertEquals(
                visibleDiary.getId(),
                monthly.get(0).diaryId()
        );

        List<DiaryHiddenResponse> hidden =
                diaryService.getHiddenDiaries(
                        user.getId()
                );

        assertEquals(1, hidden.size());
        assertEquals(
                hiddenDiary.getId(),
                hidden.get(0).diaryId()
        );
        assertEquals(
                "숨길 일기",
                hidden.get(0).content()
        );
        assertNotNull(hidden.get(0).hiddenAt());
        assertNotNull(hidden.get(0).reward());
        assertEquals(
                "#8297C8",
                hidden.get(0).reward().colorHex()
        );
    }

    @Test
    void 숨김은_일반_아카이브_가시성만_변경하며_본인_상세_조회는_유지된다() {
        AppUser user = saveUser("hidden-detail-user");
        Diary diary = saveDiary(
                user,
                LocalDate.of(2026, 8, 12),
                "상세 조회를 유지할 숨김 일기"
        );

        diaryService.hideDiary(
                user.getId(),
                diary.getId()
        );

        DiaryDetailResponse detail =
                diaryService.getDiary(
                        user.getId(),
                        diary.getId()
                );

        assertEquals(diary.getId(), detail.diaryId());
        assertEquals(
                "상세 조회를 유지할 숨김 일기",
                detail.content()
        );
    }

    @Test
    void 숨김_해제하면_데이터_손실_없이_월간_아카이브에_다시_나타난다() {
        AppUser user = saveUser("unhide-user");
        Diary diary = saveDiary(
                user,
                LocalDate.of(2026, 8, 13),
                "다시 나타날 일기"
        );

        diaryService.hideDiary(
                user.getId(),
                diary.getId()
        );

        diaryService.unhideDiary(
                user.getId(),
                diary.getId()
        );
        diaryRepository.flush();

        Diary reloaded = diaryRepository
                .findById(diary.getId())
                .orElseThrow();

        assertFalse(reloaded.isHidden());
        assertNull(reloaded.getHiddenAt());
        assertEquals(
                "다시 나타날 일기",
                reloaded.getContent()
        );

        List<DiaryResponse> monthly =
                diaryService.getMonthlyDiaries(
                        user.getId(),
                        2026,
                        8
                );

        assertEquals(1, monthly.size());
        assertEquals(
                diary.getId(),
                monthly.get(0).diaryId()
        );
    }

    @Test
    void 숨김_일기를_삭제하면_숨김을_해제하고_휴지통_상태로_단일화하며_복원하면_일반_상태가_된다() {
        AppUser user = saveUser("hidden-delete-user");
        Diary diary = saveDiary(
                user,
                LocalDate.of(2026, 8, 14),
                "숨김 후 삭제할 일기"
        );

        diaryService.hideDiary(
                user.getId(),
                diary.getId()
        );

        diaryService.deleteDiary(
                user.getId(),
                diary.getId()
        );
        diaryRepository.flush();

        Diary trashed = diaryRepository
                .findById(diary.getId())
                .orElseThrow();

        assertTrue(trashed.isDeleted());
        assertFalse(trashed.isHidden());
        assertNull(trashed.getHiddenAt());

        diaryTrashService.restore(
                user.getId(),
                diary.getId()
        );
        diaryRepository.flush();

        Diary restored = diaryRepository
                .findById(diary.getId())
                .orElseThrow();

        assertFalse(restored.isDeleted());
        assertFalse(restored.isHidden());
        assertNull(restored.getHiddenAt());
    }

    @Test
    void 다른_사용자의_일기는_숨기거나_숨김_해제할_수_없다() {
        AppUser owner = saveUser("hidden-owner");
        AppUser other = saveUser("hidden-other");
        Diary diary = saveDiary(
                owner,
                LocalDate.of(2026, 8, 15),
                "소유자 일기"
        );

        ProjectException hideException = assertThrows(
                ProjectException.class,
                () -> diaryService.hideDiary(
                        other.getId(),
                        diary.getId()
                )
        );

        assertEquals(
                ErrorCode.DIARY_NOT_FOUND,
                hideException.getErrorCode()
        );

        diaryService.hideDiary(
                owner.getId(),
                diary.getId()
        );

        ProjectException unhideException = assertThrows(
                ProjectException.class,
                () -> diaryService.unhideDiary(
                        other.getId(),
                        diary.getId()
                )
        );

        assertEquals(
                ErrorCode.DIARY_NOT_FOUND,
                unhideException.getErrorCode()
        );
    }

    @Test
    void 휴지통_일기는_숨김_상태로_변경할_수_없다() {
        AppUser user = saveUser("hidden-trash-user");
        Diary diary = saveDiary(
                user,
                LocalDate.of(2026, 8, 16),
                "삭제할 일기"
        );

        diaryService.deleteDiary(
                user.getId(),
                diary.getId()
        );

        ProjectException exception = assertThrows(
                ProjectException.class,
                () -> diaryService.hideDiary(
                        user.getId(),
                        diary.getId()
                )
        );

        assertEquals(
                ErrorCode.DIARY_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    @Test
    void 기존_DB에서_is_hidden이_null인_일기는_일반_아카이브에서_보이는_상태로_취급한다() {
        AppUser user = saveUser("legacy-hidden-null-user");
        Diary diary = saveDiary(
                user,
                LocalDate.of(2026, 8, 17),
                "기존 사용자 일기"
        );

        diaryRepository.flush();
        jdbcTemplate.update(
                "update diary set is_hidden = null where id = ?",
                diary.getId()
        );

        List<DiaryResponse> monthly =
                diaryService.getMonthlyDiaries(
                        user.getId(),
                        2026,
                        8
                );

        assertEquals(1, monthly.size());
        assertEquals(
                diary.getId(),
                monthly.get(0).diaryId()
        );
    }

    private AppUser saveUser(
            String providerIdPrefix
    ) {
        return appUserRepository.saveAndFlush(
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
        return diaryRepository.saveAndFlush(
                Diary.create(
                        user,
                        content,
                        recordedDate
                )
        );
    }

    private DiaryReward saveCompletedReward(
            Diary diary,
            String colorHex,
            List<String> keywords
    ) {
        DiaryReward reward =
                DiaryReward.createPending(diary);

        reward.complete(
                colorHex,
                keywords,
                "숨김 상태에서도 기존 색 보상은 유지됩니다."
        );

        return diaryRewardRepository.saveAndFlush(
                reward
        );
    }
}
