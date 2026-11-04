package domain.model;

/**
 * API scaffold for the first test-first betting increment. No betting behavior is implemented yet.
 * The application will record statistics only after receiving an accepted result.
 */
public final class BettingEngine {
    /**
     * Applies a legal action to a dealt snapshot and returns a new state plus accepted action.
     * The initial contract covers the first preflop CALL; later behaviors need further BVA.
     * @throws IllegalArgumentException for a null state/request or an out-of-turn request
     * @throws UnsupportedOperationException while this RED checkpoint remains unimplemented
     */
    public BettingResult apply(GameState before, ActionRequest request) {
        throw new UnsupportedOperationException("TDD RED checkpoint: betting behavior is not implemented.");
    }
}
