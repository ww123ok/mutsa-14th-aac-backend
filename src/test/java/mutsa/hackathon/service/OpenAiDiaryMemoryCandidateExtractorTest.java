package mutsa.hackathon.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mutsa.hackathon.domain.UserMemoryCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiDiaryMemoryCandidateExtractorTest {

    private HttpServer server;

    private JsonMapper jsonMapper;

    private OpenAiDiaryMemoryCandidateExtractor
            extractor;

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
                          "text": "{\\"candidates\\":[{\\"category\\":\\"PET\\",\\"memoryText\\":\\"반려묘와 함께 생활함\\"},{\\"category\\":\\"CONCERN\\",\\"memoryText\\":\\"최근 팀 프로젝트 마감을 준비하고 있음\\"}]}"
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

        extractor =
                new OpenAiDiaryMemoryCandidateExtractor(
                        RestClient.builder(),
                        jsonMapper
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
    void 일기에서_안전한_개인화_기억후보를_구조화해서_추출한다() {

        List<DiaryMemoryCandidate> candidates =
                extractor.extract(
                        """
                        오늘 집에 와서 반려묘와 시간을 보냈다.
                        요즘 팀 프로젝트 마감 준비 때문에 정신이 없다.
                        """
                );

        assertEquals(
                2,
                candidates.size()
        );

        assertEquals(
                UserMemoryCategory.PET,
                candidates.get(0)
                        .category()
        );

        assertEquals(
                "반려묘와 함께 생활함",
                candidates.get(0)
                        .memoryText()
        );

        assertEquals(
                UserMemoryCategory.CONCERN,
                candidates.get(1)
                        .category()
        );

        assertEquals(
                "최근 팀 프로젝트 마감을 준비하고 있음",
                candidates.get(1)
                        .memoryText()
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
                        "오늘 집에 와서 반려묘와 시간을 보냈다."
                )
        );

        assertTrue(
                requestBody.contains(
                        "\"store\":false"
                )
        );

        assertTrue(
                requestBody.contains(
                        "\"model\":\"test-model\""
                )
        );

        assertTrue(
                requestBody.contains(
                        "\"name\":\"diary_memory_candidates\""
                )
        );

        assertTrue(
                requestBody.contains(
                        "\"strict\":true"
                )
        );
    }

    @Test
    void 기억할_내용이_없으면_빈_목록을_반환한다() {

        responseBody.set("""
                {
                  "output_text": "{\\"candidates\\":[]}"
                }
                """);

        List<DiaryMemoryCandidate> candidates =
                extractor.extract(
                        "오늘은 그냥 평범한 하루였다."
                );

        assertTrue(
                candidates.isEmpty()
        );
    }

    @Test
    void 이메일이_그대로_포함된_기억후보는_서버에서_거부한다() {

        responseBody.set("""
                {
                  "output_text": "{\\"candidates\\":[{\\"category\\":\\"RELATIONSHIP\\",\\"memoryText\\":\\"친구의 이메일은 friend@example.com임\\"}]}"
                }
                """);

        assertThrows(
                IllegalStateException.class,
                () ->
                        extractor.extract(
                                "오늘 친구에게 이메일을 보냈다."
                        )
        );
    }

    @Test
    void API_Key가_없으면_외부요청을_시도하지_않는다() {

        ReflectionTestUtils.setField(
                extractor,
                "apiKey",
                ""
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        extractor.extract(
                                "오늘 하루를 기록했다."
                        )
        );

        assertNull(
                capturedRequestBody.get()
        );
    }

    @Test
    void 사용할_수_없는_JSON은_거부한다() {

        responseBody.set("""
                {
                  "output_text": "not-json"
                }
                """);

        assertThrows(
                IllegalStateException.class,
                () ->
                        extractor.extract(
                                "오늘은 러닝을 했다."
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
                        extractor.extract(
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

        exchange
                .getResponseBody()
                .close();
    }
}