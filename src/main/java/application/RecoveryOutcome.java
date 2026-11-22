package application;

/** Result of grading and, when eligible, saving a recovery reward. */
public enum RecoveryOutcome {
    WRONG_ANSWERS, AWARDED, ALREADY_AWARDED, DAILY_LIMIT, FUNDED
}
