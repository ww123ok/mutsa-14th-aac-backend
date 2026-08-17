package mutsa.hackathon.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DevDatedDiaryControllerConditionTest {

    @Test
    void 날짜지정_일기_API는_환경변수가_true일_때만_활성화된다() {
        ConditionalOnProperty condition =
                DevDatedDiaryController.class
                        .getAnnotation(
                                ConditionalOnProperty.class
                        );

        assertNotNull(condition);
        assertEquals(
                "app.dev",
                condition.prefix()
        );
        assertEquals(
                "dated-diary-enabled",
                condition.name()[0]
        );
        assertEquals(
                "true",
                condition.havingValue()
        );
        assertFalse(
                condition.matchIfMissing()
        );
    }
}
