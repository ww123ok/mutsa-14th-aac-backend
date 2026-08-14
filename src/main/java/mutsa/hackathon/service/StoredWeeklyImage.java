package mutsa.hackathon.service;

public record StoredWeeklyImage(
        String key,
        String contentType
) {
    public StoredWeeklyImage {
        if (key == null || key.isBlank() || contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("저장된 이미지 정보가 올바르지 않습니다.");
        }
    }
}