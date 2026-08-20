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
                "작업 노트와 저녁빛 사이",
                "팀 작업을 정리하고 저녁에 동네를 걸은 장면이 기록에 남았습니다. "
                        + "이미지에는 각진 노트 형태와 넓은 색면, 산책길을 떠올리게 하는 부드러운 저녁빛이 겹쳐 배치되었습니다.",
                List.of("조용한", "작업 정리", "저녁 산책", "휴식")
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

        assertEquals("작업 노트와 저녁빛 사이", result.title());
        assertEquals("그래픽 포스터", result.categoryKeyword());
        assertEquals(
                List.of("조용한", "작업 정리", "저녁 산책", "휴식"),
                result.keywords()
        );
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
                "Korean image-caption-like title"
        ));
        assertTrue(normalizedInstructions.contains(
                "three to five concise Korean keywords without #"
        ));
        assertTrue(normalizedInstructions.contains(
                "Do not frame the title as a date range"
        ));
        assertTrue(normalizedInstructions.contains(
                "Use descriptive Korean modifiers naturally"
        ));
        assertTrue(normalizedInstructions.contains(
                "concrete visible details"
        ));
        assertTrue(normalizedInstructions.contains(
                "Do not write system-like production language"
        ));
        JsonNode schema = request.get("text")
                .get("format")
                .get("schema");
        assertTrue(schema.get("properties").has("title"));
        assertTrue(schema.get("properties").has("summary"));
        assertTrue(schema.get("properties").has("keywords"));
        assertEquals(
                3,
                schema.get("properties").get("keywords").get("minItems").asInt()
        );
        assertEquals(
                5,
                schema.get("properties").get("keywords").get("maxItems").asInt()
        );
        assertFalse(schema.get("properties").has("visualCategory"));
        assertFalse(schema.get("properties").has("visualMotif"));
    }

    @Test
    void 픽셀아트와_실사풍경_카테고리키워드를_지원한다() {
        WeeklyRewardResultText pixelArt = generator.generate(
                context(),
                visualPlan(WeeklyVisualCategory.PIXEL_ART),
                image()
        );
        WeeklyRewardResultText photoLandscape = generator.generate(
                context(),
                visualPlan(WeeklyVisualCategory.PHOTO_LANDSCAPE),
                image()
        );

        assertEquals("픽셀아트", pixelArt.categoryKeyword());
        assertEquals("실사 풍경", photoLandscape.categoryKeyword());
    }

    @Test
    void 요약이_한문장이면_재요청한뒤_두문장을_사용한다() {
        responses.clear();
        responses.add(successResponse(
                "작업 노트와 저녁빛 사이",
                "작업을 정리하고 산책한 장면이 기록에 남았습니다.",
                List.of("작업", "산책", "휴식")
        ));
        responses.add(successResponse(
                "작업 노트와 저녁빛 사이",
                "작업을 정리하고 저녁에 산책한 장면이 기록에 남았습니다. "
                        + "이미지에는 각진 노트 형태와 부드러운 저녁빛이 넓은 색면 사이에 구체적으로 배치되었습니다.",
                List.of("작업", "산책", "휴식")
        ));

        WeeklyRewardResultText result = generator.generate(
                context(),
                visualPlan(),
                image()
        );

        assertEquals(2, requestCount.get());
        assertTrue(result.summary().contains("작업을 정리하고"));
    }

    @Test
    void 날짜범위나_한주형_제목이면_재요청한다() {
        responses.clear();
        responses.add(successResponse(
                "8월 3일부터 이어진 한 주",
                "작업을 정리하고 저녁에 산책한 장면이 기록에 남았습니다. "
                        + "이미지에는 각진 노트와 부드러운 저녁빛이 넓은 색면 사이에 배치되었습니다.",
                List.of("작업", "산책", "휴식")
        ));
        responses.add(successResponse(
                "작업 노트와 저녁빛 사이",
                "작업을 정리하고 저녁에 산책한 장면이 기록에 남았습니다. "
                        + "이미지에는 각진 노트와 부드러운 저녁빛이 넓은 색면 사이에 배치되었습니다.",
                List.of("작업", "산책", "휴식")
        ));

        WeeklyRewardResultText result = generator.generate(
                context(),
                visualPlan(),
                image()
        );

        assertEquals(2, requestCount.get());
        assertEquals("작업 노트와 저녁빛 사이", result.title());
    }

    @Test
    void 생성과정형_요약이면_재요청한다() {
        responses.clear();
        responses.add(successResponse(
                "작업 노트와 저녁빛 사이",
                "이번 주 기록에는 작업과 산책이 담겼습니다. "
                        + "이 내용과 각 날의 색을 바탕으로 주간 이미지가 구성되었습니다.",
                List.of("작업", "산책", "휴식")
        ));
        responses.add(successResponse(
                "작업 노트와 저녁빛 사이",
                "작업을 정리하고 저녁에 산책한 장면이 기록에 남았습니다. "
                        + "이미지에는 각진 노트와 부드러운 저녁빛, 넓은 색면이 층을 이루며 배치되었습니다.",
                List.of("작업", "산책", "휴식")
        ));

        WeeklyRewardResultText result = generator.generate(
                context(),
                visualPlan(),
                image()
        );

        assertEquals(2, requestCount.get());
        assertTrue(result.summary().contains("각진 노트"));
        assertFalse(result.summary().contains("주간 이미지가 구성되었습니다"));
    }

    @Test
    void 하단_키워드에_카테고리가_오면_재요청한다() {
        responses.clear();
        responses.add(successResponse(
                "밤거리와 늦은 귀가",
                "친구를 만나고 늦게 귀가한 장면이 기록에 남았습니다. "
                        + "이미지에는 어두운 밤거리와 길게 이어진 이동의 흔적, 작은 불빛이 주요 장면으로 배치되었습니다.",
                List.of("그래픽 포스터", "친구 만남", "밤거리", "늦은 귀가")
        ));
        responses.add(successResponse(
                "밤거리와 늦은 귀가",
                "친구를 만나고 늦게 귀가한 장면이 기록에 남았습니다. "
                        + "이미지에는 어두운 밤거리와 길게 이어진 이동의 흔적, 작은 불빛이 주요 장면으로 배치되었습니다.",
                List.of("친구 만남", "밤거리", "늦은 귀가", "동네 산책")
        ));

        WeeklyRewardResultText result = generator.generate(
                context(),
                visualPlan(WeeklyVisualCategory.NON_HUMAN_CHARACTER),
                image()
        );

        assertEquals(2, requestCount.get());
        assertEquals("3D캐릭터", result.categoryKeyword());
        assertFalse(result.keywords().contains("그래픽 포스터"));
        assertEquals(
                List.of("친구 만남", "밤거리", "늦은 귀가", "동네 산책"),
                result.keywords()
        );
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
        return visualPlan(WeeklyVisualCategory.GRAPHIC_POSTER);
    }

    private WeeklyVisualPlan visualPlan(
            WeeklyVisualCategory category
    ) {
        String motif = String.join(
                " ",
                java.util.Collections.nCopies(80, "visual")
        );

        return new WeeklyVisualPlan(
                category,
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
