package application;

import java.util.List;

/** A language-independent question ID and its correct choice. */
public final class RecoveryQuestion {
    public static final int CHOICES = 3;
    static final List<RecoveryQuestion> BANK = List.of(
            new RecoveryQuestion("blinds", 1),
            new RecoveryQuestion("fold", 2),
            new RecoveryQuestion("private", 0),
            new RecoveryQuestion("board", 1),
            new RecoveryQuestion("allIn", 2),
            new RecoveryQuestion("refund", 0),
            new RecoveryQuestion("sidePot", 1),
            new RecoveryQuestion("tie", 2),
            new RecoveryQuestion("quit", 0),
            new RecoveryQuestion("topUp", 1));

    private final String id;
    private final int correctChoice;

    private RecoveryQuestion(String id, int correctChoice) {
        this.id = id;
        this.correctChoice = correctChoice;
    }

    public String getId() { return id; }
    int correctChoice() { return correctChoice; }
}
