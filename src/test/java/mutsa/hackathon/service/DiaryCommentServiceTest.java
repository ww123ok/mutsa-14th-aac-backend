package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryComment;
import mutsa.hackathon.dto.DiaryCommentCreateRequest;
import mutsa.hackathon.dto.DiaryCommentResponse;
import mutsa.hackathon.repository.DiaryCommentRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryCommentServiceTest {

    @Mock
    private DiaryRepository
            diaryRepository;

    @Mock
    private DiaryCommentRepository
            diaryCommentRepository;

    private DiaryCommentService
            diaryCommentService;

    private Diary diary;

    @BeforeEach
    void setUp() {
        diaryCommentService =
                new DiaryCommentService(
                        diaryRepository,
                        diaryCommentRepository
                );

        AppUser user =
                AppUser.createKakaoUser(
                        "provider-1",
                        "사용자",
                        null,
                        null
                );
        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        diary = Diary.create(
                user,
                "이미 완료된 일기",
                LocalDate.of(
                        2026,
                        8,
                        18
                )
        );
        ReflectionTestUtils.setField(
                diary,
                "id",
                20L
        );
    }

    @Test
    void 완료된_일기에_추가_댓글을_남길_수_있다() {
        when(
                diaryRepository
                        .findByIdAndUserIdAndDeletedFalse(
                                20L,
                                1L
                        )
        ).thenReturn(
                Optional.of(diary)
        );

        when(
                diaryCommentRepository.save(
                        any(DiaryComment.class)
                )
        ).thenAnswer(invocation -> {
            DiaryComment comment =
                    invocation.getArgument(0);
            ReflectionTestUtils.setField(
                    comment,
                    "id",
                    30L
            );
            return comment;
        });

        DiaryCommentResponse response =
                diaryCommentService.addComment(
                        1L,
                        20L,
                        new DiaryCommentCreateRequest(
                                "그땐 이런 생각을 했구나."
                        )
                );

        assertEquals(
                30L,
                response.commentId()
        );
        assertEquals(
                "그땐 이런 생각을 했구나.",
                response.content()
        );
    }

    @Test
    void 댓글은_작성순서대로_조회한다() {
        DiaryComment first = DiaryComment.create(
                diary,
                "첫 번째 추가 기록"
        );
        DiaryComment second = DiaryComment.create(
                diary,
                "두 번째 추가 기록"
        );

        when(
                diaryRepository
                        .findByIdAndUserIdAndDeletedFalse(
                                20L,
                                1L
                        )
        ).thenReturn(
                Optional.of(diary)
        );

        when(
                diaryCommentRepository
                        .findAllByDiaryIdOrderByCreatedAtAscIdAsc(
                                20L
                        )
        ).thenReturn(
                List.of(
                        first,
                        second
                )
        );

        List<DiaryCommentResponse> responses =
                diaryCommentService.getComments(
                        1L,
                        20L
                );

        assertEquals(
                List.of(
                        "첫 번째 추가 기록",
                        "두 번째 추가 기록"
                ),
                responses.stream()
                        .map(
                                DiaryCommentResponse::content
                        )
                        .toList()
        );
    }
}
