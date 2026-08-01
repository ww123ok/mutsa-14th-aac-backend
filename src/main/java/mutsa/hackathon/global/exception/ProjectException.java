package mutsa.hackathon.global.exception;

import lombok.Getter;
import mutsa.hackathon.global.code.BaseErrorCode;
@Getter
public class ProjectException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public ProjectException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
