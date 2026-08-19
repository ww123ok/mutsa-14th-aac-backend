package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiExperienceStructureExtractorTest {

    private final OpenAiExperienceStructureExtractor extractor =
            new OpenAiExperienceStructureExtractor(null, null);

    @Test
    void instructionsPrioritizeExperienceStructureOverSurfaceTopics() {
        String instructions = extractor.instructions();

        assertTrue(instructions.contains("surface"));
        assertTrue(instructions.contains("topic similarity"));
        assertTrue(instructions.contains("waiting for an interview result"));
        assertTrue(instructions.contains("상황: ... | 핵심 어려움: ... | 반응: ... | 영향 또는 변화: ..."));
    }
}
