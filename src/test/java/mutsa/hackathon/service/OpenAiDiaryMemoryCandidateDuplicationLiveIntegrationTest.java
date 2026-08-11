package mutsa.hackathon.service;

import mutsa.hackathon.domain.UserMemoryCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = {
                "app.openai.memory-extraction-enabled=true"
        }
)
@EnabledIfEnvironmentVariable(
        named = "OPENAI_LIVE_TEST",
        matches = "true"
)
class OpenAiDiaryMemoryCandidateDeduplicationLiveIntegrationTest {

    @Autowired
    private DiaryMemoryCandidateExtractor
            diaryMemoryCandidateExtractor;

    @Test
    void 실제_OpenAI가_이미알고있는_사실을_반복하지_않고_새로운_맥락을_우선추출한다() {

        assertInstanceOf(
                OpenAiDiaryMemoryCandidateExtractor.class,
                diaryMemoryCandidateExtractor
        );

        List<DiaryMemoryCandidate> candidates =
                diaryMemoryCandidateExtractor.extract(
                        new DiaryMemoryExtractionPrompt(
                                """
                                나는 대학생이고 집에서는 반려묘와 함께 지낸다.
                                주말에는 러닝하는 것을 좋아한다.
                                요즘은 팀 프로젝트 마감을 준비하고 있다.
                                """,
                                "대학생",
                                """
                                {
                                  "schemaVersion": 1,
                                  "stableMemories": [
                                    {
                                      "category": "PET",
                                      "text": "반려묘와 함께 생활함"
                                    },
                                    {
                                      "category": "HOBBY",
                                      "text": "러닝을 취미로 즐김"
                                    }
                                  ],
                                  "ongoingTopics": []
                                }
                                """
                        )
                );

        assertFalse(
                candidates.stream()
                        .anyMatch(candidate ->
                                candidate.category()
                                        == UserMemoryCategory.WORK_STUDY
                                        && candidate.memoryText()
                                        .contains("대학생")
                        )
        );

        assertFalse(
                candidates.stream()
                        .anyMatch(candidate ->
                                candidate.memoryText()
                                        .equals("반려묘와 함께 생활함")
                        )
        );

        assertFalse(
                candidates.stream()
                        .anyMatch(candidate ->
                                candidate.memoryText()
                                        .equals("러닝을 취미로 즐김")
                        )
        );

        assertTrue(
                candidates.stream()
                        .anyMatch(candidate ->
                                candidate.category()
                                        == UserMemoryCategory.ONGOING_TOPIC
                                        || candidate.category()
                                        == UserMemoryCategory.CONCERN
                        )
        );

        System.out.println(
                "[OpenAI 실호출 결과 - 개인화 중복 방지]"
        );

        for (
                DiaryMemoryCandidate candidate
                : candidates
        ) {
            System.out.println(
                    candidate.category()
                            + " -> "
                            + candidate.memoryText()
            );
        }
    }
}