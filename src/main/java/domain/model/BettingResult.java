package domain.model;

/** Immutable output of one accepted engine action, created after engine validation. */
public final class BettingResult {
    private final GameState state;
    private final AcceptedAction acceptedAction;

    BettingResult(GameState state, AcceptedAction acceptedAction) {
        this.state = state;
        this.acceptedAction = acceptedAction;
    }

    public GameState getState() {
        return state;
    }

    public AcceptedAction getAcceptedAction() {
        return acceptedAction;
    }
}
