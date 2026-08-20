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
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiWeeklyImageGeneratorRetryTest {

    private HttpServer server;
    private OpenAiWeeklyImageGenerator generator;
    private final AtomicInteger requestCount = new AtomicInteger();
    private volatile int firstStatus;

    @BeforeEach
    void setUp() throws IOException {
        OpenAiWeeklyImageQualityValidator validator = mock(
                OpenAiWeeklyImageQualityValidator.class
        );
        when(validator.review(any(), any(), any(), any()))
                .thenReturn(WeeklyImageQualityReview.skipped());

        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        server.createContext("/images/generations", this::handleRequest);
        server.start();

        generator = new OpenAiWeeklyImageGenerator(
                RestClient.builder(),
                validator
        );
        ReflectionTestUtils.setField(generator, "apiKey", "test-key");
        ReflectionTestUtils.setField(generator, "model", "gpt-image-2");
        ReflectionTestUtils.setField(
                generator,
                "baseUrl",
                "http://127.0.0.1:" + server.getAddress().getPort()
        );
        ReflectionTestUtils.setField(generator, "squareSize", "1024x1024");
        ReflectionTestUtils.setField(generator, "portraitSize", "1024x1536");
        ReflectionTestUtils.setField(generator, "landscapeSize", "1536x1024");
        ReflectionTestUtils.setField(generator, "quality", "medium");
        ReflectionTestUtils.setField(generator, "maxGenerationAttempts", 1);
        ReflectionTestUtils.setField(generator, "strictValidation", true);
        ReflectionTestUtils.setField(generator, "maxRequestAttempts", 3);
        ReflectionTestUtils.setField(generator, "requestBaseBackoffMillis", 0L);
        ReflectionTestUtils.setField(generator, "requestMaxBackoffMillis", 0L);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void 이미지API_429는_같은_프롬프트로_재시도하고_성공한다() {
        firstStatus = 429;

        GeneratedWeeklyImage image = generator.generate(
                context(),
                visualPlan()
        );

        assertEquals(2, requestCount.get());
        assertArrayEquals(new byte[]{1, 2, 3}, image.bytes());
    }

    @Test
    void 이미지API_5xx는_재시도하고_성공한다() {
        firstStatus = 503;

        GeneratedWeeklyImage image = generator.generate(
                context(),
                visualPlan()
        );

        assertEquals(2, requestCount.get());
        assertArrayEquals(new byte[]{1, 2, 3}, image.bytes());
    }

    @Test
    void 이미지API_사용자오류_400은_맹목적으로_재시도하지_않는다() {
        firstStatus = 400;

        assertThrows(
                IllegalStateException.class,
                () -> generator.generate(context(), visualPlan())
        );
        assertEquals(1, requestCount.get());
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
                                "카페와 학교를 오가며 발표 준비를 했다.",
                                "#6A8FB3",
                                List.of("발표")
                        ),
                        new WeeklyRewardGenerationContext.DayRecord(
                                LocalDate.of(2026, 8, 7),
                                "집에서 발표 자료를 수정했다.",
                                "#C9878A",
                                List.of("수정")
                        )
                )
        );
    }

    private WeeklyVisualPlan visualPlan() {
        return new WeeklyVisualPlan(
                WeeklyVisualCategory.PIXEL_ART,
                String.join(
                        " ",
                        java.util.Collections.nCopies(80, "visual")
                )
        );
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        int current = requestCount.incrementAndGet();

        if (current == 1) {
            String errorBody = firstStatus == 400
                    ? "{\"error\":{\"type\":\"image_generation_user_error\",\"code\":\"moderation_blocked\"}}"
                    : "{\"error\":{\"type\":\"rate_limit_error\",\"code\":\"rate_limit_exceeded\"}}";
            byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(
                    "Content-Type",
                    "application/json"
            );
            exchange.getResponseHeaders().add("x-request-id", "req-test-1");
            exchange.getResponseHeaders().add("Retry-After", "0");
            exchange.sendResponseHeaders(firstStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
            return;
        }

        String encoded = Base64.getEncoder().encodeToString(
                new byte[]{1, 2, 3}
        );
        byte[] bytes = ("{\"data\":[{\"b64_json\":\""
                + encoded
                + "\"}]}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add(
                "Content-Type",
                "application/json"
        );
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
