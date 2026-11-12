package domain.model;
import java.util.Map;
import java.util.Set;

/** Immutable round bookkeeping supplied by the engine, including raise-reopening rights. */
public final class BettingProgress {
    private final long currentBet;
    private final long lastFullRaiseSize;
    private final Set<String> playersToAct;
    private final Set<String> playersAllowedToRaise;
    private final Map<String, Long> lastDecisionTargets;

    /**
     * Raise eligibility need not be a subset of pending actors: another full raise may create
     * a new decision. GameState checks that both sets refer to players who can act.
     * @throws IllegalArgumentException for invalid amounts, null sets or blank IDs
     */
    public BettingProgress(long currentBet, long lastFullRaiseSize,
            Set<String> pending, Set<String> raise) {
        this(currentBet, lastFullRaiseSize, pending, raise, Map.of());
    }

    /**
     * Retains each active player's last voluntary decision level for cumulative reopening.
     * Forced blinds are not decisions. An empty map preserves legacy snapshot construction.
     * @throws IllegalArgumentException for invalid amounts, IDs or decision levels
     */
    public BettingProgress(long currentBet, long lastFullRaiseSize,
            Set<String> pending, Set<String> raise, Map<String, Long> lastDecisionTargets) {
        if (currentBet < 0 || lastFullRaiseSize <= 0) {
            throw new IllegalArgumentException(
                "Current bet must be nonnegative and full raise size positive.");
        }
        this.currentBet = currentBet;
        this.lastFullRaiseSize = lastFullRaiseSize;
        this.playersToAct = copyIds(pending);
        this.playersAllowedToRaise = copyIds(raise);
        validateDecisionTargets(lastDecisionTargets, currentBet);
        this.lastDecisionTargets = Map.copyOf(lastDecisionTargets);
    }

    private static Set<String> copyIds(Set<String> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("Bookkeeping requires nonblank player IDs.");
        }
        return Set.copyOf(ids);
    }

    private static void validateDecisionTargets(Map<String, Long> targets, long current) {
        if (targets == null || targets.entrySet().stream().anyMatch(entry ->
                entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null
                || entry.getValue() < 0 || entry.getValue() > current)) {
            throw new IllegalArgumentException(
                    "Decision levels must name players and fit the round.");
        }
    }

    public Map<String, Long> getLastDecisionTargets() { return lastDecisionTargets; }

    public long getCurrentBet() { return currentBet; }
    public long getLastFullRaiseSize() { return lastFullRaiseSize; }
    public Set<String> getPlayersToAct() { return playersToAct; }
    public Set<String> getPlayersAllowedToRaise() { return playersAllowedToRaise; }
}
