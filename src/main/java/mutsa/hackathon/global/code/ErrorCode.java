package mutsa.hackathon.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode
        implements BaseErrorCode {

    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "COMMON400",
            "요청 값이 올바르지 않습니다."
    ),

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER404",
            "존재하지 않는 유저입니다."
    ),

    ONBOARDING_CONSENT_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "ONBOARDING400_1",
            "온보딩을 완료하려면 AI 기억 활용에 동의해야 합니다."
    ),

    EXPIRED_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH401_1",
            "만료된 토큰입니다."
    ),

    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AUTH401_2",
            "유효하지 않은 토큰입니다."
    ),

    DIARY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "DIARY404",
            "존재하지 않거나 삭제된 일기입니다."
    ),

    DIARY_ALREADY_WRITTEN_TODAY(
            HttpStatus.CONFLICT,
            "DIARY409_1",
            "오늘의 일기는 이미 작성했습니다."
    ),

    DIARY_CONTENT_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "DIARY400_1",
            "일기 내용은 필수입니다."
    ),

    QUESTION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "QUESTION404",
            "존재하지 않는 질문입니다."
    ),

    WRITING_HELP_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "QUESTION429",
            "오늘 사용할 수 있는 작성 도움 질문을 모두 사용했습니다."
    ),

    REFLECTION_ANSWER_ALREADY_SUBMITTED(
            HttpStatus.CONFLICT,
            "QUESTION409_1",
            "성찰 질문 답변은 한 번만 제출할 수 있습니다."
    ),

    INVALID_REFLECTION_ANSWER(
            HttpStatus.BAD_REQUEST,
            "QUESTION400_1",
            "성찰 질문 답변은 공백일 수 없습니다."
    ),

    SHARE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SHARE404",
            "존재하지 않는 공유 요청입니다."
    ),

    SHARE_ALREADY_REQUESTED(
            HttpStatus.CONFLICT,
            "SHARE409_1",
            "이미 공유 요청한 일기입니다."
    ),

    SHARED_DIARY_NOT_AVAILABLE(
            HttpStatus.NOT_FOUND,
            "SHARE404_2",
            "현재 전달할 수 있는 유사 경험 일기가 없습니다."
    ),

    SHARED_DIARY_ALREADY_RECEIVED(
            HttpStatus.CONFLICT,
            "SHARE409_2",
            "이미 전달받은 공유 일기입니다."
    ),

    INSUFFICIENT_CREDIT(
            HttpStatus.CONFLICT,
            "CREDIT409_1",
            "크레딧이 부족합니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}