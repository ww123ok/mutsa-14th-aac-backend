package mutsa.hackathon.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiDiaryColorRewardGeneratorTest {

    private HttpServer server;

    private JsonMapper jsonMapper;

    private OpenAiDiaryColorRewardGenerator
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

    private final AtomicInteger responseStatus =
            new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        jsonMapper =
                JsonMapper.builder()
                        .build();

        responseStatus.set(200);

        responseBody.set("""
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\"colorHex\\":\\"#73d8b4\\",\\"colorName\\":\\"포근한 민트빛\\"}"
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
                new OpenAiDiaryColorRewardGenerator(
                        RestClient.builder(),
                        jsonMapper
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
    void 일기내용으로_구조화된_색_보상을_생성한다()
            throws Exception {

        DiaryColorReward reward =
                generator.generate(
                        "오늘 팀원들과 오류를 해결했고 테스트가 성공해서 뿌듯했다."
                );

        assertEquals(
                "#73D8B4",
                reward.colorHex()
        );

        assertEquals(
                "포근한 민트빛",
                reward.colorName()
        );

        assertEquals(
                "POST",
                capturedMethod.get()
        );

        assertEquals(
                "Bearer test-api-key",
                capturedAuthorization.get()
        );

        JsonNode request =
                jsonMapper.readTree(
                        capturedRequestBody.get()
                );

        assertEquals(
                "test-model",
                request.get("model")
                        .asText()
        );

        assertFalse(
                request.get("store")
                        .asBoolean()
        );

        assertEquals(
                300,
                request.get(
                                "max_output_tokens"
                        )
                        .asInt()
        );

        assertTrue(
                request.get("input")
                        .asText()
                        .contains(
                                "오늘 팀원들과 오류를 해결했고 테스트가 성공해서 뿌듯했다."
                        )
        );

        JsonNode format =
                request.get("text")
                        .get("format");

        assertEquals(
                "json_schema",
                format.get("type")
                        .asText()
        );

        assertEquals(
                "diary_color_reward",
                format.get("name")
                        .asText()
        );

        assertTrue(
                format.get("strict")
                        .asBoolean()
        );

        JsonNode schema =
                format.get("schema");

        assertEquals(
                "object",
                schema.get("type")
                        .asText()
        );

        assertFalse(
                schema.get(
                                "additionalProperties"
                        )
                        .asBoolean()
        );

        assertTrue(
                schema.get("properties")
                        .has("colorHex")
        );

        assertTrue(
                schema.get("properties")
                        .has("colorName")
        );
    }

    @Test
    void 빈_일기내용이면_외부요청을_시도하지_않는다() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        generator.generate(
                                "   "
                        )
        );

        assertNull(
                capturedRequestBody.get()
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
                                "오늘 하루를 기록했다."
                        )
        );

        assertNull(
                capturedRequestBody.get()
        );
    }

    @Test
    void 잘못된_색상코드는_거부한다() {
        responseBody.set("""
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\"colorHex\\":\\"mint\\",\\"colorName\\":\\"민트빛\\"}"
                        }
                      ]
                    }
                  ]
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        generator.generate(
                                "오늘은 편안한 하루였다."
                        )
        );
    }

    @Test
    void 지나치게_긴_색상이름은_거부한다() {
        String longColorName =
                "가".repeat(31);

        responseBody.set(
                """
                {
                  "output_text": "{\\"colorHex\\":\\"#73D8B4\\",\\"colorName\\":\\"%s\\"}"
                }
                """.formatted(
                        longColorName
                )
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                "오늘은 편안한 하루였다."
                        )
        );
    }

    @Test
    void 사용할_수_없는_JSON은_거부한다() {
        responseBody.set("""
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "not-json"
                        }
                      ]
                    }
                  ]
                }
                """);

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                "오늘 하루를 기록했다."
                        )
        );
    }

    @Test
    void 빈_OpenAI_응답은_거부한다() {
        responseBody.set("""
                {
                  "output": []
                }
                """);

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                "오늘 하루를 기록했다."
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
                                "오늘 하루를 기록했다."
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
                exchange.getRequestHeaders()
                        .getFirst("Authorization")
        );

        capturedRequestBody.set(
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                )
        );

        byte[] responseBytes =
                responseBody.get()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json;charset=UTF-8"
                );

        exchange.sendResponseHeaders(
                responseStatus.get(),
                responseBytes.length
        );

        exchange.getResponseBody()
                .write(responseBytes);

        exchange.close();
    }
}