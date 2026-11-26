package domain.model;

import java.util.List;
import java.util.Map;

/** Immutable paid-hand result, with contested winnings and refunds kept separate. */
public final class Settlement {
    private final GameState state;
    private final Map<String, Long> payouts;
    private final Map<String, Long> refunds;
    private final Map<String, Long> contestedPayouts;
    private final List<Map<String, Long>> pots;

    Settlement(GameState state, Map<String, Long> payouts, Map<String, Long> refunds,
            Map<String, Long> contestedPayouts, List<Map<String, Long>> pots) {
        this.state = state;
        this.payouts = Map.copyOf(payouts);
        this.refunds = Map.copyOf(refunds);
        this.contestedPayouts = Map.copyOf(contestedPayouts);
        this.pots = List.copyOf(pots);
    }

    /** Actual shares by pot, starting with the main pot. Refunds are separate. */
    public List<Map<String, Long>> getPots() { return pots; }

    public GameState getState() { return state; }
    public Map<String, Long> getPayouts() { return payouts; }
    public Map<String, Long> getRefunds() { return refunds; }
    public Map<String, Long> getContestedPayouts() { return contestedPayouts; }
}
