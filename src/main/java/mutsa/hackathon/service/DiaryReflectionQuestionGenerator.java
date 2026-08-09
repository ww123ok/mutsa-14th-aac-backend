package mutsa.hackathon.service;

@FunctionalInterface
public interface DiaryReflectionQuestionGenerator {

    String generate(String diaryContent);
}