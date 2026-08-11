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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private final AtomicInteger requestCount =
            new AtomicInteger();

    private final AtomicInteger responseStatus =
            new AtomicInteger();

    private final List<String> responseBodies =
            new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        jsonMapper =
                JsonMapper.builder()
                        .build();

        responseStatus.set(200);
        requestCount.set(0);
        responseBodies.clear();

        setResponses(
                successResponse(
                        "#73d8b4",
                        List.of(
                                "해결",
                                "성취",
                                "안도"
                        )
                )
        );

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
    void 일기내용으로_HEX와_키워드가_포함된_구조화_색보상을_생성한다()
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
                List.of(
                        "해결",
                        "성취",
                        "안도"
                ),
                reward.keywords()
        );

        assertEquals(
                1,
                requestCount.get()
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

        /*
         * 팀에서 사용하는 UI 예약색 중 하나가
         * 실제 prompt에도 포함되어 있는지 확인합니다.
         */
        assertTrue(
                request.get("instructions")
                        .asText()
                        .contains("#414450")
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
                        .has("keywords")
        );

        /*
         * 기존 colorName 계약이 구조화 출력에서
         * 완전히 사라졌는지도 확인합니다.
         */
        assertFalse(
                schema.get("properties")
                        .has("colorName")
        );

        assertEquals(
                "array",
                schema.get("properties")
                        .get("keywords")
                        .get("type")
                        .asText()
        );
    }

    @Test
    void 키워드의_해시태그와_공백은_서버정책에_따라_정규화된다() {
        setResponses(
                successResponse(
                        "#D99A7A",
                        List.of(
                                "#새벽비",
                                "팀 프로젝트",
                                "따뜻한"
                        )
                )
        );

        DiaryColorReward reward =
                generator.generate(
                        "새벽에 비가 왔고 팀 프로젝트에 집중했다."
                );

        assertEquals(
                List.of(
                        "새벽비",
                        "팀프로젝트",
                        "따뜻한"
                ),
                reward.keywords()
        );
    }

    @Test
    void UI_예약색을_반환하면_한번_재생성하고_두번째_정상색을_사용한다() {
        setResponses(
                successResponse(
                        "#414450",
                        List.of("집중")
                ),
                successResponse(
                        "#D99A7A",
                        List.of(
                                "집중",
                                "안도"
                        )
                )
        );

        DiaryColorReward reward =
                generator.generate(
                        "오늘은 오래 집중한 뒤 일을 마무리했다."
                );

        assertEquals(
                2,
                requestCount.get()
        );

        assertEquals(
                "#D99A7A",
                reward.colorHex()
        );

        assertEquals(
                List.of(
                        "집중",
                        "안도"
                ),
                reward.keywords()
        );

        /*
         * 두 번째 요청에는 이전 정책 위반 사실을
         * 명시하는 재생성 안내가 포함됩니다.
         */
        assertTrue(
                capturedRequestBody.get()
                        .contains(
                                "previous attempt violated a hard DAYBIT reward policy"
                        )
        );
    }

    @Test
    void 정책위반_결과를_두번_반환하면_최종적으로_실패한다() {
        setResponses(
                successResponse(
                        "#FFFFFF",
                        List.of("차분한")
                ),
                successResponse(
                        "#F6F8FA",
                        List.of("차분한")
                )
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                "오늘은 조용히 하루를 보냈다."
                        )
        );

        assertEquals(
                2,
                requestCount.get()
        );
    }

    @Test
    void 직접적인_부정감정_라벨도_정책위반으로_보고_재생성한다() {
        setResponses(
                successResponse(
                        "#D99A7A",
                        List.of(
                                "우울한하루"
                        )
                ),
                successResponse(
                        "#C58A73",
                        List.of(
                                "흐린날",
                                "생각"
                        )
                )
        );

        DiaryColorReward reward =
                generator.generate(
                        "비가 와서 집에서 조용히 생각을 정리했다."
                );

        assertEquals(
                2,
                requestCount.get()
        );

        assertEquals(
                List.of(
                        "흐린날",
                        "생각"
                ),
                reward.keywords()
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

        assertEquals(
                0,
                requestCount.get()
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

        assertEquals(
                0,
                requestCount.get()
        );
    }

    @Test
    void 잘못된_색상코드는_정책위반으로_두번_시도한_뒤_실패한다() {
        setResponses(
                successResponse(
                        "mint",
                        List.of("편안함")
                )
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                "오늘은 편안한 하루였다."
                        )
        );

        assertEquals(
                2,
                requestCount.get()
        );
    }

    @Test
    void 키워드가_4개이면_정책위반으로_두번_시도한_뒤_실패한다() {
        setResponses(
                successResponse(
                        "#73D8B4",
                        List.of(
                                "하나",
                                "둘",
                                "셋",
                                "넷"
                        )
                )
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                "오늘 하루를 기록했다."
                        )
        );

        assertEquals(
                2,
                requestCount.get()
        );
    }

    @Test
    void 사용할_수_없는_JSON은_거부한다() {
        setResponses(
                """
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
                """
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                "오늘 하루를 기록했다."
                        )
        );

        /*
         * JSON 자체가 잘못된 경우는 정책 위반이 아니라
         * 응답 자체가 잘못된 것이므로 재시도하지 않습니다.
         */
        assertEquals(
                1,
                requestCount.get()
        );
    }

    @Test
    void 빈_OpenAI_응답은_거부한다() {
        setResponses(
                """
                {
                  "output": []
                }
                """
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                "오늘 하루를 기록했다."
                        )
        );

        assertEquals(
                1,
                requestCount.get()
        );
    }

    @Test
    void OpenAI_HTTP_오류가_발생하면_재시도하지_않고_예외가_발생한다() {
        responseStatus.set(500);

        setResponses(
                """
                {
                  "error": {
                    "message": "temporary error"
                  }
                }
                """
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        generator.generate(
                                "오늘 하루를 기록했다."
                        )
        );

        assertEquals(
                1,
                requestCount.get()
        );
    }

    private String successResponse(
            String colorHex,
            List<String> keywords
    ) {
        try {
            String payload =
                    jsonMapper.writeValueAsString(
                            java.util.Map.of(
                                    "colorHex",
                                    colorHex,
                                    "keywords",
                                    keywords
                            )
                    );

            /*
             * Responses API 응답 안의 output_text에
             * JSON 문자열을 안전하게 넣기 위한 escaping입니다.
             */
            String escapedPayload =
                    jsonMapper.writeValueAsString(
                            payload
                    );

            return """
                    {
                      "output": [
                        {
                          "type": "message",
                          "content": [
                            {
                              "type": "output_text",
                              "text": %s
                            }
                          ]
                        }
                      ]
                    }
                    """.formatted(
                    escapedPayload
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "테스트 응답 JSON 생성에 실패했습니다.",
                    exception
            );
        }
    }

    private void setResponses(
            String... bodies
    ) {
        responseBodies.clear();

        responseBodies.addAll(
                List.of(bodies)
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
                        .getFirst(
                                "Authorization"
                        )
        );

        capturedRequestBody.set(
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                )
        );

        int currentRequestIndex =
                requestCount.getAndIncrement();

        String body =
                responseBodies.get(
                        Math.min(
                                currentRequestIndex,
                                responseBodies.size() - 1
                        )
                );

        byte[] responseBytes =
                body.getBytes(
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