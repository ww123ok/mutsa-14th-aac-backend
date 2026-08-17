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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiWeeklyVisualPlanGeneratorTest {

    private HttpServer server;
    private JsonMapper jsonMapper;
    private OpenAiWeeklyVisualPlanGenerator generator;
    private final AtomicReference<String> capturedRequest =
            new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        jsonMapper = JsonMapper.builder().build();

        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        server.createContext("/responses", this::handleRequest);
        server.start();

        generator = new OpenAiWeeklyVisualPlanGenerator(
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
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void 이미지생성전에는_카테고리와_모티프만_생성한다()
            throws Exception {
        WeeklyVisualPlan result = generator.generate(context());

        assertEquals(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                result.visualCategory()
        );

        JsonNode request = jsonMapper.readTree(capturedRequest.get());
        JsonNode schema = request.get("text")
                .get("format")
                .get("schema");

        assertTrue(schema.get("properties").has("visualCategory"));
        assertTrue(schema.get("properties").has("visualMotif"));
        assertFalse(schema.get("properties").has("title"));
        assertFalse(schema.get("properties").has("summary"));
        assertFalse(schema.get("properties").has("keywords"));

        String instructions = request.get("instructions").asText();
        assertTrue(instructions.contains(
                "Return exactly these two fields and nothing else"
        ));
        assertTrue(instructions.contains(
                "Do not create a title, summary, keyword list"
        ));
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

    private void handleRequest(HttpExchange exchange)
            throws IOException {
        capturedRequest.set(
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                )
        );

        String motif = String.join(
                " ",
                java.util.Collections.nCopies(80, "visual")
        );
        String payload = jsonMapper.writeValueAsString(
                Map.of(
                        "visualCategory", "GRAPHIC_POSTER",
                        "visualMotif", motif
                )
        );
        String response = jsonMapper.writeValueAsString(
                Map.of("output_text", payload)
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
