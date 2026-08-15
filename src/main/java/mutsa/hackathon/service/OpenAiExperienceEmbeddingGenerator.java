package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiExperienceEmbeddingGenerator implements ExperienceEmbeddingGenerator {

    private final RestClient.Builder restClientBuilder;
    private final JsonMapper jsonMapper;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.embedding-model:text-embedding-3-small}")
    private String model;

    @Value("${app.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Override
    public ExperienceEmbedding generate(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Embedding text is required.");
        }

        try {
            String responseBody = restClientBuilder.baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build().post().uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", model, "input", text.trim()))
                    .retrieve().body(String.class);
            Map<?, ?> response = jsonMapper.readValue(responseBody, Map.class);
            Object data = response.get("data");
            if (!(data instanceof List<?> rows) || rows.isEmpty()
                    || !(rows.get(0) instanceof Map<?, ?> row)
                    || !(row.get("embedding") instanceof List<?> values)) {
                throw new IllegalStateException("OpenAI embedding response was invalid.");
            }
            List<Double> vector = values.stream()
                    .filter(Number.class::isInstance)
                    .map(Number.class::cast)
                    .map(Number::doubleValue)
                    .toList();
            if (vector.isEmpty() || vector.size() != values.size()) {
                throw new IllegalStateException("OpenAI embedding response was invalid.");
            }
            return new ExperienceEmbedding(model, vector);
        } catch (Exception exception) {
            log.warn("Experience embedding generation failed: model={}, reason={}",
                    model, exception.getClass().getSimpleName());
            throw new IllegalStateException("Experience embedding generation failed.", exception);
        }
    }
}
