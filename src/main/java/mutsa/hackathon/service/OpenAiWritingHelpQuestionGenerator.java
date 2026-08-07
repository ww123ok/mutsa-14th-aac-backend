package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonProperty;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenAiWritingHelpQuestionGenerator
        implements WritingHelpQuestionGenerator {

    private static final String INSTRUCTIONS = """
            You create one Korean diary-writing prompt for a user.
            Return only one short, warm question in Korean, under 80 characters.
            Help the user recall a concrete moment from today. Do not give advice,
            diagnose emotions, mention that you are an AI, or expose profile data.
            Use the supplied profile only when it naturally makes the question more relevant.
            """;

    private final RestClient.Builder restClientBuilder;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.model:gpt-5-mini}")
    private String model;

    @Value("${app.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Override
    public String generate(WritingHelpPrompt prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ProjectException(ErrorCode.AI_WRITING_HELP_UNAVAILABLE);
        }

        try {
            OpenAiResponse response = restClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build()
                    .post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OpenAiRequest(model, false, INSTRUCTIONS, buildInput(prompt)))
                    .retrieve()
                    .body(OpenAiResponse.class);

            return extractQuestion(response);
        } catch (RestClientResponseException exception) {
            log.warn(
                    "OpenAI writing-help request failed: status={}, model={}, body={}",
                    exception.getStatusCode(),
                    model,
                    exception.getResponseBodyAsString()
            );
            throw new ProjectException(ErrorCode.AI_WRITING_HELP_UNAVAILABLE);
        } catch (RestClientException exception) {
            log.warn(
                    "OpenAI writing-help request could not be completed: model={}, reason={}",
                    model,
                    exception.getMessage()
            );
            throw new ProjectException(ErrorCode.AI_WRITING_HELP_UNAVAILABLE);
        }
    }

    private String buildInput(WritingHelpPrompt prompt) {
        String nickname = defaultValue(prompt.nickname(), "사용자");
        String job = defaultValue(prompt.job(), "정보 없음");
        String memoryProfile = defaultValue(prompt.memoryProfile(), "승인된 장기 기억 없음");

        return """
                User profile:
                - nickname: %s
                - job: %s
                - approved memory profile: %s

                Create the next diary-writing question now.
                """.formatted(nickname, job, memoryProfile);
    }

    private String extractQuestion(OpenAiResponse response) {
        if (response == null || response.outputText() == null) {
            throw new ProjectException(ErrorCode.AI_WRITING_HELP_UNAVAILABLE);
        }

        String question = response.outputText().trim();
        if (question.isBlank() || question.length() > 1_000) {
            throw new ProjectException(ErrorCode.AI_WRITING_HELP_UNAVAILABLE);
        }

        return question;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record OpenAiRequest(
            String model,
            boolean store,
            String instructions,
            String input
    ) {
    }

    private record OpenAiResponse(
            @JsonProperty("output_text") String outputText
    ) {
    }
}
