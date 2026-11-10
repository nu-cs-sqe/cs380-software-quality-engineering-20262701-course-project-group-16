package bot;

import java.util.Set;

/** Public awardable pot in the scenario being evaluated; excludes uncalled refunds. */
public final class EquityPot {
    private final String id;
    private final long amount;
    private final Set<String> eligiblePlayers;

    public EquityPot(String id, long amount, Set<String> eligiblePlayers) {
        requireId(id);
        requireEligibility(amount, eligiblePlayers);
        this.id = id;
        this.amount = amount;
        this.eligiblePlayers = Set.copyOf(eligiblePlayers);
    }

    private static void requireEligibility(long amount, Set<String> players) {
        if (amount <= 0 || players == null || players.isEmpty()) {
            throw new IllegalArgumentException("A positive pot and eligible players are required.");
        }
        for (String player : players) {
            requireId(player);
        }
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Pot/player IDs must be nonblank.");
        }
    }

    public String getId() { return id; }
    public long getAmount() { return amount; }
    public Set<String> getEligiblePlayers() { return eligiblePlayers; }
}
