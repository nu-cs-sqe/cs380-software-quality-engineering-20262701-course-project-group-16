package domain.model;

/** API scaffold for the immutable output of one accepted engine action. */
public final class BettingResult {
    BettingResult(GameState state, AcceptedAction acceptedAction) {
        throw new UnsupportedOperationException("TDD RED checkpoint: result construction is not implemented.");
    }

    public GameState getState() {
        throw new UnsupportedOperationException("TDD RED checkpoint: result state is not implemented.");
    }

    public AcceptedAction getAcceptedAction() {
        throw new UnsupportedOperationException("TDD RED checkpoint: accepted result is not implemented.");
    }
}
