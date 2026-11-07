package domain.model;

import java.util.Map;

/** Immutable paid-hand result, with contested winnings and refunds kept separate. */
public final class Settlement {
    private final GameState state;
    private final Map<String, Long> payouts;
    private final Map<String, Long> refunds;
    private final Map<String, Long> contestedPayouts;

    Settlement(GameState state, Map<String, Long> payouts, Map<String, Long> refunds,
            Map<String, Long> contestedPayouts) {
        this.state = state;
        this.payouts = Map.copyOf(payouts);
        this.refunds = Map.copyOf(refunds);
        this.contestedPayouts = Map.copyOf(contestedPayouts);
    }

    public GameState getState() { return state; }
    public Map<String, Long> getPayouts() { return payouts; }
    public Map<String, Long> getRefunds() { return refunds; }
    public Map<String, Long> getContestedPayouts() { return contestedPayouts; }
}
