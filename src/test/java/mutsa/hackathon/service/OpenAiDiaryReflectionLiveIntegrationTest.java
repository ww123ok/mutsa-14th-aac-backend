package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 OpenAI Responses API 연결을 검증하는 수동 테스트
 * 일반 clean test에서는 실행되지 않음.
 * OPENAI_LIVE_TEST=true 환경변수가 있을 때만 실행됨.
 * 실제 API 호출이 두 번 발생하므로 명시적으로 실행할 때만
 * 환경변수를 활성화해야 함.
 */
@SpringBootTest(
        properties = {
                "app.openai.reflection-enabled=true"
        }
)
@EnabledIfEnvironmentVariable(
        named = "OPENAI_LIVE_TEST",
        matches = "true"
)
class OpenAiDiaryReflectionLiveIntegrationTest {

    @Autowired
    private DiaryReflectionQuestionGenerator
            diaryReflectionQuestionGenerator;

    @Test
    void 실제_OpenAI가_일기내용을_반영한_성찰질문을_생성한다() {
        assertInstanceOf(
                OpenAiDiaryReflectionQuestionGenerator.class,
                diaryReflectionQuestionGenerator
        );

        String question =
                diaryReflectionQuestionGenerator.generate(
                        new DiaryReflectionPrompt(
                                "데이빗",
                                "대학생",
                                """
                                {
                                  "schemaVersion": 1,
                                  "stableMemories": [],
                                  "ongoingTopics": []
                                }
                                """,
                                "오늘 팀원들과 백엔드 오류를 하나씩 해결했고, 테스트가 모두 성공해서 뿌듯했다.",
                                true
                        )
                );

        assertValidQuestion(question);

        System.out.println(
                "[OpenAI 실호출 결과 - 일기 반영] "
                        + question
        );
    }

    @Test
    void 실제_OpenAI가_일기내용_없이_일반_성찰질문을_생성한다() {
        assertInstanceOf(
                OpenAiDiaryReflectionQuestionGenerator.class,
                diaryReflectionQuestionGenerator
        );

        String question =
                diaryReflectionQuestionGenerator.generate(
                        new DiaryReflectionPrompt(
                                "데이빗",
                                "대학생",
                                null,
                                null,
                                false
                        )
                );

        assertValidQuestion(question);

        System.out.println(
                "[OpenAI 실호출 결과 - 일기 미반영] "
                        + question
        );
    }

    private void assertValidQuestion(
            String question
    ) {
        assertNotNull(question);

        assertFalse(
                question.isBlank()
        );

        assertTrue(
                question.endsWith("?")
                        || question.endsWith("？")
        );

        assertTrue(
                question.length() <= 200
        );
    }
}