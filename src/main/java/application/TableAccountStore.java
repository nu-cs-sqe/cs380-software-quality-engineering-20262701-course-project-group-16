package application;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/** Storage boundary for an authenticated table, implemented by the account database. */
public interface TableAccountStore {
    /** Returns total balances including chips currently on the table, keyed by account ID. */
    Map<String, Long> getBalances(Set<String> accountIds) throws IOException;

    /**
     * Atomically saves every human balance change, statistics delta and chip-history entry.
     * Deduplicate by session/hand ID. An identical saved hand succeeds without another write.
     * Reject inconsistent balances or conflicting duplicate data before changing any account.
     * A failure after durable commit must be safe to retry with the same completion.
     */
    void save(HandCompletion hand) throws IOException;
}
