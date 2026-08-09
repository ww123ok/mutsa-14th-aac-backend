package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.dto.ReflectionAnswerRequest;
import mutsa.hackathon.dto.ReflectionAnswerResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryReflectionService {

    private final DiaryRepository diaryRepository;

    private final AiQuestionRepository
            aiQuestionRepository;

    @Transactional
    public ReflectionAnswerResponse submitAnswer(
            Long userId,
            Long diaryId,
            ReflectionAnswerRequest request
    ) {
        Diary diary = diaryRepository
                .findByIdAndUserIdAndDeletedFalse(
                        diaryId,
                        userId
                )
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.DIARY_NOT_FOUND
                        )
                );

        AiQuestion reflectionQuestion =
                aiQuestionRepository
                        .findByDiaryIdAndQuestionType(
                                diary.getId(),
                                AiQuestionType.REFLECTION
                        )
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode
                                                .QUESTION_NOT_FOUND
                                )
                        );

        if (reflectionQuestion.isAnswered()) {
            throw new ProjectException(
                    ErrorCode
                            .REFLECTION_ANSWER_ALREADY_SUBMITTED
            );
        }

        reflectionQuestion.submitReflectionAnswer(
                request.answerText()
        );

        return ReflectionAnswerResponse.from(
                reflectionQuestion
        );
    }
}