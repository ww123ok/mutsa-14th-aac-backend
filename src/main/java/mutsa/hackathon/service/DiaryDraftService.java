package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.DiaryDraft;
import mutsa.hackathon.dto.DiaryDraftResponse;
import mutsa.hackathon.dto.DiaryDraftUpsertRequest;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryDraftRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DiaryDraftService {

    private final DiaryDraftRepository diaryDraftRepository;
    private final DiaryRepository diaryRepository;
    private final AppUserRepository appUserRepository;
    private final UserDayService userDayService;
    private final Clock serviceClock;

    @Value("${app.diary.draft-editing-lease-seconds:90}")
    private long editingLeaseSeconds;

    @Transactional
    public DiaryDraftResponse saveCurrentDraft(
            Long userId,
            DiaryDraftUpsertRequest request
    ) {
        LocalDateTime now = LocalDateTime.now(serviceClock);

        DiaryDraft draft;
        if (request.draftId() != null) {
            draft = findOwnedDraft(userId, request.draftId());
            validateDiaryNotWritten(userId, draft.getRecordedDate());
            draft.update(
                    request.content(),
                    request.shouldUseDiaryContentForPersonalization()
            );
        } else {
            LocalDate recordedDate = userDayService.currentDay(userId);
            validateDiaryNotWritten(userId, recordedDate);

            draft = diaryDraftRepository
                    .findByUserIdAndRecordedDate(userId, recordedDate)
                    .map(existing -> {
                        existing.update(
                                request.content(),
                                request.shouldUseDiaryContentForPersonalization()
                        );
                        return existing;
                    })
                    .orElseGet(() -> DiaryDraft.create(
                            findUser(userId),
                            recordedDate,
                            request.content(),
                            request.shouldUseDiaryContentForPersonalization()
                    ));
        }

        draft.markEditingActiveUntil(
                now.plusSeconds(validatedEditingLeaseSeconds())
        );

        return DiaryDraftResponse.from(
                diaryDraftRepository.save(draft)
        );
    }

    /**
     * 현재 DAYBIT 날짜를 넘겨 편집 중인 draft도 복구할 수 있도록
     * 사용자의 가장 최근 draft를 반환한다. 자동완료된 draft는 삭제되므로
     * 정상 상태에서는 현재 작성 중인 하나만 남는다.
     */
    @Transactional(readOnly = true)
    public DiaryDraftResponse getCurrentDraft(Long userId) {
        return diaryDraftRepository
                .findTopByUserIdOrderByRecordedDateDescIdDesc(userId)
                .map(DiaryDraftResponse::from)
                .orElse(null);
    }


    /**
     * 최종 작성 요청이 들어오면 draft가 처음 생성된 DAYBIT 날짜를
     * 완료 날짜로 고정한다. 동시에 짧은 lease를 연장하여
     * 성찰 질문 생성 중 자동완료 scheduler와 경쟁하는 가능성을 줄인다.
     */
    @Transactional
    public LocalDate prepareForCompletion(
            Long userId,
            Long draftId
    ) {
        DiaryDraft draft = findOwnedDraft(userId, draftId);
        validateDiaryNotWritten(userId, draft.getRecordedDate());
        draft.markEditingActiveUntil(
                LocalDateTime.now(serviceClock)
                        .plusSeconds(validatedEditingLeaseSeconds())
        );
        return draft.getRecordedDate();
    }

    @Transactional(readOnly = true)
    public boolean hasUnfinishedDraftInPeriod(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "draft 조회 기간은 필수입니다."
            );
        }
        return diaryDraftRepository
                .existsByUserIdAndRecordedDateBetween(
                        userId,
                        startDate,
                        endDate
                );
    }

    @Transactional
    public DiaryDraftResponse keepEditing(
            Long userId,
            Long draftId
    ) {
        DiaryDraft draft = findOwnedDraft(userId, draftId);
        draft.markEditingActiveUntil(
                LocalDateTime.now(serviceClock)
                        .plusSeconds(validatedEditingLeaseSeconds())
        );
        return DiaryDraftResponse.from(draft);
    }

    @Transactional
    public void stopEditing(
            Long userId,
            Long draftId
    ) {
        findOwnedDraft(userId, draftId).stopEditing();
    }

    @Transactional
    public void deleteCurrentDraft(Long userId) {
        LocalDate recordedDate = userDayService.currentDay(userId);
        diaryDraftRepository.deleteByUserIdAndRecordedDate(userId, recordedDate);
    }

    @Transactional
    public void deleteDraft(
            Long userId,
            Long draftId
    ) {
        diaryDraftRepository.delete(findOwnedDraft(userId, draftId));
    }

    private DiaryDraft findOwnedDraft(
            Long userId,
            Long draftId
    ) {
        return diaryDraftRepository
                .findByIdAndUserId(draftId, userId)
                .orElseThrow(() -> new ProjectException(
                        ErrorCode.DIARY_DRAFT_NOT_FOUND
                ));
    }

    private void validateDiaryNotWritten(
            Long userId,
            LocalDate recordedDate
    ) {
        if (diaryRepository.existsByUserIdAndRecordedDate(userId, recordedDate)) {
            throw new ProjectException(
                    ErrorCode.DIARY_ALREADY_WRITTEN_TODAY
            );
        }
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(
                        ErrorCode.USER_NOT_FOUND
                ));
    }

    private long validatedEditingLeaseSeconds() {
        if (editingLeaseSeconds <= 0) {
            throw new IllegalStateException(
                    "일기 작성 활성 lease는 1초 이상이어야 합니다."
            );
        }
        return editingLeaseSeconds;
    }
}
