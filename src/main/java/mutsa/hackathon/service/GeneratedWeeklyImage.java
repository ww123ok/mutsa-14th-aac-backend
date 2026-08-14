package mutsa.hackathon.service;

import mutsa.hackathon.domain.WeeklyRewardImageSource;

public record GeneratedWeeklyImage(
        byte[] bytes,
        String contentType,
        String fileExtension,
        WeeklyRewardImageSource source
) {
    public GeneratedWeeklyImage {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("생성된 이미지 데이터는 필수입니다.");
        }
        bytes = bytes.clone();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("이미지 Content-Type은 필수입니다.");
        }
        if (fileExtension == null || !fileExtension.matches("[a-z0-9]{3,5}")) {
            throw new IllegalArgumentException("이미지 확장자가 올바르지 않습니다.");
        }
        if (source == null) {
            throw new IllegalArgumentException("이미지 생성 출처는 필수입니다.");
        }
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}