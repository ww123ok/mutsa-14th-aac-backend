package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.CreditReferenceType;
import mutsa.hackathon.domain.CreditTransaction;
import mutsa.hackathon.domain.CreditTransactionType;
import mutsa.hackathon.domain.DiaryShare;
import mutsa.hackathon.domain.DiaryShareStatus;
import mutsa.hackathon.dto.ExperienceFragmentResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.CreditTransactionRepository;
import mutsa.hackathon.repository.DiaryShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class ExperienceFragmentApprovalPersistenceService {

    private static final int SHARE_REWARD_INTERVAL = 3;

    private final DiaryShareRepository diaryShareRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public ExperienceFragmentResponse approve(Long userId, Long shareId, ExperienceEmbedding embedding) {
        DiaryShare share = diaryShareRepository.findByIdWithDiaryAndUser(shareId)
                .filter(candidate -> candidate.getDiary().getUser().getId().equals(userId))
                .orElseThrow(() -> new ProjectException(ErrorCode.SHARE_NOT_FOUND));
        if (share.getShareStatus() != DiaryShareStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("This fragment cannot be approved.");
        }

        try {
            share.approve(jsonMapper.writeValueAsString(embedding.values()), embedding.model());
        } catch (Exception exception) {
            throw new IllegalStateException("Embedding could not be stored.", exception);
        }

        rewardEveryThirdShare(share);
        return ExperienceFragmentResponse.from(share);
    }

    private void rewardEveryThirdShare(DiaryShare share) {
        long approved = diaryShareRepository
                .findAllByDiaryUserIdOrderByCreatedAtDesc(share.getDiary().getUser().getId())
                .stream()
                .filter(item -> item.getShareStatus() == DiaryShareStatus.APPROVED)
                .count();
        if (approved % SHARE_REWARD_INTERVAL != 0) {
            return;
        }

        AppUser sender = share.getDiary().getUser();
        sender.addCredit(1);
        share.markRewarded(1);
        creditTransactionRepository.save(
                CreditTransaction.create(
                        sender,
                        CreditTransactionType.SHARE_REWARD,
                        1,
                        sender.getCredit(),
                        CreditReferenceType.DIARY_SHARE,
                        share.getId(),
                        "Three experience fragments shared"
                )
        );
    }
}
