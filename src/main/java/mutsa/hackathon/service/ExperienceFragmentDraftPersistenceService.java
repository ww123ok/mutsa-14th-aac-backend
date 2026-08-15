package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.DiaryShare;
import mutsa.hackathon.domain.DiaryShareStatus;
import mutsa.hackathon.repository.DiaryShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExperienceFragmentDraftPersistenceService {

    private final DiaryShareRepository diaryShareRepository;

    @Transactional
    public void saveDraft(Long shareId, ExperienceFragmentDraft draft) {
        DiaryShare share = diaryShareRepository.findByIdWithDiaryAndUser(shareId).orElseThrow();
        if (share.getShareStatus() == DiaryShareStatus.REQUESTED) {
            share.requireReview(draft.anonymizedContent(), draft.generalTopic(),
                    draft.keywords(), draft.matchingText());
        }
    }

    @Transactional
    public void block(Long shareId) {
        diaryShareRepository.findByIdWithDiaryAndUser(shareId).ifPresent(share -> {
            if (share.getShareStatus() == DiaryShareStatus.REQUESTED) {
                share.block("ANONYMIZATION_FAILED");
            }
        });
    }
}
