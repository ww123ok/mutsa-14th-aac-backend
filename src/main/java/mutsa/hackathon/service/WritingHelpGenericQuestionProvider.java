package mutsa.hackathon.service;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 사용자 개인정보나 과거 맥락을 전혀 사용하지 않는 범용 작성 도움 질문 풀.
 *
 * 같은 날 범용 질문이 여러 번 필요한 경우에는 이미 사용한 범주의 질문을
 * 우선 피해서 질문 결이 반복되지 않도록 함.
 */
@Component
public class WritingHelpGenericQuestionProvider {

    private static final List<GenericQuestion> QUESTIONS = List.of(
            // 오늘의 장면을 떠올리는 질문
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 가장 기억에 남는 순간은 언제였나요?"),
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 하루에서 다시 떠올리고 싶은 장면이 있나요?"),
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 가장 많은 시간을 보낸 곳에서는 어떤 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 예상하지 못했던 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 하루 중 시간이 가장 빠르게 지나간 것처럼 느껴진 순간은 언제였나요?"),
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 잠깐이라도 멈춰서 바라보게 된 것이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 평소와 조금 달랐던 순간이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 하루를 한 장면으로 남긴다면 어떤 순간을 고르고 싶나요?"),
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 밖을 바라봤을 때 기억에 남는 모습이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SCENE, "오늘 하루의 시작은 어땠나요?"),

            // 사소한 일을 자연스럽게 떠올리는 질문
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 별일 아니지만 괜히 기억에 남는 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 소소하게 기분이 좋아졌던 순간은 언제였나요?"),
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 조금 귀찮거나 번거롭게 느껴졌던 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 나도 모르게 웃었던 순간이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 평소에는 그냥 지나쳤을 것 같은 일이 기억에 남았나요?"),
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 먹거나 마신 것 중 가장 기억에 남는 것은 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 들었던 소리나 음악 중 기억에 남는 것이 있나요?"),
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 본 것 중 누군가에게 보여주고 싶었던 것이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 작은 행운처럼 느껴졌던 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.SMALL_MOMENT, "오늘 생각보다 괜찮았던 일이 하나 있다면 무엇인가요?"),

            // 사람과 대화를 떠올리는 질문
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 나눈 대화 중 가장 기억에 남는 이야기는 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 누군가가 한 말 중 계속 생각나는 말이 있나요?"),
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 함께 있어서 즐거웠던 사람이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 누군가 덕분에 기분이 달라진 순간이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 누군가에게 하고 싶었지만 하지 못한 말이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 먼저 말을 걸거나 연락하고 싶었던 사람이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 사람들과 함께했던 순간 중 기억에 남는 장면이 있나요?"),
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 누군가의 행동 중 인상 깊었던 것이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 누군가에게 고마움을 느낀 순간이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.PEOPLE, "오늘 있었던 이야기 중 누군가에게 가장 먼저 들려주고 싶은 이야기는 무엇인가요?"),

            // 기분과 생각을 자연스럽게 기록하는 질문
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 가장 기분이 좋았던 순간에는 어떤 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 조금 아쉬움이 남는 순간이 있다면 어떤 일이었나요?"),
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 하루 중 마음이 가장 편안했던 순간은 언제였나요?"),
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 순간적으로 당황하거나 놀랐던 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 기대했던 것과 실제로 달랐던 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 괜히 신경 쓰였던 일이 하나 있다면 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 시간이 지나도 기억에 남을 것 같은 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 하루 중 기분이 달라졌던 순간에는 어떤 일이 있었나요?"),
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 처음에는 별생각 없었지만 나중에 다시 생각난 일이 있나요?"),
            q(WritingHelpGenericQuestionCategory.FEELING_THOUGHT, "오늘 스스로에게 조금 만족스러웠던 순간이 있었나요?"),

            // 하루를 마무리하며 쓰기 좋은 질문
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "오늘 하루에서 가장 좋았던 한 가지를 고른다면 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "오늘 하루에서 조금 아쉬웠던 한 가지를 고른다면 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "오늘로 다시 돌아간다면 한 번 더 경험하고 싶은 순간은 언제인가요?"),
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "오늘 하루를 짧은 제목으로 붙인다면 어떤 제목이 어울릴까요? 그 이유는 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "오늘을 사진 한 장으로 남긴다면 어떤 모습을 찍고 싶나요?"),
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "오늘 있었던 일 중 내일도 기억하고 싶은 것은 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "오늘 하루가 어제와 달랐던 점이 있다면 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "오늘 하루를 누군가에게 이야기한다면 어떤 이야기부터 꺼내고 싶나요?"),
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "오늘 하루를 마무리하면서 가장 먼저 떠오르는 장면은 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.WRAP_UP, "지금 이 순간, 오늘 있었던 일 중 가장 먼저 떠오르는 것은 무엇인가요?"),

            // 추가 재미/확장 질문
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘 하루를 영화의 한 장면으로 만든다면 어떤 장면이 될까요?"),
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘의 나에게 별명을 하나 붙인다면 무엇이 가장 어울릴까요?"),
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘 하루에 배경음악을 하나 깐다면 어떤 분위기의 음악일까요?"),
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘 있었던 일 중 10년 뒤에도 뜬금없이 기억날 것 같은 순간은 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘 하루에서 딱 5초만 영상으로 저장할 수 있다면 어떤 순간을 남기고 싶나요?"),
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘 하루를 이모지 3개로 표현한다면 어떤 이모지를 고르고 싶나요? 왜 그 이모지인가요?"),
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘의 나를 몰래 지켜본 사람이 있다면 가장 웃겼을 것 같은 순간은 언제였나요?"),
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘 하루에서 누군가에게 스포일러하고 싶은 장면이 하나 있다면 무엇인가요?"),
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘 있었던 일 중 제목만 들으면 아무도 내용을 예상하지 못할 것 같은 이야기가 있나요?"),
            q(WritingHelpGenericQuestionCategory.PLAYFUL, "오늘을 게임이라고 생각한다면, 오늘 얻은 아이템이나 경험치는 무엇이었나요?")
    );

    /**
     * 기존 호출 호환용. 카테고리 다양성 정보가 없으면 정확한 문구 중복만 피함
     */
    public String nextQuestion(
            List<String> excludedQuestions
    ) {
        return nextQuestion(
                excludedQuestions,
                List.of()
        );
    }

    /**
     * 오늘 이미 사용한 범용 질문의 카테고리를 우선 피해서 한 문항을 선택.
     *
     * excludedQuestions에는 최근 며칠간의 동일 문구 반복 방지용 질문까지 들어올 수 있지만,
     * 카테고리 제외는 todayQuestions만 기준으로 계산. 따라서 과거에 한 번 사용했던 범주가
     * 며칠 동안 통째로 막히는 일은 없음.
     */
    public String nextQuestion(
            List<String> excludedQuestions,
            List<String> todayQuestions
    ) {
        Set<String> excluded =
                normalizeSet(
                        excludedQuestions
                );

        Set<WritingHelpGenericQuestionCategory> usedTodayCategories =
                findUsedCategories(
                        todayQuestions
                );

        List<GenericQuestion> exactFiltered =
                QUESTIONS.stream()
                        .filter(question ->
                                !excluded.contains(
                                        normalize(
                                                question.text()
                                        )
                                )
                        )
                        .toList();

        List<GenericQuestion> available =
                exactFiltered.isEmpty()
                        ? QUESTIONS
                        : exactFiltered;

        List<WritingHelpGenericQuestionCategory> preferredCategories =
                available.stream()
                        .map(GenericQuestion::category)
                        .distinct()
                        .filter(category ->
                                !usedTodayCategories.contains(
                                        category
                                )
                        )
                        .toList();

        List<WritingHelpGenericQuestionCategory> candidateCategories =
                preferredCategories.isEmpty()
                        ? available.stream()
                        .map(GenericQuestion::category)
                        .distinct()
                        .toList()
                        : preferredCategories;

        WritingHelpGenericQuestionCategory selectedCategory =
                randomItem(
                        candidateCategories
                );

        List<GenericQuestion> categoryCandidates =
                available.stream()
                        .filter(question ->
                                question.category()
                                        == selectedCategory
                        )
                        .toList();

        return randomItem(
                categoryCandidates
        ).text();
    }

    int questionCount() {
        return QUESTIONS.size();
    }

    List<String> questions() {
        return QUESTIONS.stream()
                .map(GenericQuestion::text)
                .toList();
    }

    WritingHelpGenericQuestionCategory categoryOf(
            String questionText
    ) {
        if (questionText == null || questionText.isBlank()) {
            return null;
        }

        String normalized = normalize(questionText);

        return QUESTIONS.stream()
                .filter(question ->
                        normalize(
                                question.text()
                        ).equals(normalized)
                )
                .map(GenericQuestion::category)
                .findFirst()
                .orElse(null);
    }

    private Set<WritingHelpGenericQuestionCategory> findUsedCategories(
            List<String> todayQuestions
    ) {
        if (todayQuestions == null || todayQuestions.isEmpty()) {
            return Set.of();
        }

        Set<WritingHelpGenericQuestionCategory> categories =
                new HashSet<>();

        for (String question : todayQuestions) {
            WritingHelpGenericQuestionCategory category =
                    categoryOf(question);

            if (category != null) {
                categories.add(category);
            }
        }

        return categories;
    }

    private Set<String> normalizeSet(
            List<String> questions
    ) {
        if (questions == null || questions.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized =
                new HashSet<>();

        for (String question : questions) {
            if (question == null || question.isBlank()) {
                continue;
            }

            normalized.add(
                    normalize(question)
            );
        }

        return normalized;
    }

    private <T> T randomItem(
            List<T> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                    "범용 작성 도움 질문 후보가 없습니다."
            );
        }

        return items.get(
                ThreadLocalRandom.current()
                        .nextInt(
                                items.size()
                        )
        );
    }

    private String normalize(
            String question
    ) {
        return question
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static GenericQuestion q(
            WritingHelpGenericQuestionCategory category,
            String text
    ) {
        return new GenericQuestion(
                category,
                text
        );
    }

    private record GenericQuestion(
            WritingHelpGenericQuestionCategory category,
            String text
    ) {
    }
}
