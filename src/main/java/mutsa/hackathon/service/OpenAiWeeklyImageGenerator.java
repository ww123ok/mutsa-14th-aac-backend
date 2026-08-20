package mutsa.hackathon.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.WeeklyRewardImageSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
@Slf4j
public class OpenAiWeeklyImageGenerator implements WeeklyImageGenerator {

    private static final int MAX_IMAGE_BYTES =
            25 * 1024 * 1024;
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile(
            "\"code\"\\s*:\\s*\"([^\"]+)\""
    );
    private static final Pattern ERROR_TYPE_PATTERN = Pattern.compile(
            "\"type\"\\s*:\\s*\"([^\"]+)\""
    );

    private final RestClient.Builder restClientBuilder;
    private final OpenAiWeeklyImageQualityValidator qualityValidator;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.weekly-reward.openai.image-model:gpt-image-2}")
    private String model;

    @Value("${app.weekly-reward.openai.image-square-size:${app.weekly-reward.openai.image-size:1024x1024}}")
    private String squareSize;

    @Value("${app.weekly-reward.openai.image-portrait-size:1024x1536}")
    private String portraitSize;

    @Value("${app.weekly-reward.openai.image-landscape-size:1536x1024}")
    private String landscapeSize;

    @Value("${app.weekly-reward.openai.image-quality:medium}")
    private String quality;

    @Value("${app.weekly-reward.openai.image-max-generation-attempts:2}")
    private int maxGenerationAttempts;

    @Value("${app.weekly-reward.openai.image-validation-strict:true}")
    private boolean strictValidation;

    @Value("${app.weekly-reward.openai.image-request-max-attempts:3}")
    private int maxRequestAttempts;

    @Value("${app.weekly-reward.openai.image-request-base-backoff-millis:750}")
    private long requestBaseBackoffMillis;

    @Value("${app.weekly-reward.openai.image-request-max-backoff-millis:5000}")
    private long requestMaxBackoffMillis;

    @Override
    public GeneratedWeeklyImage generate(
            WeeklyRewardGenerationContext context,
            WeeklyVisualPlan visualPlan
    ) {
        if (context == null || visualPlan == null) {
            throw new IllegalArgumentException(
                    "주간 이미지 생성 정보는 필수입니다."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API Key가 설정되지 않았습니다."
            );
        }

        String requestSize = WeeklyImagePromptFactory.resolveImageSize(
                visualPlan.visualCategory(),
                squareSize,
                portraitSize,
                landscapeSize
        );

        String basePrompt = buildPrompt(context, visualPlan);
        String currentPrompt = basePrompt;
        int attempts = normalizeAttempts(maxGenerationAttempts);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            log.info(
                    "Weekly image request: promptVersion=FINAL_USER_RULES, category={}, "
                            + "size={}, attempt={}, promptLength={}",
                    visualPlan.visualCategory(),
                    requestSize,
                    attempt,
                    currentPrompt.length()
            );

            GeneratedWeeklyImage image = requestImage(
                    context,
                    currentPrompt,
                    requestSize
            );

            WeeklyImageQualityReview review = qualityValidator.review(
                    image,
                    visualPlan.visualCategory(),
                    requestSize,
                    currentPrompt
            );

            if (!review.reviewed() || review.approved()) {
                return image;
            }

            log.warn(
                    "Weekly image rejected by quality gate: category={}, "
                            + "attempt={}, violations={}",
                    visualPlan.visualCategory(),
                    attempt,
                    review.violations()
            );

            if (attempt == attempts) {
                if (strictValidation) {
                    throw new IllegalStateException(
                            "주간 이미지가 카테고리 품질 검수를 통과하지 못했습니다."
                    );
                }

                log.warn(
                        "Weekly image quality retries exhausted; returning the last "
                                + "generated image because strict validation is disabled: category={}",
                        visualPlan.visualCategory()
                );
                return image;
            }

            currentPrompt = WeeklyImagePromptFactory.buildRetryPrompt(
                    basePrompt,
                    visualPlan.visualCategory(),
                    review
            );
        }

        throw new IllegalStateException(
                "주간 이미지 생성 시도 횟수가 올바르지 않습니다."
        );
    }

    private GeneratedWeeklyImage requestImage(
            WeeklyRewardGenerationContext context,
            String prompt,
            String requestSize
    ) {
        OpenAiImageRequest request = new OpenAiImageRequest(
                model,
                prompt,
                1,
                requestSize,
                quality,
                "opaque",
                "auto",
                "webp",
                "daybit-user-" + context.userId()
        );

        int requestAttempts = normalizeRequestAttempts(maxRequestAttempts);

        for (int requestAttempt = 1; requestAttempt <= requestAttempts; requestAttempt++) {
            try {
                OpenAiImageResponse response =
                        restClientBuilder
                                .baseUrl(baseUrl)
                                .defaultHeader(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + apiKey
                                )
                                .build()
                                .post()
                                .uri("/images/generations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(request)
                                .retrieve()
                                .body(OpenAiImageResponse.class);

                return parse(response);

            } catch (RestClientResponseException exception) {
                String requestId = responseHeader(
                        exception,
                        "x-request-id"
                );
                String responseBody = exception.getResponseBodyAsString();
                String errorCode = extractJsonString(
                        ERROR_CODE_PATTERN,
                        responseBody
                );
                String errorType = extractJsonString(
                        ERROR_TYPE_PATTERN,
                        responseBody
                );
                boolean retryable = isRetryableStatus(
                        exception.getStatusCode().value()
                ) && !"image_generation_user_error".equals(errorType);

                log.warn(
                        "OpenAI weekly image failed: status={}, model={}, "
                                + "requestAttempt={}/{}, requestId={}, errorType={}, "
                                + "errorCode={}, retryable={}",
                        exception.getStatusCode(),
                        model,
                        requestAttempt,
                        requestAttempts,
                        requestId,
                        errorType,
                        errorCode,
                        retryable
                );

                if (!retryable || requestAttempt == requestAttempts) {
                    throw new IllegalStateException(
                            "OpenAI 주간 이미지 요청에 실패했습니다. "
                                    + "status=" + exception.getStatusCode().value()
                                    + ", errorCode=" + errorCode
                                    + ", requestId=" + requestId,
                            exception
                    );
                }

                sleepBeforeRetry(exception, requestAttempt);

            } catch (RestClientException exception) {
                log.warn(
                        "OpenAI weekly image could not be completed: model={}, "
                                + "requestAttempt={}/{}, reason={}, retryable=true",
                        model,
                        requestAttempt,
                        requestAttempts,
                        exception.getClass().getSimpleName()
                );

                if (requestAttempt == requestAttempts) {
                    throw new IllegalStateException(
                            "OpenAI 주간 이미지 요청을 완료하지 못했습니다.",
                            exception
                    );
                }

                sleepBeforeRetry(null, requestAttempt);
            }
        }

        throw new IllegalStateException(
                "OpenAI 주간 이미지 요청 재시도 횟수가 올바르지 않습니다."
        );
    }

    private boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private int normalizeRequestAttempts(int value) {
        if (value < 1) {
            return 1;
        }
        return Math.min(value, 5);
    }

    private void sleepBeforeRetry(
            RestClientResponseException exception,
            int failedAttempt
    ) {
        long delayMillis = resolveRetryDelayMillis(
                exception,
                failedAttempt
        );

        if (delayMillis <= 0) {
            return;
        }

        log.info(
                "Retrying OpenAI weekly image request after {}ms: failedAttempt={}",
                delayMillis,
                failedAttempt
        );

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "OpenAI 주간 이미지 재시도 대기 중 인터럽트되었습니다.",
                    interruptedException
            );
        }
    }

    private long resolveRetryDelayMillis(
            RestClientResponseException exception,
            int failedAttempt
    ) {
        long maxBackoff = Math.max(
                0L,
                requestMaxBackoffMillis
        );

        long retryAfter = retryAfterMillis(exception);
        if (retryAfter >= 0) {
            return maxBackoff == 0
                    ? retryAfter
                    : Math.min(retryAfter, maxBackoff);
        }

        long baseBackoff = Math.max(
                0L,
                requestBaseBackoffMillis
        );
        if (baseBackoff == 0) {
            return 0L;
        }

        int exponent = Math.max(
                0,
                Math.min(failedAttempt - 1, 4)
        );
        long multiplier = 1L << exponent;
        long calculated;

        if (baseBackoff > Long.MAX_VALUE / multiplier) {
            calculated = Long.MAX_VALUE;
        } else {
            calculated = baseBackoff * multiplier;
        }

        return maxBackoff == 0
                ? calculated
                : Math.min(calculated, maxBackoff);
    }

    private long retryAfterMillis(
            RestClientResponseException exception
    ) {
        if (exception == null || exception.getResponseHeaders() == null) {
            return -1L;
        }

        String value = exception.getResponseHeaders().getFirst(
                HttpHeaders.RETRY_AFTER
        );
        if (value == null || value.isBlank()) {
            return -1L;
        }

        String trimmed = value.trim();

        try {
            long seconds = Long.parseLong(trimmed);
            return Math.max(0L, seconds) * 1_000L;
        } catch (NumberFormatException ignored) {
            // Retry-After can also be an RFC 1123 date.
        }

        try {
            ZonedDateTime retryAt = ZonedDateTime.parse(
                    trimmed,
                    DateTimeFormatter.RFC_1123_DATE_TIME
            );
            long millis = Duration.between(
                    ZonedDateTime.now(retryAt.getZone()),
                    retryAt
            ).toMillis();
            return Math.max(0L, millis);
        } catch (DateTimeParseException ignored) {
            return -1L;
        }
    }

    private String responseHeader(
            RestClientResponseException exception,
            String headerName
    ) {
        if (exception.getResponseHeaders() == null) {
            return "unknown";
        }

        String value = exception.getResponseHeaders().getFirst(headerName);
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private String extractJsonString(
            Pattern pattern,
            String body
    ) {
        if (body == null || body.isBlank()) {
            return "unknown";
        }

        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return "unknown";
        }
        return matcher.group(1);
    }

    private int normalizeAttempts(int value) {
        if (value < 1) {
            return 1;
        }
        return Math.min(value, 3);
    }

    private String buildPrompt(
            WeeklyRewardGenerationContext context,
            WeeklyVisualPlan visualPlan
    ) {
        String palette = context.days()
                .stream()
                .map(
                        WeeklyRewardGenerationContext
                                .DayRecord::colorHex
                )
                .distinct()
                .reduce(
                        (left, right) ->
                                left + ", " + right
                )
                .orElse("#D6A45C");

        return WeeklyImagePromptFactory.buildPrompt(
                visualPlan,
                palette
        );
    }

    private GeneratedWeeklyImage parse(
            OpenAiImageResponse response
    ) {
        if (
                response == null
                        || response.data() == null
                        || response.data().isEmpty()
                        || response.data()
                        .get(0)
                        .base64Json() == null
                        || response.data()
                        .get(0)
                        .base64Json()
                        .isBlank()
        ) {
            throw new IllegalStateException(
                    "OpenAI 이미지 응답이 비어 있습니다."
            );
        }

        byte[] bytes;

        try {
            bytes = Base64
                    .getDecoder()
                    .decode(
                            response.data()
                                    .get(0)
                                    .base64Json()
                    );

        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "OpenAI 이미지 Base64를 해석할 수 없습니다.",
                    exception
            );
        }

        if (
                bytes.length == 0
                        || bytes.length > MAX_IMAGE_BYTES
        ) {
            throw new IllegalStateException(
                    "OpenAI 이미지 크기가 허용 범위를 벗어났습니다."
            );
        }

        return new GeneratedWeeklyImage(
                bytes,
                "image/webp",
                "webp",
                WeeklyRewardImageSource.AI
        );
    }

    private record OpenAiImageRequest(
            String model,
            String prompt,
            int n,
            String size,
            String quality,
            String background,
            String moderation,

            @JsonProperty("output_format")
            String outputFormat,

            String user
    ) {
    }

    private record OpenAiImageResponse(
            List<OpenAiImageData> data
    ) {
    }

    private record OpenAiImageData(
            @JsonProperty("b64_json")
            String base64Json
    ) {
    }
}
