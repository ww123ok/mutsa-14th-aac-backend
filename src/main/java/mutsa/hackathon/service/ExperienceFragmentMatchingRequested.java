package mutsa.hackathon.service;

/** Published after a diary is committed so matching never delays diary creation. */
public record ExperienceFragmentMatchingRequested(Long diaryId) {
}
