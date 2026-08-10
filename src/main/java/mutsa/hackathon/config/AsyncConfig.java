package mutsa.hackathon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent
        .ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "diaryRewardExecutor")
    public Executor diaryRewardExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);

        executor.setThreadNamePrefix(
                "diary-reward-"
        );

        executor.setWaitForTasksToCompleteOnShutdown(
                true
        );

        executor.setAwaitTerminationSeconds(
                20
        );

        executor.initialize();

        return executor;
    }

    /**
     * 개인화 기억 추출은 색상 생성과 별도의
     * 외부 OpenAI 호출이므로 Thread Pool을 분리
     */
    @Bean(name = "diaryMemoryExecutor")
    public Executor diaryMemoryExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);

        executor.setThreadNamePrefix(
                "diary-memory-"
        );

        executor.setWaitForTasksToCompleteOnShutdown(
                true
        );

        executor.setAwaitTerminationSeconds(
                20
        );

        executor.initialize();

        return executor;
    }
}