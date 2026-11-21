package application;

import domain.model.Settlement;
import java.util.Optional;

/** Save on a background worker, then publish through the owning SetupSession. */
public final class PreparedSettlement {
    private final Settlement paid;
    private final MatchStatistics completed;
    private final Optional<AccountTable> nextAccounts;
    private final Optional<HandCompletion> completion;
    private final Runnable writer;
    private final long nextRevision;
    private volatile boolean saved;

    PreparedSettlement(Settlement paid, MatchStatistics completed,
            Optional<AccountTable> nextAccounts, Optional<HandCompletion> completion,
            Runnable writer, long nextRevision) {
        this.paid = paid;
        this.completed = completed;
        this.nextAccounts = nextAccounts;
        this.completion = completion;
        this.writer = writer;
        this.nextRevision = nextRevision;
    }

    /** Performs storage only. A failed write can be retried with this same token. */
    public synchronized void save() {
        if (!saved) {
            writer.run();
            saved = true;
        }
    }

    public boolean isSaved() { return saved; }
    public Optional<HandCompletion> getHandCompletion() { return completion; }
    Settlement paid() { return paid; }
    MatchStatistics completed() { return completed; }
    Optional<AccountTable> nextAccounts() { return nextAccounts; }
    long nextRevision() { return nextRevision; }
}
