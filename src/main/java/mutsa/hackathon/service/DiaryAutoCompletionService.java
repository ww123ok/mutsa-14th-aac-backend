package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.DiaryDraft;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.DiaryDraftRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryAutoCompletionService {

    private final DiaryDraftRepository
            diaryDraftRepository;

    private final DiaryRepository
            diaryRepository;

    private final DiaryService
            diaryService;

    private final UserDayService
            userDayService;

    private final Clock serviceClock;

    public AutoCompletionResult autoCompleteIfDue(
            Long draftId
    ) {
        DiaryDraft draft =
                diaryDraftRepository
                        .findByIdWithUser(
                                draftId
                        )
                        .orElse(null);

        if (draft == null) {
            return AutoCompletionResult.notProcessed();
        }

        LocalDateTime now =
                LocalDateTime.now(
                        serviceClock
                );

        LocalDate currentLogicalDay =
                userDayService.resolveDay(
                        now,
                        draft.getUser()
                                .getDayStartTime()
                );

        if (
                !draft.getRecordedDate()
                        .isBefore(
                                currentLogicalDay
                        )
        ) {
            return AutoCompletionResult.notProcessed();
        }

        Long userId =
                draft.getUser()
                        .getId();

        if (
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                userId,
                                draft.getRecordedDate()
                        )
        ) {
            diaryDraftRepository.deleteById(
                    draftId
            );
            return AutoCompletionResult.staleDraftRemoval();
        }

        DiaryCreateRequest request =
                new DiaryCreateRequest(
                        draft.getContent(),
                        draft.shouldUseDiaryContentForPersonalization()
                );

        try {
            Long diaryId =
                    diaryService
                            .autoCompleteForRecordedDate(
                                    userId,
                                    request,
                                    draft.getRecordedDate()
                            )
                            .diaryId();

            return AutoCompletionResult.completed(
                    diaryId
            );

        } catch (ProjectException exception) {
            if (
                    exception.getErrorCode()
                            == ErrorCode
                            .DIARY_ALREADY_WRITTEN_TODAY
            ) {
                diaryDraftRepository.deleteById(
                        draftId
                );
                return AutoCompletionResult
                        .staleDraftRemoval();
            }

            throw exception;
        }
    }

    public record AutoCompletionResult(
            boolean processed,
            boolean autoCompleted,
            boolean staleDraftRemoved,
            Long diaryId
    ) {
        static AutoCompletionResult notProcessed() {
            return new AutoCompletionResult(
                    false,
                    false,
                    false,
                    null
            );
        }

        static AutoCompletionResult staleDraftRemoval() {
            return new AutoCompletionResult(
                    true,
                    false,
                    true,
                    null
            );
        }

        static AutoCompletionResult completed(
                Long diaryId
        ) {
            return new AutoCompletionResult(
                    true,
                    true,
                    false,
                    diaryId
            );
        }
    }
}
