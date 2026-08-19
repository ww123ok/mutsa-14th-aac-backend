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
            You are the visual-quality gate for DAYBIT weekly reward images.

            Review the supplied generated image against:
            1. the selected category,
            2. the expected canvas orientation,
            3. the explicit HARD CONSTRAINTS and clear prohibitions in the generation brief.

            The generation brief also contains visual guidance, preferences, examples, and approximate targets.
            Treat those as strong creative direction, not automatic failure conditions.

            Approve an image when it clearly belongs to the selected category, preserves the core approved visual motif,
            uses the expected orientation, and has no clear material hard-rule violation.

            Reject only clear and material failures such as:
            - the wrong visual category or clearly wrong orientation,
            - recognizable human faces when prohibited,
            - a human or humanoid subject in NON_HUMAN_CHARACTER,
            - major unsupported clothing, props, events, people, brands, or identifying information,
            - dashboards, software UI, charts, checklists, or readable explanatory copy where prohibited,
            - severe malformed anatomy, broken objects, meaningless pseudo-text, or major visual artifacts,
            - loss of the core diary-derived visual identity required by the generation brief.

            Do NOT reject solely because:
            - a preferred composition, texture, material, camera angle, or styling choice was interpreted differently,
            - an approximate target such as proportion, prop count, or pose is not exact,
            - one secondary motif cue is absent while the core diary-derived identity remains clear,
            - a weekly-palette color is not an exact pixel-level hexadecimal match,
            - an optional or preference word such as "prefer", "approximately", "may", "can", or "when practical" was not followed,
            - the image differs from a subjective aesthetic preference while remaining coherent and category-correct.

            For NON_HUMAN_CHARACTER:
            - a clearly visible ANIMAL face is allowed and often desirable;
            - the recognizable-human-face privacy rule does not apply to the animal;
            - do not require an exact frontal pose or exact head-to-body ratio;
            - intentional stylized animal anatomy is acceptable when coherent and clearly non-human.

            For GRAPHIC_POSTER:
            - typography may appear as graphic material;
            - reject readable explanatory copy, fake UI text, checklist text, dashboards, or software-interface layouts;
            - supporting geometry is allowed when it does not replace the diary-derived visual identity.

            Treat palette requirements perceptually rather than as exact pixel-level color matching.

            Return:
            - reviewed: always true
            - approved: true when there is no clear material hard-rule violation
            - violations: short English descriptions of clear material failures, at most eight
            - correctionPrompt: concise English imperatives correcting only those failures

            The correction prompt must preserve the selected category, expected orientation, weekly palette direction,
            and approved visual motif. Never introduce a new event, place, person, brand, artist, franchise, or story.
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
            String expectedSize,
            String generationPrompt
    ) {
        if (!validationEnabled) {
            return WeeklyImageQualityReview.skipped();
        }
        if (
                image == null
                        || category == null
                        || generationPrompt == null
                        || generationPrompt.isBlank()
        ) {
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

        String reviewInput = buildReviewInput(
                category,
                expectedSize,
                generationPrompt
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

    static String buildReviewInput(
            WeeklyVisualCategory category,
            String expectedSize,
            String generationPrompt
    ) {
        return """
                SELECTED CATEGORY: %s
                EXPECTED IMAGE SIZE: %s
                EXPECTED ORIENTATION: %s

                GENERATION BRIEF:
                %s

                Inspect the image now. Extract only explicit HARD CONSTRAINTS and clear prohibitions as rejection criteria.
                Treat the remaining creative directions as guidance rather than automatic failure conditions.
                """.formatted(
                category.name(),
                expectedSize,
                category.imageAspect().name(),
                generationPrompt
        );
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
