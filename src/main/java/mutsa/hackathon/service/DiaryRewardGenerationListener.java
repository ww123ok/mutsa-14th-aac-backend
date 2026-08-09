package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event
        .TransactionPhase;
import org.springframework.transaction.event
        .TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DiaryRewardGenerationListener {

    private final DiaryRewardGenerationService
            diaryRewardGenerationService;

    /**
     * 일기와 PENDING 보상이 DB에 정상 커밋된 후,
     * 별도 스레드에서 색 생성을 시작.
     */
    @Async("diaryRewardExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            DiaryRewardGenerationRequested event
    ) {
        diaryRewardGenerationService.generate(
                event.rewardId()
        );
    }
}