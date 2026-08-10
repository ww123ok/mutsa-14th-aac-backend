package mutsa.hackathon.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiDiaryReflectionQuestionGeneratorTest {

    private HttpServer server;

    private OpenAiDiaryReflectionQuestionGenerator
            generator;

    private final AtomicReference<String>
            capturedRequestBody =
            new AtomicReference<>();

    private final AtomicReference<String>
            capturedAuthorization =
            new AtomicReference<>();

    private final AtomicReference<String>
            capturedMethod =
            new AtomicReference<>();

    private final AtomicReference<String>
            responseBody =
            new AtomicReference<>();

    private final AtomicInteger
            responseStatus =
            new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {

        responseStatus.set(200);

        responseBody.set("""
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "오늘 가장 오래 마음에 남은 순간은 무엇이었나요?"
                        }
                      ]
                    }
                  ]
                }
                """);

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

        generator =
                new OpenAiDiaryReflectionQuestionGenerator(
                        RestClient.builder()
                );

        ReflectionTestUtils.setField(
                generator,
                "apiKey",
                "test-api-key"
        );

        ReflectionTestUtils.setField(
                generator,
                "model",
                "test-model"
        );

        ReflectionTestUtils.setField(
                generator,
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
    void 성찰질문은_항상_오늘_일기내용을_OpenAI에_전달한다() {

        String question =
                generator.generate(
                        new DiaryReflectionPrompt(
                                "오늘 팀원들과 프로젝트의 오류를 해결했다."
                        )
                );

        assertEquals(
                "오늘 가장 오래 마음에 남은 순간은 무엇이었나요?",
                question
        );

        assertEquals(
                "POST",
                capturedMethod.get()
        );

        assertEquals(
                "Bearer test-api-key",
                capturedAuthorization.get()
        );

        String requestBody =
                capturedRequestBody.get();

        assertTrue(
                requestBody.contains(
                        "오늘 팀원들과 프로젝트의 오류를 해결했다."
                )
        );

        assertTrue(
                requestBody.contains(
                        "\"store\":false"
                )
        );

        assertTrue(
                requestBody.contains(
                        "\"max_output_tokens\":300"
                )
        );

        assertTrue(
                requestBody.contains(
                        "\"model\":\"test-model\""
                )
        );
    }

    @Test
    void OpenAI_응답의_불필요한_따옴표와_줄바꿈을_정리한다() {

        responseBody.set("""
                {
                  "output_text": "“오늘 가장 고마웠던 순간은\\n무엇이었나요?”"
                }
                """);

        String question =
                generator.generate(
                        new DiaryReflectionPrompt(
                                "오늘 고마운 일이 있었다."
                        )
                );

        assertEquals(
                "오늘 가장 고마웠던 순간은 무엇이었나요?",
                question
        );
    }

    @Test
    void API_Key가_없으면_외부요청을_시도하지_않는다() {

        ReflectionTestUtils.setField(
                generator,
                "apiKey",
                ""
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                new DiaryReflectionPrompt(
                                        "오늘 하루를 기록했다."
                                )
                        )
        );

        assertEquals(
                null,
                capturedRequestBody.get()
        );
    }

    @Test
    void OpenAI가_사용할수없는_응답을_보내면_예외가_발생한다() {

        responseBody.set("""
                {
                  "output": []
                }
                """);

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                new DiaryReflectionPrompt(
                                        "오늘은 러닝을 했다."
                                )
                        )
        );
    }

    @Test
    void OpenAI_HTTP_오류가_발생하면_예외가_발생한다() {

        responseStatus.set(500);

        responseBody.set("""
                {
                  "error": {
                    "message": "temporary error"
                  }
                }
                """);

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                new DiaryReflectionPrompt(
                                        "오늘 하루를 기록했다."
                                )
                        )
        );
    }

    private void handleRequest(
            HttpExchange exchange
    ) throws IOException {

        capturedMethod.set(
                exchange.getRequestMethod()
        );

        capturedAuthorization.set(
                exchange
                        .getRequestHeaders()
                        .getFirst(
                                "Authorization"
                        )
        );

        capturedRequestBody.set(
                new String(
                        exchange
                                .getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                )
        );

        byte[] responseBytes =
                responseBody
                        .get()
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
                responseStatus.get(),
                responseBytes.length
        );

        exchange
                .getResponseBody()
                .write(
                        responseBytes
                );

        exchange.close();
    }
}