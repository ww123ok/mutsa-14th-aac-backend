package mutsa.hackathon.dto;

import jakarta.validation.constraints.Size;

public record WritingHelpQuestionRequest(

        @Size(
                max = 10000,
                message = "작성 중인 일기 내용은 10000자 이하로 입력해야 합니다."
        )
        String currentContent

) {

    public boolean hasCurrentContent() {
        return currentContent != null
                && !currentContent.isBlank();
    }

    public String normalizedCurrentContent() {
        return hasCurrentContent()
                ? currentContent.trim()
                : null;
    }
}
