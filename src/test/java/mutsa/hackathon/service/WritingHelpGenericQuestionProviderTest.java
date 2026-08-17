package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WritingHelpGenericQuestionProviderTest {

    private final WritingHelpGenericQuestionProvider provider =
            new WritingHelpGenericQuestionProvider();

    @Test
    void 범용질문은_총_60개를_보유한다() {
        assertEquals(
                60,
                provider.questionCount()
        );

        assertTrue(
                provider.questions()
                        .contains(
                                "오늘 가장 기억에 남는 순간은 언제였나요?"
                        )
        );

        assertTrue(
                provider.questions()
                        .contains(
                                "오늘을 게임이라고 생각한다면, 오늘 얻은 아이템이나 경험치는 무엇이었나요?"
                        )
        );
    }

    @Test
    void 이미_물었던_동일문구는_후보에서_제외한다() {
        String excluded =
                "오늘 가장 기억에 남는 순간은 언제였나요?";

        for (int index = 0; index < 100; index++) {
            String selected =
                    provider.nextQuestion(
                            List.of(excluded),
                            List.of()
                    );

            assertFalse(
                    excluded.equals(selected)
            );
        }
    }

    @Test
    void 오늘_이미_사용한_범용질문의_카테고리는_우선_피한다() {
        String firstQuestion =
                "오늘 가장 기억에 남는 순간은 언제였나요?";

        WritingHelpGenericQuestionCategory firstCategory =
                provider.categoryOf(
                        firstQuestion
                );

        assertNotNull(firstCategory);

        for (int index = 0; index < 100; index++) {
            String selected =
                    provider.nextQuestion(
                            List.of(firstQuestion),
                            List.of(firstQuestion)
                    );

            assertNotEquals(
                    firstCategory,
                    provider.categoryOf(
                            selected
                    )
            );
        }
    }

    @Test
    void 최근맥락이_없는_신규사용자가_세번_받아도_범용질문_카테고리가_겹치지_않는다() {
        List<String> todayQuestions =
                new ArrayList<>();

        Set<WritingHelpGenericQuestionCategory> categories =
                new HashSet<>();

        for (int order = 1; order <= 3; order++) {
            String selected =
                    provider.nextQuestion(
                            todayQuestions,
                            todayQuestions
                    );

            WritingHelpGenericQuestionCategory category =
                    provider.categoryOf(
                            selected
                    );

            assertNotNull(category);
            categories.add(category);
            todayQuestions.add(selected);
        }

        assertEquals(
                3,
                categories.size()
        );
    }
}
