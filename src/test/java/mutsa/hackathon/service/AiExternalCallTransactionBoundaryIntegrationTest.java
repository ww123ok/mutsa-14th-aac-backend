package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = {
                "app.openai.reflection-enabled=false",
                "app.openai.reward-enabled=false",
                "app.openai.memory-extraction-enabled=false"
        }
)
@Import(
        AiExternalCallTransactionBoundaryIntegrationTest
                .TestAiConfiguration.class
)
class AiExternalCallTransactionBoundaryIntegrationTest {

    @Autowired
    private DiaryService
            diaryService;

    @Autowired
    private AiWritingHelpService
            aiWritingHelpService;

    @Autowired
    private AppUserRepository
            appUserRepository;

    @Autowired
    private TransactionRecorder
            transactionRecorder;

    @Test
    void 성찰질문과_작성도움_OpenAI호출은_DB트랜잭션_밖에서_실행된다() {

        AppUser user =
                AppUser.createKakaoUser(
                        "transaction-boundary-"
                                + System.nanoTime(),
                        "데이빗",
                        null,
                        null
                );

        user.updatePersonalSettings(
                "데이빗",
                "대학생",
                LocalTime.of(21, 0),
                false
        );

        AppUser savedUser =
                appUserRepository
                        .saveAndFlush(user);

        transactionRecorder.reset();

        diaryService.create(
                savedUser.getId(),
                new DiaryCreateRequest(
                        "오늘 팀원들과 프로젝트를 진행했다.",
                        false
                )
        );

        assertTrue(
                transactionRecorder
                        .reflectionCalled()
        );

        assertFalse(
                transactionRecorder
                        .reflectionTransactionActive()
        );

        aiWritingHelpService
                .generateQuestion(
                        savedUser.getId()
                );

        assertTrue(
                transactionRecorder
                        .writingHelpCalled()
        );

        assertFalse(
                transactionRecorder
                        .writingHelpTransactionActive()
        );
    }

    @TestConfiguration(
            proxyBeanMethods = false
    )
    static class TestAiConfiguration {

        @Bean
        TransactionRecorder
        transactionRecorder() {
            return new TransactionRecorder();
        }

        @Bean
        @Primary
        DiaryReflectionQuestionGenerator
        transactionTestReflectionGenerator(
                TransactionRecorder recorder
        ) {
            return prompt -> {
                recorder
                        .recordReflectionCall();

                return "오늘 기록에서 가장 의미 있었던 순간은 무엇인가요?";
            };
        }

        @Bean
        @Primary
        WritingHelpQuestionGenerator
        transactionTestWritingHelpGenerator(
                TransactionRecorder recorder
        ) {
            return prompt -> {
                recorder
                        .recordWritingHelpCall();

                return "오늘 가장 기억에 남은 장면은 무엇인가요?";
            };
        }
    }

    static class TransactionRecorder {

        private final AtomicBoolean
                reflectionCalled =
                new AtomicBoolean(false);

        private final AtomicBoolean
                reflectionTransactionActive =
                new AtomicBoolean(false);

        private final AtomicBoolean
                writingHelpCalled =
                new AtomicBoolean(false);

        private final AtomicBoolean
                writingHelpTransactionActive =
                new AtomicBoolean(false);

        void recordReflectionCall() {
            reflectionCalled.set(true);

            reflectionTransactionActive.set(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );
        }

        void recordWritingHelpCall() {
            writingHelpCalled.set(true);

            writingHelpTransactionActive.set(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );
        }

        void reset() {
            reflectionCalled.set(false);
            reflectionTransactionActive.set(false);
            writingHelpCalled.set(false);
            writingHelpTransactionActive.set(false);
        }

        boolean reflectionCalled() {
            return reflectionCalled.get();
        }

        boolean reflectionTransactionActive() {
            return reflectionTransactionActive.get();
        }

        boolean writingHelpCalled() {
            return writingHelpCalled.get();
        }

        boolean writingHelpTransactionActive() {
            return writingHelpTransactionActive.get();
        }
    }
}