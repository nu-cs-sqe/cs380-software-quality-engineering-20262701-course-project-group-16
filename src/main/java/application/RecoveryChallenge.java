package application;

import java.util.List;
import java.util.UUID;

/** Five distinct questions; translations never participate in scoring. */
public final class RecoveryChallenge {
    public static final int QUESTION_COUNT = 5;
    public static final int PASS_SCORE = 4;
    private final String id = UUID.randomUUID().toString();
    private final List<RecoveryQuestion> questions;

    RecoveryChallenge(List<RecoveryQuestion> questions) {
        this.questions = List.copyOf(questions);
    }

    public String getId() { return id; }
    public List<RecoveryQuestion> getQuestions() { return questions; }

    public int score(List<Integer> answers) {
        if (answers.size() != questions.size()) {
            throw new IllegalArgumentException("Answer every recovery question.");
        }
        int score = 0;
        for (int index = 0; index < questions.size(); index++) {
            int choice = answers.get(index);
            if (choice < 0 || choice >= RecoveryQuestion.CHOICES) {
                throw new IllegalArgumentException("Choose a listed answer.");
            }
            if (choice == questions.get(index).correctChoice()) {
                score++;
            }
        }
        return score;
    }
}
