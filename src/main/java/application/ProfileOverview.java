package application;

import java.util.List;

/** Saved balance and history for an authenticated account. */
public final class ProfileOverview {
    private final long balance;
    private final List<ChipHistoryEntry> history;

    ProfileOverview(long balance, List<ChipHistoryEntry> history) {
        this.balance = balance;
        this.history = List.copyOf(history);
    }

    public long getBalance() { return balance; }
    public List<ChipHistoryEntry> getHistory() { return history; }
}
