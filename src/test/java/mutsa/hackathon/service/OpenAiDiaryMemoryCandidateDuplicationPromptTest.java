package mutsa.hackathon.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiDiaryMemoryCandidateDeduplicationPromptTest {

    private HttpServer server;

    private OpenAiDiaryMemoryCandidateExtractor
            extractor;

    private final AtomicReference<String>
            capturedRequestBody =
            new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {

        server =
                HttpServer.create(
                        new InetSocketAddress(
                                "127.0.0.1",
                                0
                        ),
                        0
                );

        server.createContext(
                "/responses",
                this::handleRequest
        );

        server.start();

        extractor =
                new OpenAiDiaryMemoryCandidateExtractor(
                        RestClient.builder(),
                        JsonMapper.builder()
                                .build()
                );

        ReflectionTestUtils.setField(
                extractor,
                "apiKey",
                "test-api-key"
        );

        ReflectionTestUtils.setField(
                extractor,
                "model",
                "test-model"
        );

        ReflectionTestUtils.setField(
                extractor,
                "baseUrl",
                "http://127.0.0.1:"
                        + server
                        .getAddress()
                        .getPort()
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void OpenAI_요청에_온보딩직업과_기존기억을_함께_전달한다() {

        extractor.extract(
                new DiaryMemoryExtractionPrompt(
                        "오늘은 학교에 다녀온 뒤 러닝을 했다.",
                        "대학생",
                        """
                        {"stableMemories":[{"category":"HOBBY","text":"러닝을 취미로 즐김"}]}
                        """
                )
        );

        String requestBody =
                capturedRequestBody.get();

        assertTrue(
                requestBody.contains(
                        "onboardingJob: 대학생"
                )
        );

        assertTrue(
                requestBody.contains(
                        "러닝을 취미로 즐김"
                )
        );

        assertTrue(
                requestBody.contains(
                        "IMPORTANT DUPLICATION RULE"
                )
        );

        assertTrue(
                requestBody.contains(
                        "Do not return information already present in onboardingJob"
                )
        );
    }

    private void handleRequest(
            HttpExchange exchange
    ) throws IOException {

        capturedRequestBody.set(
                new String(
                        exchange
                                .getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                )
        );

        byte[] responseBytes =
                """
                {
                  "output_text": "{\\"candidates\\":[]}"
                }
                """
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        exchange
                .getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json;charset=UTF-8"
                );

        exchange.sendResponseHeaders(
                200,
                responseBytes.length
        );

        exchange
                .getResponseBody()
                .write(responseBytes);

        exchange.close();
    }
}