package mutsa.hackathon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
public class WeeklyRewardConfig {

    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock weeklyRewardClock() {
        return Clock.system(SERVICE_ZONE);
    }

    @Bean(name = "weeklyRewardExecutor")
    @ConditionalOnProperty(
            prefix = "app.weekly-reward",
            name = "enabled",
            havingValue = "true"
    )
    public Executor weeklyRewardExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("weekly-reward-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.initialize();
        return executor;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "app.weekly-reward",
            name = "enabled",
            havingValue = "true"
    )
    public S3Client weeklyRewardS3Client(
            @Value("${app.weekly-reward.s3.region:ap-northeast-2}")
            String region
    ) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "app.weekly-reward",
            name = "enabled",
            havingValue = "true"
    )
    public S3Presigner weeklyRewardS3Presigner(
            @Value("${app.weekly-reward.s3.region:ap-northeast-2}")
            String region
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}