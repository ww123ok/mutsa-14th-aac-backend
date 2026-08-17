package mutsa.hackathon.repository;

import mutsa.hackathon.domain.Diary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 작성 도움 질문의 최근 맥락 조회만 담당하는 전용 Repository.
 * DiaryRepository의 월간 아카이브/주간 보상 조회 정책과 섞이지 않도록
 * 별도로 분리.
 */
public interface WritingHelpRecentDiaryRepository
        extends Repository<Diary, Long> {

    /**
     * 최근 맥락은 현재 DAYBIT 날짜를 제외한 과거 7일에서 조회합니다.
     * 실제 최대 개수는 Pageable로 제한.
     * 신규 일기는 personalizationUsesDiaryContent=true인 경우만 사용.
     * 기존 운영 일기는 해당 컬럼이 null일 수 있으므로, 과거 개인화 파이프라인이
     * 정상 완료되어 memoryAppliedAt이 존재하는 경우에만 legacy opt-in으로 인정.
     * hidden은 UI 가시성 상태이므로 최근 맥락에서 제외하지 않음.
     * deleted 일기만 제외.
     */
    @Query("""
            select diary
            from Diary diary
            where diary.user.id = :userId
              and diary.deleted = false
              and diary.recordedDate between :startDate and :endDate
              and (
                    diary.personalizationUsesDiaryContent = true
                    or (
                        diary.personalizationUsesDiaryContent is null
                        and diary.memoryAppliedAt is not null
                    )
                  )
            order by diary.recordedDate desc, diary.id desc
            """)
    List<Diary> findRecentPersonalizationDiaries(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
