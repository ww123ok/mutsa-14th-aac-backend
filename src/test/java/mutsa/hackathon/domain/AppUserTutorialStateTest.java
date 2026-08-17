package mutsa.hackathon.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUserTutorialStateTest {

    @Test
    void completesTutorialOnlyOnce() {
        AppUser user = AppUser.createLocalUser("tutorial@example.com", "encoded-password");

        assertFalse(user.isTutorialCompleted());

        user.completeTutorial();
        assertTrue(user.isTutorialCompleted());
        assertNotNull(user.getTutorialCompletedAt());
    }
}
