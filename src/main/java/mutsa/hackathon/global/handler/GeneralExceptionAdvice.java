package mutsa.hackathon.global.handler;

import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.global.ApiResponse;
import mutsa.hackathon.global.exception.ProjectException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {
    @ExceptionHandler(ProjectException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectException(ProjectException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.onFailure(e.getErrorCode(), e.getMessage()));
    }
}
