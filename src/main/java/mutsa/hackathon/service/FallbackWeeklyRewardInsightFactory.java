package mutsa.hackathon.service;

import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class FallbackWeeklyRewardInsightFactory {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일");

    public WeeklyRewardInsight create(
            WeeklyRewardGenerationContext context
    ) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        context.days().stream()
                .flatMap(day -> day.keywords().stream())
                .filter(value -> value != null && !value.isBlank())
                .forEach(value -> {
                    if (keywords.size() < 3) {
                        keywords.add(value);
                    }
                });

        if (keywords.isEmpty()) {
            keywords.add("기록");
            keywords.add("흐름");
        }

        String title = "%s부터 이어진 한 주".formatted(
                context.weekStartDate().format(DATE_FORMAT)
        );

        String summary = (
                "이번 주에는 %d개의 기록과 서로 다른 색이 쌓였습니다. "
                        + "각 날의 분위기가 하나의 장면 안에서 자연스럽게 이어집니다."
        ).formatted(context.days().size());

        return new WeeklyRewardInsight(
                title,
                summary,
                List.copyOf(keywords),
                "서로 다른 색의 생활 장면과 사물이 한 주의 흐름으로 연결되는 구성"
        );
    }
}