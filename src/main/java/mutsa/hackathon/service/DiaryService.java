package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public DiaryCreateResponse create(Long userId, DiaryCreateRequest request) {
        LocalDate recordedDate = request.recordedDate() == null
                ? LocalDate.now()
                : request.recordedDate();

        if (diaryRepository.existsByUserIdAndRecordedDate(userId, recordedDate)) {
            throw new ProjectException(ErrorCode.DIARY_ALREADY_WRITTEN_TODAY);
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        Diary diary = Diary.create(user, request.content(), recordedDate);
        return DiaryCreateResponse.from(diaryRepository.save(diary));
    }
}
