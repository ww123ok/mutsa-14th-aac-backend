package mutsa.hackathon.global;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mutsa.hackathon.global.code.BaseErrorCode;
import mutsa.hackathon.global.code.BaseSuccessCode;
import mutsa.hackathon.global.code.SuccessCode;
import mutsa.hackathon.repository.AppUserRepository;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // result가 null이면 필드 자체가 응답에서 빠짐
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    private boolean isSuccess;
    private String message;
    private String code;
    private T result;

    private ApiResponse(boolean isSuccess, String code, String message, T result) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.result = result;
    }

    @JsonProperty("isSuccess")
    public boolean isSuccess() { return isSuccess; }

    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(true, SuccessCode.OK.getCode(), SuccessCode.OK.getMessage(), result);
    }
    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result) {
        return new ApiResponse<>(true, code.getCode(),code.getMessage(), result);
    }
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), null);
    }
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T result) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), result);
    }
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, String message) {
        return new ApiResponse<>(false, code.getCode(), message, null);
    }

}
