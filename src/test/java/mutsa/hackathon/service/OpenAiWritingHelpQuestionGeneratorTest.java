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
                        List.of(),
                        List.of()
                )
        );

        assertTrue(
                input.contains(
                        "AVAILABLE - use as context without assuming it happened today"
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
                        List.of(),
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

    @Test
    void earlierQuestions_areIncludedToPreventRepeatedStatusQuestions() {
        String input = generator.buildInput(
                new WritingHelpPrompt(
                        "test-user",
                        "student",
                        "{\"stableMemories\":[{\"text\":\"has a dog\"}]}",
                        1,
                        List.of(),
                        List.of(
                                "Has the dog recovered?"
                        )
                )
        );

        assertTrue(
                input.contains(
                        "Earlier writing-help questions:"
                )
        );
        assertTrue(
                input.contains(
                        "Has the dog recovered?"
                )
        );
    }
}
