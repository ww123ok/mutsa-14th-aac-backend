package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public DiaryResponse create(Long userId, DiaryCreateRequest request) {
        LocalDate recordedDate = request.recordedDate() == null
                ? LocalDate.now()
                : request.recordedDate();

        if (diaryRepository.existsByUserIdAndRecordedDate(userId, recordedDate)) {
            throw new ProjectException(ErrorCode.DIARY_ALREADY_WRITTEN_TODAY);
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        Diary diary = Diary.create(user, request.content(), recordedDate);
        return DiaryResponse.from(diaryRepository.save(diary));
    }

    @Transactional(readOnly = true)
    public List<DiaryResponse> getMonthlyDiaries(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return diaryRepository.findAllByUserIdAndRecordedDateBetweenAndDeletedFalseOrderByRecordedDateAsc(
                        userId,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                ).stream()
                .map(DiaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiaryResponse getDiary(Long userId, Long diaryId) {
        return DiaryResponse.from(findActiveDiary(userId, diaryId));
    }

    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        findActiveDiary(userId, diaryId).softDelete();
    }

    private Diary findActiveDiary(Long userId, Long diaryId) {
        return diaryRepository.findByIdAndUserIdAndDeletedFalse(diaryId, userId)
                .orElseThrow(() -> new ProjectException(ErrorCode.DIARY_NOT_FOUND));
    }
}