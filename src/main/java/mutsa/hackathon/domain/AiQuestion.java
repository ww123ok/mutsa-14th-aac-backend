package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "ai_question",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_question_diary_type",
                        columnNames = {"diary_id", "question_type"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_ai_question_user_date_type",
                        columnList = "user_id, asked_date, question_type"
                )
        }
)
public class AiQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private Diary diary;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private AiQuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_source", nullable = false, length = 20)
    private QuestionGenerationSource generationSource;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @Column(name = "asked_date", nullable = false)
    private LocalDate askedDate;

    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "memory_applied_at")
    private LocalDateTime memoryAppliedAt;

    public static AiQuestion createWritingHelp(
            AppUser user,
            String questionText,
            int questionOrder,
            LocalDate askedDate,
            QuestionGenerationSource generationSource
    ) {
        validateCommon(user, questionText, askedDate, generationSource);
        if (questionOrder < 1) {
            throw new IllegalArgumentException("질문 순서는 1 이상이어야 합니다.");
        }

        return AiQuestion.builder()
                .version(0L)
                .user(user)
                .questionType(AiQuestionType.WRITING_HELP)
                .generationSource(generationSource)
                .questionOrder(questionOrder)
                .askedDate(askedDate)
                .questionText(questionText.trim())
                .build();
    }

    public static AiQuestion createReflection(
            AppUser user,
            Diary diary,
            String questionText,
            LocalDate askedDate,
            QuestionGenerationSource generationSource
    ) {
        validateCommon(user, questionText, askedDate, generationSource);
        if (diary == null) {
            throw new IllegalArgumentException("성찰 질문에 연결할 일기는 필수입니다.");
        }

        return AiQuestion.builder()
                .version(0L)
                .user(user)
                .diary(diary)
                .questionType(AiQuestionType.REFLECTION)
                .generationSource(generationSource)
                .questionOrder(1)
                .askedDate(askedDate)
                .questionText(questionText.trim())
                .build();
    }

    public void submitReflectionAnswer(String answerText) {
        if (questionType != AiQuestionType.REFLECTION) {
            throw new IllegalStateException("작성 도움 질문에는 별도 답변을 저장할 수 없습니다.");
        }
        if (this.answerText != null) {
            throw new IllegalStateException("이미 성찰 답변을 제출했습니다.");
        }
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("성찰 답변은 공백일 수 없습니다.");
        }

        this.answerText = answerText.trim();
        this.answeredAt = LocalDateTime.now();
    }

    public void markMemoryApplied() {
        if (answerText == null) {
            throw new IllegalStateException("답변이 없는 질문은 메모리에 추가 반영할 수 없습니다.");
        }
        if (memoryAppliedAt == null) {
            this.memoryAppliedAt = LocalDateTime.now();
        }
    }

    public boolean isAnswered() {
        return answerText != null;
    }

    private static void validateCommon(
            AppUser user,
            String questionText,
            LocalDate askedDate,
            QuestionGenerationSource generationSource
    ) {
        if (user == null) {
            throw new IllegalArgumentException("질문 대상 사용자는 필수입니다.");
        }
        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException("질문 내용은 필수입니다.");
        }
        if (askedDate == null) {
            throw new IllegalArgumentException("질문 생성일은 필수입니다.");
        }
        if (generationSource == null) {
            throw new IllegalArgumentException("질문 생성 출처는 필수입니다.");
        }
    }
}