package mutsa.hackathon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

        /*
         * 서버 종료 시 진행 중인 색 생성 작업이
         * 가능한 한 정상 종료되도록 대기
         */
        executor.setWaitForTasksToCompleteOnShutdown(
                true
        );

        executor.setAwaitTerminationSeconds(20);
        executor.initialize();

        return executor;
    }
}