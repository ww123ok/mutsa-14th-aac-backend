package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.UserMemoryCategory;
import mutsa.hackathon.domain.UserMemoryItem;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * AI 프롬프트 단계에서 놓친 명확한 중복을
 * DB 저장 직전에 한 번 더 차단.
 * 벡터 검색이나 과도한 의미 추론은 하지 않고,
 * 해커톤 MVP에서 안전하게 판단할 수 있는
 * 명백한 중복만 보수적으로 제거.
 */
@Component
public class DiaryMemoryDuplicateGuard {

    private static final List<String>
            JOB_COPULA_ENDINGS = List.of(
            "입니다",
            "이에요",
            "예요",
            "이다",
            "임"
    );

    /**
     * 이미 알고 있는 사용자 정보 또는 승인된 기억과
     * 명확하게 같은 사실이면 true를 반환
     */
    public boolean isDuplicate(
            AppUser user,
            DiaryMemoryCandidate candidate,
            List<UserMemoryItem> existingMemories
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "중복 확인 대상 사용자는 필수입니다."
            );
        }

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "중복 확인 대상 기억 후보는 필수입니다."
            );
        }

        if (
                candidate.category()
                        == UserMemoryCategory.WORK_STUDY
                        && isSameAsOnboardingJob(
                        user.getJob(),
                        candidate.memoryText()
                )
        ) {
            return true;
        }

        if (
                existingMemories == null
                        || existingMemories.isEmpty()
        ) {
            return false;
        }

        String candidateKey =
                createComparisonKey(
                        candidate.memoryText()
                );

        return existingMemories.stream()
                .map(UserMemoryItem::getMemoryText)
                .map(this::createComparisonKey)
                .anyMatch(candidateKey::equals);
    }

    /**
     * 같은 AI 응답 안에서 공백/문장부호만 다른
     * 후보를 반복 반환했는지 확인할 때 사용
     */
    public String createCandidateKey(
            DiaryMemoryCandidate candidate
    ) {
        if (candidate == null) {
            throw new IllegalArgumentException(
                    "기억 후보는 필수입니다."
            );
        }

        return createComparisonKey(
                candidate.memoryText()
        );
    }

    private boolean isSameAsOnboardingJob(
            String job,
            String memoryText
    ) {
        if (
                job == null
                        || job.isBlank()
        ) {
            return false;
        }

        String normalizedJob =
                createComparisonKey(job);

        String normalizedMemory =
                createComparisonKey(memoryText);

        if (
                normalizedJob.isBlank()
                        || normalizedMemory.isBlank()
        ) {
            return false;
        }

        if (
                normalizedJob.equals(
                        normalizedMemory
                )
        ) {
            return true;
        }

        for (String ending : JOB_COPULA_ENDINGS) {
            if (
                    normalizedMemory.equals(
                            normalizedJob + ending
                    )
                            || normalizedJob.equals(
                            normalizedMemory + ending
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    private String createComparisonKey(
            String value
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return "";
        }

        return Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFKC
                )
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "[^\\p{L}\\p{N}]",
                        ""
                );
    }
}