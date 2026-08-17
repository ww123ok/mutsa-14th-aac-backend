package mutsa.hackathon.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "OPENAI_LIVE_TEST",
        matches = "true"
)
class OpenAiWritingHelpQuestionLiveIntegrationTest {

    private static final Path LIVE_RESULT_PATH =
            Path.of(
                    "build",
                    "reports",
                    "writing-help-live-results.txt"
            );

    @Autowired
    private WritingHelpQuestionGenerator
            writingHelpQuestionGenerator;

    @BeforeAll
    static void resetLiveResultFile() throws IOException {
        Files.createDirectories(
                LIVE_RESULT_PATH.getParent()
        );

        Files.writeString(
                LIVE_RESULT_PATH,
                "DAYBIT writing-help OpenAI live results\n"
                        + "========================================\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    @Test
    void 실제_OpenAI가_카페_초안에_살을붙이는_질문을_생성한다() {
        assertInstanceOf(
                OpenAiWritingHelpQuestionGenerator.class,
                writingHelpQuestionGenerator
        );

        String question =
                writingHelpQuestionGenerator.generate(
                        new WritingHelpPrompt(
                                WritingHelpQuestionContextType.CURRENT_DRAFT,
                                "수업이 끝나고 혼자 카페에 갔다.",
                                List.of(),
                                1,
                                List.of()
                        )
                );

        assertValidQuestion(question);

        recordResult(
                "CURRENT_DRAFT_SHORT",
                question
        );
    }

    @Test
    void 실제_OpenAI가_추가된_초안과_이전질문을_보고_다른_세부질문을_생성한다() {
        String question =
                writingHelpQuestionGenerator.generate(
                        new WritingHelpPrompt(
                                WritingHelpQuestionContextType.CURRENT_DRAFT,
                                "수업이 끝나고 혼자 카페에 갔다. 창가 자리에 앉아서 디저트를 먹었다.",
                                List.of(),
                                2,
                                List.of(
                                        "카페에서 가장 눈에 들어온 분위기나 인테리어는 어땠나요?"
                                )
                        )
                );

        assertValidQuestion(question);

        recordResult(
                "CURRENT_DRAFT_EXPANDED",
                question
        );
    }

    @Test
    void 실제_OpenAI가_최근알바_맥락을_오늘일로_단정하지않고_질문한다() {
        String question =
                writingHelpQuestionGenerator.generate(
                        new WritingHelpPrompt(
                                WritingHelpQuestionContextType.RECENT_CONTEXT,
                                null,
                                List.of(
                                        new WritingHelpRecentDiary(
                                                LocalDate.of(2026, 8, 16),
                                                "최근에 카페 아르바이트를 시작했다. 주문 받는 일이 아직 낯설어서 긴장했다."
                                        )
                                ),
                                1,
                                List.of()
                        )
                );

        assertValidQuestion(question);
        assertFalse(
                question.contains("오늘"),
                "최근 맥락 질문은 과거 사건을 오늘 일로 끌고 오면 안 됩니다."
        );

        recordResult(
                "RECENT_CONTEXT_WORK",
                question
        );
    }

    @Test
    void 실제_OpenAI가_세번째_최근맥락에서_다른_최근주제를_시간축에맞게_질문한다() {
        String question =
                writingHelpQuestionGenerator.generate(
                        new WritingHelpPrompt(
                                WritingHelpQuestionContextType.RECENT_CONTEXT,
                                null,
                                List.of(
                                        new WritingHelpRecentDiary(
                                                LocalDate.of(2026, 8, 15),
                                                "팀 프로젝트 첫 회의를 했다. 역할을 나눴는데 앞으로 일정이 조금 걱정된다."
                                        ),
                                        new WritingHelpRecentDiary(
                                                LocalDate.of(2026, 8, 14),
                                                "이번 주부터 새로운 운동 루틴을 시작해 보기로 했다."
                                        )
                                ),
                                3,
                                List.of(
                                        "최근 시작한 카페 알바에는 조금씩 적응하고 있나요?",
                                        "오늘 나도 모르게 웃었던 순간이 있었나요?"
                                )
                        )
                );

        assertValidQuestion(question);
        assertFalse(question.contains("오늘"));

        recordResult(
                "RECENT_CONTEXT_OTHER_TOPIC",
                question
        );
    }

    private void assertValidQuestion(
            String question
    ) {
        assertNotNull(question);
        assertFalse(question.isBlank());
        assertTrue(
                question.endsWith("?")
                        || question.endsWith("？")
        );
        assertTrue(question.length() <= 200);
    }

    private synchronized void recordResult(
            String scenario,
            String question
    ) {
        String resultLine =
                "[" + scenario + "] "
                        + question
                        + System.lineSeparator();

        try {
            Files.writeString(
                    LIVE_RESULT_PATH,
                    resultLine,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to write OpenAI live-test result file.",
                    exception
            );
        }

        System.out.println(
                "[OPENAI_LIVE]["
                        + scenario
                        + "] "
                        + toUnicodeEscapes(question)
        );
    }

    private String toUnicodeEscapes(
            String value
    ) {
        StringBuilder escaped =
                new StringBuilder();

        value.codePoints()
                .forEach(codePoint -> {
                    if (
                            codePoint >= 0x20
                                    && codePoint <= 0x7E
                    ) {
                        escaped.appendCodePoint(
                                codePoint
                        );
                        return;
                    }

                    if (codePoint <= 0xFFFF) {
                        escaped.append(
                                String.format(
                                        "\\u%04X",
                                        codePoint
                                )
                        );
                        return;
                    }

                    char[] surrogatePair =
                            Character.toChars(
                                    codePoint
                            );

                    for (char surrogate : surrogatePair) {
                        escaped.append(
                                String.format(
                                        "\\u%04X",
                                        (int) surrogate
                                )
                        );
                    }
                });

        return escaped.toString();
    }
}
