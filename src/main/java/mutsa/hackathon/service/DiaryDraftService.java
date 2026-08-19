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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DiaryDraftService {

    private final DiaryDraftRepository
            diaryDraftRepository;

    private final DiaryRepository
            diaryRepository;

    private final AppUserRepository
            appUserRepository;

    private final UserDayService
            userDayService;

    @Transactional
    public DiaryDraftResponse saveCurrentDraft(
            Long userId,
            DiaryDraftUpsertRequest request
    ) {
        LocalDate recordedDate =
                userDayService.currentDay(
                        userId
                );

        if (
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                userId,
                                recordedDate
                        )
        ) {
            throw new ProjectException(
                    ErrorCode
                            .DIARY_ALREADY_WRITTEN_TODAY
            );
        }

        DiaryDraft draft =
                diaryDraftRepository
                        .findByUserIdAndRecordedDate(
                                userId,
                                recordedDate
                        )
                        .map(existing -> {
                            existing.update(
                                    request.content(),
                                    request
                                            .shouldUseDiaryContentForPersonalization()
                            );
                            return existing;
                        })
                        .orElseGet(() ->
                                DiaryDraft.create(
                                        findUser(userId),
                                        recordedDate,
                                        request.content(),
                                        request
                                                .shouldUseDiaryContentForPersonalization()
                                )
                        );

        return DiaryDraftResponse.from(
                diaryDraftRepository.save(
                        draft
                )
        );
    }

    @Transactional(readOnly = true)
    public DiaryDraftResponse getCurrentDraft(
            Long userId
    ) {
        LocalDate recordedDate =
                userDayService.currentDay(
                        userId
                );

        if (
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                userId,
                                recordedDate
                        )
        ) {
            return null;
        }

        return diaryDraftRepository
                .findByUserIdAndRecordedDate(
                        userId,
                        recordedDate
                )
                .map(
                        DiaryDraftResponse::from
                )
                .orElse(null);
    }

    @Transactional
    public void deleteCurrentDraft(
            Long userId
    ) {
        LocalDate recordedDate =
                userDayService.currentDay(
                        userId
                );

        diaryDraftRepository
                .deleteByUserIdAndRecordedDate(
                        userId,
                        recordedDate
                );
    }

    private AppUser findUser(
            Long userId
    ) {
        return appUserRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }
}
