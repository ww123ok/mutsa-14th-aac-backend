package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExperienceFragmentTimelineFormatterTest {

    @Test
    void groupsConsecutiveEntriesInTheSamePeriodWithBlankLines() {
        String content = "[PM 07:00] 밥을 먹었다.\n[PM 08:00] 잠을 잤다.\n[PM 10:00] 과제를 했다.";

        String normalized = ExperienceFragmentTimelineFormatter.normalize(content);

        assertEquals(
                "[저녁]\n밥을 먹었다.\n\n잠을 잤다.\n\n[밤]\n과제를 했다.",
                normalized
        );
    }

    @Test
    void leavesOrdinaryTimeExpressionsOutsideBracketsUntouchedForAiGeneralization() {
        String content = "7시에 마라탕을 먹었다.";

        assertEquals(content, ExperienceFragmentTimelineFormatter.normalize(content));
    }

    @Test
    void doesNotMergeTheSamePeriodWhenAnotherPeriodAppearsInBetween() {
        String content = "[PM 07:00] 저녁을 먹었다.\n[PM 10:00] 과제를 했다.\n[PM 11:00] 간식을 먹었다.";

        String normalized = ExperienceFragmentTimelineFormatter.normalize(content);

        assertEquals(
                "[저녁]\n저녁을 먹었다.\n\n[밤]\n과제를 했다.\n\n간식을 먹었다.",
                normalized
        );
    }
}
