package mutsa.hackathon.service;

import mutsa.hackathon.domain.UserMemoryCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class OpenAiDiaryMemoryCandidateExtractorLiveIntegrationTest {

    @Autowired
    private DiaryMemoryCandidateExtractor
            diaryMemoryCandidateExtractor;

    @Test
    void 실제_OpenAI가_일기에서_안전한_개인화기억을_추출한다() {

        assertInstanceOf(
                OpenAiDiaryMemoryCandidateExtractor.class,
                diaryMemoryCandidateExtractor
        );

        List<DiaryMemoryCandidate> candidates =
                diaryMemoryCandidateExtractor.extract(
                        """
                        나는 대학생이고 요즘 팀 프로젝트 마감을 준비하고 있다.
                        프로젝트 때문에 조금 신경이 쓰이지만
                        집에 돌아오면 키우는 고양이와 시간을 보내며 쉰다.
                        주말에는 러닝하는 것을 좋아해서 자주 달린다.
                        """
                );

        assertNotNull(
                candidates
        );

        assertFalse(
                candidates.isEmpty()
        );

        assertTrue(
                candidates.size() <= 5
        );

        for (
                DiaryMemoryCandidate candidate
                : candidates
        ) {
            assertNotNull(
                    candidate.category()
            );

            assertNotNull(
                    candidate.memoryText()
            );

            assertFalse(
                    candidate.memoryText()
                            .isBlank()
            );

            assertTrue(
                    candidate.memoryText()
                            .length() <= 500
            );
        }

        /*
         * 모델의 분류에는 일정한 자유도가 있으므로
         * 정확히 동일한 배열을 강제하지 않습니다.
         *
         * 다만 테스트 일기에는
         * 반려동물에 대한 지속적 정보가 명확히 있으므로
         * PET 후보가 생성되는지는 확인합니다.
         */
        assertTrue(
                candidates.stream()
                        .anyMatch(candidate ->
                                candidate.category()
                                        == UserMemoryCategory.PET
                        )
        );

        System.out.println(
                "[OpenAI 실호출 결과 - 개인화 기억]"
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