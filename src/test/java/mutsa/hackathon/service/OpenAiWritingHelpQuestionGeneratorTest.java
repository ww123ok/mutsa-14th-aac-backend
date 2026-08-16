package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiWritingHelpQuestionGeneratorTest {

    private final OpenAiWritingHelpQuestionGenerator generator =
            new OpenAiWritingHelpQuestionGenerator(
                    null
            );

    @Test
    void approvedMemoryExists_marksPersonalizationAsRequired() {
        String input = generator.buildInput(
                new WritingHelpPrompt(
                        "test-user",
                        "student",
                        "{\"ongoingTopics\":[{\"text\":\"diet and exercise concern\"}]}",
                        1,
                        List.of()
                )
        );

        assertTrue(
                input.contains(
                        "AVAILABLE - personalization is required"
                )
        );
        assertTrue(
                input.contains(
                        "diet and exercise concern"
                )
        );
    }

    @Test
    void noApprovedMemory_allowsGenericQuestion() {
        String input = generator.buildInput(
                new WritingHelpPrompt(
                        "test-user",
                        "student",
                        null,
                        1,
                        List.of()
                )
        );

        assertTrue(
                input.contains(
                        "UNAVAILABLE - a generic question is allowed"
                )
        );
        assertTrue(
                input.contains(
                        "(no approved personalization memory)"
                )
        );
    }
}
