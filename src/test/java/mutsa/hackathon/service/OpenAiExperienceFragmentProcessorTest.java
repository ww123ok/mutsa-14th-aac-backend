package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiExperienceFragmentProcessorTest {

    private final OpenAiExperienceFragmentProcessor processor =
            new OpenAiExperienceFragmentProcessor(
                    null,
                    null
            );

    @Test
    void anonymizationInstructions_preserveNonIdentifyingEverydayContext() {
        String instructions = processor.instructions();

        assertTrue(
                instructions.contains(
                        "Do not over-generalize ordinary nouns"
                )
        );
        assertTrue(
                instructions.contains(
                        "'홍익대학교' becomes '학교'"
                )
        );
        assertTrue(
                instructions.contains(
                        "named cafe becomes '근처 카페'"
                )
        );
    }

    @Test
    void anonymizationInstructions_coverRequiredPrivacyReviewRules() {
        String instructions = processor.instructions();

        assertTrue(instructions.contains("Required privacy and safety review"));
        assertTrue(instructions.contains("phone numbers, social media accounts"));
        assertTrue(instructions.contains("A generic word such as \"school\" must remain \"school\""));
        assertTrue(instructions.contains("I did a team project with A from the same department"));
    }
}
