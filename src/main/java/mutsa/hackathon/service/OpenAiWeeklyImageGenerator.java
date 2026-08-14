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

import java.util.Base64;
import java.util.List;

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

    private final RestClient.Builder restClientBuilder;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.weekly-reward.openai.image-model:gpt-image-2}")
    private String model;

    @Value("${app.weekly-reward.openai.image-size:1024x1024}")
    private String size;

    @Value("${app.weekly-reward.openai.image-quality:medium}")
    private String quality;

    @Override
    public GeneratedWeeklyImage generate(
            WeeklyRewardGenerationContext context,
            WeeklyRewardInsight insight
    ) {
        if (context == null || insight == null) {
            throw new IllegalArgumentException(
                    "주간 이미지 생성 정보는 필수입니다."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API Key가 설정되지 않았습니다."
            );
        }

        OpenAiImageRequest request =
                new OpenAiImageRequest(
                        model,
                        buildPrompt(context, insight),
                        1,
                        size,
                        quality,
                        "opaque",
                        "auto",
                        "webp",
                        "daybit-user-" + context.userId()
                );

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
            log.warn(
                    "OpenAI weekly image failed: status={}, model={}",
                    exception.getStatusCode(),
                    model
            );

            throw new IllegalStateException(
                    "OpenAI 주간 이미지 요청에 실패했습니다.",
                    exception
            );

        } catch (RestClientException exception) {
            log.warn(
                    "OpenAI weekly image could not be completed: "
                            + "model={}, reason={}",
                    model,
                    exception.getClass().getSimpleName()
            );

            throw new IllegalStateException(
                    "OpenAI 주간 이미지 요청을 완료하지 못했습니다.",
                    exception
            );
        }
    }

    private String buildPrompt(
            WeeklyRewardGenerationContext context,
            WeeklyRewardInsight insight
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

        return """
                Create one polished weekly reward image for DAYBIT,
                a mobile diary archive.

                SELECTED VISUAL DIRECTION:
                %s

                WEEKLY COLOR PALETTE:
                %s

                WEEKLY KEYWORDS FOR CONTEXT ONLY:
                %s

                GLOBAL REQUIREMENTS:
                - Create one integrated scene or composition.
                - Do not create a collage, calendar, storyboard,
                  or list of separate daily scenes.
                - Use the supplied palette as primary, supporting,
                  and accent colors.
                - The colors do not need equal visual weight.
                - Keep one clear focal composition.
                - Use only places, actions, objects, situations,
                  and visual details supported by the visual direction.
                - Do not invent emotions, relationships, events,
                  symbols, or happy resolutions.
                - If the week contains difficulty, represent it through
                  a safe and restrained everyday scene without frightening,
                  hopeless, or oppressive exaggeration.
                - Do not display a recognizable person
                  or visible human face.
                - Do not imitate Studio Ghibli, any named artist,
                  studio, franchise, copyrighted character,
                  existing movie poster, logo, or brand identity.

                NEGATIVE CONSTRAINTS:
                - No overly lyrical or vague emotional illustration.
                - No monochrome or nearly monochrome composition.
                - No abstract color wash, floating symbolic fragments,
                  empty dream haze, ambiguous surrealism,
                  or unresolved visual metaphor.
                - No grotesque anatomy, horror, blood, violence,
                  self-harm, medical distress, threatening imagery,
                  despair, or oppressive darkness.
                - No glossy AI portrait, synthetic skin,
                  malformed hands, uncanny faces, excessive HDR,
                  plastic lighting, or artificial photo artifacts.
                - No watermark, signature, logo, brand mark,
                  random letters, or unreadable text.
                - Do not render a user interface, calendar,
                  phone frame, color swatch list,
                  or explanation panel.

                FINAL OUTPUT:
                - Produce exactly one cohesive and finished image.
                - Keep the result contemporary,
                  visually intentional, shareable,
                  and appropriate for a wellness diary.
                - Do not force a positive interpretation.
                """.formatted(
                insight.visualMotif(),
                palette,
                String.join(
                        ", ",
                        insight.keywords()
                )
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