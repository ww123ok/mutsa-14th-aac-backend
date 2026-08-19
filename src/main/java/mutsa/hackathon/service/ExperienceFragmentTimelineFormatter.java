package mutsa.hackathon.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the diary editor's bracketed visit timestamps into non-identifying
 * time-of-day blocks before the content is sent for anonymization.
 */
public final class ExperienceFragmentTimelineFormatter {

    private static final Pattern TIMESTAMP = Pattern.compile(
            "\\[(?i:(AM|PM)|오전|오후)\\s*(0?[1-9]|1[0-2])\\s*:\\s*([0-5]\\d)\\]"
    );

    private ExperienceFragmentTimelineFormatter() {
    }

    public static String normalize(String diaryContent) {
        Matcher matcher = TIMESTAMP.matcher(diaryContent);
        if (!matcher.find()) {
            return diaryContent;
        }

        List<RawEntry> entries = new ArrayList<>();
        String leadingContent = diaryContent.substring(0, matcher.start()).trim();
        int contentStart = matcher.end();
        String period = periodOf(matcher.group(1), matcher.group(2));

        while (matcher.find()) {
            entries.add(new RawEntry(period, diaryContent.substring(contentStart, matcher.start()).trim()));
            period = periodOf(matcher.group(1), matcher.group(2));
            contentStart = matcher.end();
        }
        entries.add(new RawEntry(period, diaryContent.substring(contentStart).trim()));

        List<TimeBlock> blocks = consecutiveTimeBlocks(entries);
        List<String> paragraphs = new ArrayList<>();
        if (!leadingContent.isBlank()) {
            paragraphs.add(leadingContent);
        }
        for (TimeBlock block : blocks) {
            String content = String.join("\n\n", block.contents());
            paragraphs.add(content.isBlank() ? "[" + block.period() + "]" : "[" + block.period() + "]\n" + content);
        }
        return String.join("\n\n", paragraphs);
    }

    private static List<TimeBlock> consecutiveTimeBlocks(List<RawEntry> entries) {
        List<TimeBlock> blocks = new ArrayList<>();
        for (RawEntry entry : entries) {
            TimeBlock current = blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);
            if (current == null || !current.period().equals(entry.period())) {
                current = new TimeBlock(entry.period());
                blocks.add(current);
            }
            if (!entry.content().isBlank()) {
                current.contents().add(entry.content());
            }
        }
        return blocks;
    }

    private static String periodOf(String meridiem, String hourText) {
        int hour = Integer.parseInt(hourText);
        int hourOfDay;
        if ("오전".equals(meridiem) || "AM".equalsIgnoreCase(meridiem)) {
            hourOfDay = hour == 12 ? 0 : hour;
        } else {
            hourOfDay = hour == 12 ? 12 : hour + 12;
        }

        if (hourOfDay < 3) return "한밤중";
        if (hourOfDay < 6) return "새벽";
        if (hourOfDay < 9) return "아침";
        if (hourOfDay < 12) return "오전";
        if (hourOfDay < 15) return "낮";
        if (hourOfDay < 18) return "오후";
        if (hourOfDay < 21) return "저녁";
        return "밤";
    }

    private record RawEntry(String period, String content) {
    }

    private record TimeBlock(String period, List<String> contents) {
        private TimeBlock(String period) {
            this(period, new ArrayList<>());
        }
    }
}
