package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.DiaryDraft;
import mutsa.hackathon.repository.DiaryDraftRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiaryAutoCompletionScheduler {

    private final DiaryDraftRepository
            diaryDraftRepository;

    private final DiaryAutoCompletionService
            autoCompletionService;

    /**
     * 사용자별 dayStartTime이 다르므로 활성 임시 저장본만 매분 확인.
     * 정확한 경계를 놓쳐도 recordedDate가 현재 논리 날짜보다 과거이면
     * 다음 실행에서 자동으로 catch-up.
     */
    @Scheduled(
            cron = "${app.diary.auto-completion-cron:0 * * * * *}",
            zone = "Asia/Seoul"
    )
    public void autoCompleteDueDrafts() {
        List<DiaryDraft> drafts =
                diaryDraftRepository
                        .findAllWithUser();

        for (DiaryDraft draft : drafts) {
            try {
                DiaryAutoCompletionService
                        .AutoCompletionResult result =
                        autoCompletionService
                                .autoCompleteIfDue(
                                        draft.getId()
                                );

                if (result.autoCompleted()) {
                    log.info(
                            "Diary draft auto-completed: draftId={}, diaryId={}, recordedDate={}",
                            draft.getId(),
                            result.diaryId(),
                            draft.getRecordedDate()
                    );
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "Diary draft auto-completion failed safely: draftId={}, reason={}",
                        draft.getId(),
                        exception.getClass()
                                .getSimpleName()
                );
            }
        }
    }
}
