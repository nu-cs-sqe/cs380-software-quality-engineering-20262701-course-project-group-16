package application;

import domain.model.GameConfig;
import java.util.Map;
import java.util.Set;

/** Storage input for one completed hand; keys are stable profile IDs, not seat or bot IDs. */
public final class ProfileHandSettlement {
    private final String handId;
    private final String sessionId;
    private final Map<String, AccountHandResult> accounts;

    public ProfileHandSettlement(String handId, String sessionId,
            Map<String, AccountHandResult> accounts) {
        if (handId.isBlank() || sessionId.isBlank()) {
            throw new IllegalArgumentException("A hand and session ID are required.");
        }
        requireCompletedAccounts(handId, accounts);
        this.handId = handId;
        this.sessionId = sessionId;
        this.accounts = Map.copyOf(accounts);
    }

    private static void requireCompletedAccounts(String handId,
            Map<String, AccountHandResult> accounts) {
        if (accounts.isEmpty() || accounts.size() > GameConfig.SEAT_COUNT) {
            throw new IllegalArgumentException("Supply one to six completed accounts.");
        }
        accounts.forEach((id, result) -> requireAccountHand(id, handId, result));
    }

    private static void requireAccountHand(String id, String handId, AccountHandResult result) {
        if (id.isBlank() || !result.getStatistics().getCompletedHandIds().equals(Set.of(handId))) {
            throw new IllegalArgumentException(
                    "Supply a valid profile and the same completed hand.");
        }
    }

    public String getHandId() { return handId; }
    public String getSessionId() { return sessionId; }
    public Map<String, AccountHandResult> getAccounts() { return accounts; }
}
