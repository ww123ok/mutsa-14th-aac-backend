package mutsa.hackathon.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mutsa.hackathon.domain.WeeklyRewardImageSource;
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
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiWeeklyRewardResultTextGeneratorTest {

    private HttpServer server;
    private JsonMapper jsonMapper;
    private OpenAiWeeklyRewardResultTextGenerator generator;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicReference<String> capturedRequest =
            new AtomicReference<>();
    private final List<String> responses =
            new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        jsonMapper = JsonMapper.builder().build();
        requestCount.set(0);
        responses.clear();
        responses.add(successResponse(
                "작업과 산책이 이어진 한 주",
                "이번 주 기록에는 팀 작업을 정리하고 저녁에 동네를 걸은 내용이 담겼습니다. "
                        + "이미지에는 작업의 구조적인 흐름과 산책에서 본 저녁빛이 주요 형태와 색으로 반영되었습니다.",
                List.of("작업 정리", "저녁 산책")
        ));

        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        server.createContext("/responses", this::handleRequest);
        server.start();

        generator = new OpenAiWeeklyRewardResultTextGenerator(
                RestClient.builder(),
                jsonMapper
        );

        ReflectionTestUtils.setField(generator, "apiKey", "test-key");
        ReflectionTestUtils.setField(generator, "model", "test-model");
        ReflectionTestUtils.setField(
                generator,
                "baseUrl",
                "http://127.0.0.1:" + server.getAddress().getPort()
        );
        ReflectionTestUtils.setField(generator, "imageDetail", "low");
        ReflectionTestUtils.setField(generator, "maxAttempts", 2);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void 최종이미지와_일기내용으로_생성후_한국어결과문구를_만든다()
            throws Exception {
        WeeklyRewardResultText result = generator.generate(
                context(),
                visualPlan(),
                image()
        );

        assertEquals("작업과 산책이 이어진 한 주", result.title());
        assertEquals(List.of("작업 정리", "저녁 산책"), result.keywords());
        assertEquals(1, requestCount.get());

        JsonNode request = jsonMapper.readTree(capturedRequest.get());
        assertEquals("test-model", request.get("model").asText());
        assertFalse(request.get("store").asBoolean());

        JsonNode content = request.get("input")
                .get(0)
                .get("content");

        assertEquals("input_text", content.get(0).get("type").asText());
        assertTrue(content.get(0).get("text").asText().contains("팀 작업을 정리했다"));
        assertEquals("input_image", content.get(1).get("type").asText());
        assertTrue(content.get(1).get("image_url").asText()
                .startsWith("data:image/webp;base64,"));
        assertEquals("low", content.get(1).get("detail").asText());

        String instructions =
                request.get("instructions").asText();

        String normalizedInstructions =
                instructions.replaceAll("\\s+", " ");

        assertTrue(normalizedInstructions.contains(
                "exactly two or three short, complete Korean sentences"
        ));
        assertTrue(normalizedInstructions.contains(
                "one to three concise Korean context keywords without #"
        ));
        assertTrue(normalizedInstructions.contains(
                "Explain why this final image represents the whole week"
        ));
        assertTrue(normalizedInstructions.contains(
                "actually reflected in the final image"
        ));
        JsonNode schema = request.get("text")
                .get("format")
                .get("schema");
        assertTrue(schema.get("properties").has("title"));
        assertTrue(schema.get("properties").has("summary"));
        assertTrue(schema.get("properties").has("keywords"));
        assertFalse(schema.get("properties").has("visualCategory"));
        assertFalse(schema.get("properties").has("visualMotif"));
    }

    @Test
    void 요약이_한문장이면_재요청한뒤_두문장을_사용한다() {
        responses.clear();
        responses.add(successResponse(
                "한 주의 기록",
                "이번 주 기록에는 여러 활동이 담겼습니다.",
                List.of("주간 기록")
        ));
        responses.add(successResponse(
                "한 주의 기록",
                "이번 주 기록에는 작업과 산책에 관한 내용이 담겼습니다. "
                        + "이미지에는 두 활동의 흐름이 주요 형태와 저녁빛 색으로 반영되었습니다.",
                List.of("주간 기록", "작업과 산책")
        ));

        WeeklyRewardResultText result = generator.generate(
                context(),
                visualPlan(),
                image()
        );

        assertEquals(2, requestCount.get());
        assertTrue(result.summary().contains("작업과 산책"));
    }

    private WeeklyRewardGenerationContext context() {
        return new WeeklyRewardGenerationContext(
                10L,
                20L,
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 9),
                List.of(
                        new WeeklyRewardGenerationContext.DayRecord(
                                LocalDate.of(2026, 8, 3),
                                "팀 작업을 정리했다.",
                                "#D6A45C",
                                List.of("작업")
                        ),
                        new WeeklyRewardGenerationContext.DayRecord(
                                LocalDate.of(2026, 8, 5),
                                "저녁에 동네를 걸었다.",
                                "#6A8FB3",
                                List.of("산책")
                        ),
                        new WeeklyRewardGenerationContext.DayRecord(
                                LocalDate.of(2026, 8, 7),
                                "집에서 쉬었다.",
                                "#C9878A",
                                List.of("휴식")
                        )
                )
        );
    }

    private WeeklyVisualPlan visualPlan() {
        String motif = String.join(
                " ",
                java.util.Collections.nCopies(80, "visual")
        );

        return new WeeklyVisualPlan(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                motif
        );
    }

    private GeneratedWeeklyImage image() {
        return new GeneratedWeeklyImage(
                new byte[]{1, 2, 3, 4},
                "image/webp",
                "webp",
                WeeklyRewardImageSource.AI
        );
    }

    private String successResponse(
            String title,
            String summary,
            List<String> keywords
    ) {
        try {
            String payload = jsonMapper.writeValueAsString(
                    java.util.Map.of(
                            "title", title,
                            "summary", summary,
                            "keywords", keywords
                    )
            );

            return jsonMapper.writeValueAsString(
                    java.util.Map.of("output_text", payload)
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void handleRequest(HttpExchange exchange)
            throws IOException {
        capturedRequest.set(
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                )
        );

        int index = requestCount.getAndIncrement();
        String response = responses.get(
                Math.min(index, responses.size() - 1)
        );
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add(
                "Content-Type",
                "application/json"
        );
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
