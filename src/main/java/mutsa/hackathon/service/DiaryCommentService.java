package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryComment;
import mutsa.hackathon.dto.DiaryCommentCreateRequest;
import mutsa.hackathon.dto.DiaryCommentResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.DiaryCommentRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryCommentService {

    private final DiaryRepository
            diaryRepository;

    private final DiaryCommentRepository
            diaryCommentRepository;

    @Transactional
    public DiaryCommentResponse addComment(
            Long userId,
            Long diaryId,
            DiaryCommentCreateRequest request
    ) {
        Diary diary = findDiary(
                userId,
                diaryId
        );

        DiaryComment comment =
                diaryCommentRepository.save(
                        DiaryComment.create(
                                diary,
                                request.content()
                        )
                );

        return DiaryCommentResponse.from(
                comment
        );
    }

    @Transactional(readOnly = true)
    public List<DiaryCommentResponse> getComments(
            Long userId,
            Long diaryId
    ) {
        findDiary(
                userId,
                diaryId
        );

        return diaryCommentRepository
                .findAllByDiaryIdOrderByCreatedAtAscIdAsc(
                        diaryId
                )
                .stream()
                .map(
                        DiaryCommentResponse::from
                )
                .toList();
    }

    private Diary findDiary(
            Long userId,
            Long diaryId
    ) {
        return diaryRepository
                .findByIdAndUserIdAndDeletedFalse(
                        diaryId,
                        userId
                )
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.DIARY_NOT_FOUND
                        )
                );
    }
}
