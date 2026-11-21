package application;

import domain.model.CompletedStatistics;
import java.util.Map;

/** Builds completed-hand payloads at the application boundary for storage tests. */
public final class HandCompletionFixture {
    private HandCompletionFixture() { }

    public static HandCompletion hand(String handId, String sessionId,
            Map<String, HandCompletion.AccountChange> accounts) {
        return new HandCompletion(sessionId, handId, accounts, Map.of());
    }

    public static HandCompletion.AccountChange change(String id, long balance,
            long net, CompletedStatistics stats) {
        return new HandCompletion.AccountChange(id, balance, net < 0 ? -net : 0,
                Math.max(0, net), stats);
    }
}
