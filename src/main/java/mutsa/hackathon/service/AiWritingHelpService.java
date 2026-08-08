package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AiWritingHelpService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DAILY_LIMIT = 3;

    private final AppUserRepository appUserRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final WritingHelpQuestionGenerator writingHelpQuestionGenerator;

    @Transactional
    public WritingHelpQuestionResponse generateQuestion(Long userId) {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        long usedCount = aiQuestionRepository.countByUserIdAndQuestionTypeAndAskedDate(
                userId,
                AiQuestionType.WRITING_HELP,
                today
        );

        if (usedCount >= DAILY_LIMIT) {
            throw new ProjectException(ErrorCode.WRITING_HELP_LIMIT_EXCEEDED);
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        String questionText = writingHelpQuestionGenerator.generate(
                new WritingHelpPrompt(
                        user.getNickname(),
                        user.getJob(),
                        user.getAiMemoryProfile()
                )
        );

        AiQuestion question = aiQuestionRepository.save(
                AiQuestion.createWritingHelp(
                        user,
                        questionText,
                        Math.toIntExact(usedCount + 1),
                        today,
                        QuestionGenerationSource.AI
                )
        );

        return WritingHelpQuestionResponse.from(question, DAILY_LIMIT);
    }
}
