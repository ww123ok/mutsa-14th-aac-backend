package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition
        .EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void 실제_OpenAI가_오늘일기만_기반으로_성찰질문을_생성한다() {

        assertInstanceOf(
                OpenAiDiaryReflectionQuestionGenerator.class,
                diaryReflectionQuestionGenerator
        );

        String question =
                diaryReflectionQuestionGenerator
                        .generate(
                                new DiaryReflectionPrompt(
                                        """
                                        오늘 팀원들과 백엔드 오류를 하나씩 해결했고,
                                        테스트가 모두 성공해서 뿌듯했다.
                                        """
                                )
                        );

        assertValidQuestion(
                question
        );

        System.out.println(
                "[OpenAI 실호출 결과 - 성찰 질문] "
                        + question
        );
    }

    private void assertValidQuestion(
            String question
    ) {
        assertNotNull(
                question
        );

        assertFalse(
                question.isBlank()
        );

        assertTrue(
                question.endsWith("?")
                        || question.endsWith("？")
        );

        assertTrue(
                question.length()
                        <= 200
        );
    }
}