package mutsa.hackathon.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.ErrorCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ApiAccessDeniedHandler
        implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public ApiAccessDeniedHandler(
            JsonMapper jsonMapper
    ) {
        this.jsonMapper =
                jsonMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException
                    accessDeniedException
    ) throws IOException {

        ErrorCode errorCode =
                accessDeniedException
                        instanceof CsrfException
                        ? ErrorCode
                        .CSRF_TOKEN_INVALID
                        : ErrorCode
                        .ACCESS_DENIED;

        response.setStatus(
                errorCode.getStatus()
                        .value()
        );

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8
                        .name()
        );

        ApiResponse<Void> apiResponse =
                ApiResponse.onFailure(
                        errorCode
                );

        response.getWriter()
                .write(
                        jsonMapper
                                .writeValueAsString(
                                        apiResponse
                                )
                );
    }
}