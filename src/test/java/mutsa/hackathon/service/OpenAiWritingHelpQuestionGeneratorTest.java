package mutsa.hackathon.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiWritingHelpQuestionGeneratorTest {

    private HttpServer server;

    private OpenAiWritingHelpQuestionGenerator generator;

    private final List<String> capturedRequestBodies =
            new ArrayList<>();

    private final Deque<String> responseBodies =
            new ArrayDeque<>();

    @BeforeEach
    void setUp() throws IOException {
        responseBodies.add(
                response(
                        "카페에서 가장 눈에 들어온 분위기나 인테리어는 어땠나요?"
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
                new OpenAiWritingHelpQuestionGenerator(
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
    void 작성중_본문_모드는_현재본문과_오늘의_이전질문만_전달한다() {
        WritingHelpPrompt prompt =
                new WritingHelpPrompt(
                        WritingHelpQuestionContextType.CURRENT_DRAFT,
                        "카페에 갔다. 창가 자리에 앉았다.",
                        List.of(),
                        2,
                        List.of(
                                "카페에서 누구와 시간을 보냈나요?"
                        )
                );

        String input = generator.buildInput(prompt);

        assertTrue(input.contains("MODE: CURRENT_DRAFT"));
        assertTrue(
                input.contains(
                        "카페에 갔다. 창가 자리에 앉았다."
                )
        );
        assertTrue(
                input.contains(
                        "카페에서 누구와 시간을 보냈나요?"
                )
        );
        assertFalse(input.contains("nickname"));
        assertFalse(input.contains("job"));
        assertFalse(input.contains("approved personalization memory"));
    }

    @Test
    void 작성중_본문의_편집기_타임스탬프는_OpenAI_입력에서_제거한다() {
        WritingHelpPrompt prompt =
                new WritingHelpPrompt(
                        WritingHelpQuestionContextType.CURRENT_DRAFT,
                        "AM 5:11\n친구와 카페에 갔다.\nPM 8:53\n창가에 앉았다.",
                        List.of(),
                        1,
                        List.of()
                );

        String input = generator.buildInput(prompt);

        assertFalse(input.contains("AM 5:11"));
        assertFalse(input.contains("PM 8:53"));
        assertTrue(
                input.contains(
                        "친구와 카페에 갔다.\n창가에 앉았다."
                )
        );
    }

    @Test
    void 최근맥락의_편집기_타임스탬프도_OpenAI_입력에서_제거한다() {
        WritingHelpPrompt prompt =
                new WritingHelpPrompt(
                        WritingHelpQuestionContextType.RECENT_CONTEXT,
                        null,
                        List.of(
                                new WritingHelpRecentDiary(
                                        LocalDate.of(2026, 8, 16),
                                        "오후 8:53\n최근에 운동 루틴을 시작했다."
                                )
                        ),
                        1,
                        List.of()
                );

        String input = generator.buildInput(prompt);

        assertFalse(input.contains("오후 8:53"));
        assertTrue(
                input.contains(
                        "최근에 운동 루틴을 시작했다."
                )
        );
    }

    @Test
    void 최근맥락_모드는_과거날짜와_일기본문을_명시적으로_전달한다() {
        WritingHelpPrompt prompt =
                new WritingHelpPrompt(
                        WritingHelpQuestionContextType.RECENT_CONTEXT,
                        null,
                        List.of(
                                new WritingHelpRecentDiary(
                                        LocalDate.of(2026, 8, 16),
                                        "최근에 카페 아르바이트를 시작했다."
                                )
                        ),
                        1,
                        List.of()
                );

        String input = generator.buildInput(prompt);

        assertTrue(input.contains("MODE: RECENT_CONTEXT"));
        assertTrue(input.contains("priority 1 (FOCAL)"));
        assertTrue(input.contains("recordedDate=2026-08-16"));
        assertTrue(
                input.contains(
                        "최근에 카페 아르바이트를 시작했다."
                )
        );
        assertTrue(
                input.contains(
                        "The Korean word \"오늘\" must not appear"
                )
        );
    }

    @Test
    void 최근맥락_응답에_오늘이_포함되면_한번_재시도해서_교정한다() {
        responseBodies.clear();
        responseBodies.add(
                response(
                        "오늘도 카페 알바는 할 만했나요?"
                )
        );
        responseBodies.add(
                response(
                        "최근 시작한 카페 알바에는 조금씩 적응하고 있나요?"
                )
        );

        String question =
                generator.generate(
                        new WritingHelpPrompt(
                                WritingHelpQuestionContextType.RECENT_CONTEXT,
                                null,
                                List.of(
                                        new WritingHelpRecentDiary(
                                                LocalDate.of(2026, 8, 16),
                                                "최근에 카페 알바를 시작했다."
                                        )
                                ),
                                1,
                                List.of()
                        )
                );

        assertEquals(
                "최근 시작한 카페 알바에는 조금씩 적응하고 있나요?",
                question
        );
        assertEquals(2, capturedRequestBodies.size());
        assertTrue(
                capturedRequestBodies
                        .get(1)
                        .contains(
                                "previous output incorrectly used today-oriented wording"
                        )
        );
    }

    @Test
    void 작성중_본문_응답은_한번만_호출하고_정상화한다() {
        responseBodies.clear();
        responseBodies.add(
                response(
                        "“카페에서 가장 눈에 들어온 건 무엇이었나요.”"
                )
        );

        String question =
                generator.generate(
                        new WritingHelpPrompt(
                                WritingHelpQuestionContextType.CURRENT_DRAFT,
                                "카페에 갔다.",
                                List.of(),
                                1,
                                List.of()
                        )
                );

        assertEquals(
                "카페에서 가장 눈에 들어온 건 무엇이었나요?",
                question
        );
        assertEquals(1, capturedRequestBodies.size());
    }

    @Test
    void 범용질문은_OpenAI_생성기를_직접_호출할수없다() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        generator.generate(
                                new WritingHelpPrompt(
                                        WritingHelpQuestionContextType.GENERIC,
                                        null,
                                        List.of(),
                                        1,
                                        List.of()
                                )
                        )
        );
    }

    @Test
    void API_Key가_없으면_작성도움_전용오류를_반환한다() {
        ReflectionTestUtils.setField(
                generator,
                "apiKey",
                ""
        );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                generator.generate(
                                        new WritingHelpPrompt(
                                                WritingHelpQuestionContextType.CURRENT_DRAFT,
                                                "카페에 갔다.",
                                                List.of(),
                                                1,
                                                List.of()
                                        )
                                )
                );

        assertEquals(
                ErrorCode.AI_WRITING_HELP_UNAVAILABLE,
                exception.getErrorCode()
        );
        assertTrue(capturedRequestBodies.isEmpty());
    }

    private String response(
            String text
    ) {
        return """
                {
                  "output_text": "%s"
                }
                """.formatted(
                text
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
        );
    }

    private void handleRequest(
            HttpExchange exchange
    ) throws IOException {
        String requestBody =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );

        capturedRequestBodies.add(requestBody);

        String body =
                responseBodies.isEmpty()
                        ? response(
                        "질문을 이어서 적어볼까요?"
                )
                        : responseBodies.removeFirst();

        byte[] bytes =
                body.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders()
                .add(
                        "Content-Type",
                        "application/json"
                );
        exchange.sendResponseHeaders(
                200,
                bytes.length
        );
        exchange.getResponseBody()
                .write(bytes);
        exchange.close();
    }
}
