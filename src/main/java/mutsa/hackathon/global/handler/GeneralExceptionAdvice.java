package mutsa.hackathon.global.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {

    @ExceptionHandler(ProjectException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleProjectException(
            ProjectException exception
    ) {
        return ResponseEntity
                .status(
                        exception
                                .getErrorCode()
                                .getStatus()
                )
                .body(
                        ApiResponse.onFailure(
                                exception.getErrorCode(),
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(
                        DefaultMessageSourceResolvable
                                ::getDefaultMessage
                )
                .orElse(
                        ErrorCode.INVALID_REQUEST
                                .getMessage()
                );

        return badRequest(message);
    }

    @ExceptionHandler(
            ConstraintViolationException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        String message = exception
                .getConstraintViolations()
                .stream()
                .findFirst()
                .map(violation ->
                        violation.getMessage()
                )
                .orElse(
                        ErrorCode.INVALID_REQUEST
                                .getMessage()
                );

        return badRequest(message);
    }

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleUnreadableMessage(
            HttpMessageNotReadableException exception
    ) {
        return badRequest(
                "요청 본문의 JSON 형식 또는 시간 형식이 올바르지 않습니다."
        );
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return badRequest(
                exception.getMessage()
        );
    }

    private ResponseEntity<ApiResponse<Void>>
    badRequest(String message) {
        return ResponseEntity
                .status(
                        ErrorCode.INVALID_REQUEST
                                .getStatus()
                )
                .body(
                        ApiResponse.onFailure(
                                ErrorCode.INVALID_REQUEST,
                                message
                        )
                );
    }
}