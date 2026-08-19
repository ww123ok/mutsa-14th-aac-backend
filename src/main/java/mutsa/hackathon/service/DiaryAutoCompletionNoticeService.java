package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.DiaryAutoCompletionNotice;
import mutsa.hackathon.dto.DiaryAutoCompletionNoticeResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.DiaryAutoCompletionNoticeRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DiaryAutoCompletionNoticeService {

    private final DiaryAutoCompletionNoticeRepository
            noticeRepository;

    private final DiaryRepository
            diaryRepository;

    private final Clock serviceClock;

    @Transactional
    public DiaryAutoCompletionNoticeResponse
    getPendingNotice(
            Long userId
    ) {
        while (true) {
            DiaryAutoCompletionNotice notice =
                    noticeRepository
                            .findFirstByUserIdAndViewedAtIsNullOrderByAutoCompletedAtAscIdAsc(
                                    userId
                            )
                            .orElse(null);

            if (notice == null) {
                return null;
            }

            boolean diaryStillVisible =
                    diaryRepository
                            .findByIdAndUserIdAndDeletedFalse(
                                    notice.getDiaryId(),
                                    userId
                            )
                            .isPresent();

            if (diaryStillVisible) {
                return DiaryAutoCompletionNoticeResponse
                        .from(
                                notice
                        );
            }

            /*
             * 이미 삭제된 자동 완료 일기라면 더 이상 모달로 보여줄 수 없으므로
             * 확인 처리하고 다음 미확인 안내를 계속 탐색
             */
            notice.markViewed(
                    LocalDateTime.now(
                            serviceClock
                    )
            );
        }
    }

    @Transactional
    public DiaryAutoCompletionNoticeResponse markViewed(
            Long userId,
            Long noticeId
    ) {
        DiaryAutoCompletionNotice notice =
                noticeRepository
                        .findByIdAndUserId(
                                noticeId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode
                                                .DIARY_AUTO_COMPLETION_NOTICE_NOT_FOUND
                                )
                        );

        notice.markViewed(
                LocalDateTime.now(
                        serviceClock
                )
        );

        return DiaryAutoCompletionNoticeResponse.from(
                notice
        );
    }
}
