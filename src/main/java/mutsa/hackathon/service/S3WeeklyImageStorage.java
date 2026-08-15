package mutsa.hackathon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
public class S3WeeklyImageStorage implements WeeklyImageStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3WeeklyImageStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${app.weekly-reward.s3.bucket:}") String bucket
    ) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException(
                    "WEEKLY_REWARD_S3_BUCKET 설정이 필요합니다."
            );
        }
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket.trim();
    }

    @Override
    public StoredWeeklyImage store(
            WeeklyRewardGenerationContext context,
            GeneratedWeeklyImage image
    ) {
        String key = "weekly-rewards/%d/%s/reward-%d".formatted(
                context.userId(),
                context.weekStartDate(),
                context.weeklyRewardId()
        );

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(image.contentType())
                .contentLength((long) image.bytes().length)
                .cacheControl("private, max-age=3600")
                .serverSideEncryption(ServerSideEncryption.AES256)
                .metadata(Map.of(
                        "weekly-reward-id",
                        String.valueOf(context.weeklyRewardId())
                ))
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromBytes(image.bytes())
        );

        return new StoredWeeklyImage(key, image.contentType());
    }

    @Override
    public URI createReadUri(String key, Duration duration) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("조회할 S3 Object Key는 필수입니다.");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("이미지 URL 유효시간은 1초 이상이어야 합니다.");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        return URI.create(
                s3Presigner.presignGetObject(presignRequest).url().toString()
        );
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
    }
}