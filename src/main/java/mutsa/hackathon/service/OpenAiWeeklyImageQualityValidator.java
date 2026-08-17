package mutsa.hackathon.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
@Slf4j
public class OpenAiWeeklyImageQualityValidator {

    private static final int MAX_OUTPUT_TOKENS = 700;

    private static final String INSTRUCTIONS = """
            You are the strict visual-quality gate for DAYBIT weekly reward images.

            Review only the supplied generated image against:
            1. the selected category,
            2. the expected canvas orientation,
            3. the supplied hard checklist.

            Do not infer missing diary facts. Do not reward generic beauty.
            Reject an image when a hard checklist item is visibly violated.
            A visually attractive image still fails when it uses the wrong category,
            wrong orientation, a collage, several literal scenes, a visible face,
            UI, explanatory text, a weak focal hierarchy, or prohibited style.

            Return:
            - reviewed: always true
            - approved: true only when every visible hard rule passes
            - violations: short English descriptions of visible failures, at most eight
            - correctionPrompt: concise English imperatives that correct only those failures

            The correction prompt must preserve the selected category and expected orientation.
            Never introduce a new event, place, person, brand, artist, franchise, or story.
            """;

    private final RestClient.Builder restClientBuilder;
    private final JsonMapper jsonMapper;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.weekly-reward.openai.image-validation-enabled:true}")
    private boolean validationEnabled;

    @Value("${app.weekly-reward.openai.image-validation-model:${app.weekly-reward.openai.text-model:gpt-5.6-terra}}")
    private String validationModel;

    @Value("${app.weekly-reward.openai.image-validation-detail:low}")
    private String validationDetail;

    public WeeklyImageQualityReview review(
            GeneratedWeeklyImage image,
            WeeklyVisualCategory category,
            String expectedSize
    ) {
        if (!validationEnabled) {
            return WeeklyImageQualityReview.skipped();
        }
        if (image == null || category == null) {
            throw new IllegalArgumentException(
                    "주간 이미지 검수 정보는 필수입니다."
            );
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Weekly image validation skipped: OpenAI API key is empty");
            return WeeklyImageQualityReview.skipped();
        }

        String dataUrl = "data:"
                + image.contentType()
                + ";base64,"
                + Base64.getEncoder().encodeToString(image.bytes());

        String reviewInput = """
                SELECTED CATEGORY: %s
                EXPECTED IMAGE SIZE: %s
                EXPECTED ORIENTATION: %s

                HARD CHECKLIST:
                %s

                Inspect the image now. Approve it only when all visible hard rules pass.
                """.formatted(
                category.name(),
                expectedSize,
                category.imageAspect().name(),
                WeeklyImagePromptFactory.validationChecklist(category)
        );

        List<Map<String, Object>> content = List.of(
                Map.of(
                        "type", "input_text",
                        "text", reviewInput
                ),
                Map.of(
                        "type", "input_image",
                        "image_url", dataUrl,
                        "detail", normalizeDetail(validationDetail)
                )
        );

        OpenAiRequest request = new OpenAiRequest(
                validationModel,
                false,
                MAX_OUTPUT_TOKENS,
                INSTRUCTIONS,
                List.of(Map.of(
                        "role", "user",
                        "content", content
                )),
                createTextConfiguration()
        );

        try {
            OpenAiResponse response = restClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + apiKey
                    )
                    .build()
                    .post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OpenAiResponse.class);

            return parse(response);

        } catch (RestClientResponseException exception) {
            log.warn(
                    "Weekly image validation skipped: status={}, model={}",
                    exception.getStatusCode(),
                    validationModel
            );
            return WeeklyImageQualityReview.skipped();

        } catch (RestClientException | IllegalStateException exception) {
            log.warn(
                    "Weekly image validation skipped: model={}, reason={}",
                    validationModel,
                    exception.getClass().getSimpleName()
            );
            return WeeklyImageQualityReview.skipped();
        }
    }

    private WeeklyImageQualityReview parse(OpenAiResponse response) {
        String outputText = extractOutputText(response);

        try {
            ReviewPayload payload = jsonMapper.readValue(
                    outputText,
                    ReviewPayload.class
            );

            if (payload == null) {
                throw new IllegalStateException(
                        "OpenAI 이미지 검수 결과가 비어 있습니다."
                );
            }

            return new WeeklyImageQualityReview(
                    payload.reviewed(),
                    payload.approved(),
                    payload.violations(),
                    payload.correctionPrompt()
            );

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "OpenAI 이미지 검수 JSON을 해석할 수 없습니다.",
                    exception
            );
        }
    }

    private String extractOutputText(OpenAiResponse response) {
        if (response == null) {
            throw new IllegalStateException(
                    "OpenAI 이미지 검수 응답이 비어 있습니다."
            );
        }

        String text = response.outputText();

        if (text == null || text.isBlank()) {
            text = response.output() == null
                    ? null
                    : response.output().stream()
                    .filter(output -> output.content() != null)
                    .flatMap(output -> output.content().stream())
                    .map(OpenAiContent::text)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
        }

        if (text == null || text.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI가 사용할 수 있는 이미지 검수 결과를 반환하지 않았습니다."
            );
        }

        return text.trim();
    }

    private OpenAiTextConfiguration createTextConfiguration() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "reviewed", Map.of("type", "boolean"),
                        "approved", Map.of("type", "boolean"),
                        "violations", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "maxItems", 8
                        ),
                        "correctionPrompt", Map.of("type", "string")
                ),
                "required", List.of(
                        "reviewed",
                        "approved",
                        "violations",
                        "correctionPrompt"
                ),
                "additionalProperties", false
        );

        return new OpenAiTextConfiguration(
                new OpenAiJsonSchemaFormat(
                        "json_schema",
                        "weekly_image_quality_review",
                        "A strict category-compliance review of a generated DAYBIT image.",
                        true,
                        schema
                )
        );
    }

    private String normalizeDetail(String value) {
        if ("high".equalsIgnoreCase(value)) {
            return "high";
        }
        if ("auto".equalsIgnoreCase(value)) {
            return "auto";
        }
        return "low";
    }

    private record OpenAiRequest(
            String model,
            boolean store,
            @JsonProperty("max_output_tokens") int maxOutputTokens,
            String instructions,
            List<Map<String, Object>> input,
            OpenAiTextConfiguration text
    ) {
    }

    private record OpenAiTextConfiguration(
            OpenAiJsonSchemaFormat format
    ) {
    }

    private record OpenAiJsonSchemaFormat(
            String type,
            String name,
            String description,
            boolean strict,
            Map<String, Object> schema
    ) {
    }

    private record OpenAiResponse(
            @JsonProperty("output_text") String outputText,
            List<OpenAiOutput> output
    ) {
    }

    private record OpenAiOutput(List<OpenAiContent> content) {
    }

    private record OpenAiContent(String type, String text) {
    }

    private record ReviewPayload(
            boolean reviewed,
            boolean approved,
            List<String> violations,
            @JsonProperty("correctionPrompt") String correctionPrompt
    ) {
    }
}
