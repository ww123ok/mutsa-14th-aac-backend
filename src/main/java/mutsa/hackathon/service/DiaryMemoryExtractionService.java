package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryMemoryExtractionService {

    private final DiaryRepository
            diaryRepository;

    private final DiaryMemoryCandidateExtractor
            diaryMemoryCandidateExtractor;

    private final DiaryMemoryCandidatePersistenceService
            diaryMemoryCandidatePersistenceService;

    private final DiaryMemoryApplicationService
            diaryMemoryApplicationService;

    /**
     * 외부 AI 호출 동안 DB 트랜잭션과 Connection을
     * 점유하지 않도록 이 메서드 자체에는
     * @Transactional을 붙이지 않음
     */
    public void extractAndApply(
            Long diaryId
    ) {
        Diary diary =
                diaryRepository
                        .findByIdWithUser(
                                diaryId
                        )
                        .orElse(null);

        if (diary == null) {
            log.warn(
                    "Diary memory extraction skipped because diary does not exist: diaryId={}",
                    diaryId
            );

            return;
        }

        if (diary.isDeleted()) {
            return;
        }

        if (
                diary.getMemoryAppliedAt()
                        != null
        ) {
            return;
        }

        if (
                !diary.getUser()
                        .isAiMemoryConsent()
        ) {
            return;
        }

        try {
            DiaryMemoryExtractionPrompt prompt =
                    new DiaryMemoryExtractionPrompt(
                            diary.getContent(),
                            diary.getUser()
                                    .getJob(),
                            diary.getUser()
                                    .getAiMemoryProfile()
                    );

            List<DiaryMemoryCandidate> candidates =
                    diaryMemoryCandidateExtractor
                            .extract(prompt);

            diaryMemoryCandidatePersistenceService
                    .saveCandidates(
                            diaryId,
                            candidates
                    );

            diaryMemoryApplicationService
                    .apply(
                            diaryId
                    );

        } catch (RuntimeException exception) {
            /*
             * 기억 추출은 부가적인 개인화 기능이므로
             * 실패해도 이미 저장된 일기, 성찰 질문,
             * 색 보상 흐름에는 영향을 주지 않음.
             * 일기 본문이나 AI 응답 본문은 로그에
             * 절대 기록하지 않음.
             */
            log.warn(
                    "Diary memory extraction failed: diaryId={}, reason={}",
                    diaryId,
                    exception
                            .getClass()
                            .getSimpleName()
            );
        }
    }
}