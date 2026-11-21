package application;

/** Immutable account totals and the portion reserved for the next or current hand. */
public final class TableFunds {
    private final long balance;
    private final long tableChips;
    private final long sessionNetChange;
    private final long fundingLimit;

    TableFunds(long balance, long tableChips, long sessionNetChange) {
        this(balance, tableChips, sessionNetChange, balance - tableChips);
    }

    TableFunds(long balance, long tableChips, long sessionNetChange, long fundingLimit) {
        if (balance < 0 || tableChips < 0 || tableChips > balance) {
            throw new IllegalArgumentException("Table chips must fit within the account balance.");
        }
        if (fundingLimit < 0 || fundingLimit > balance - tableChips) {
            throw new IllegalArgumentException("Funding limit must fit the available balance.");
        }
        this.fundingLimit = fundingLimit;
        this.balance = balance;
        this.tableChips = tableChips;
        this.sessionNetChange = sessionNetChange;
    }

    public long getBalance() { return balance; }
    public long getTableChips() { return tableChips; }
    public long getAvailableChips() { return balance - tableChips; }
    public long getSessionNetChange() { return sessionNetChange; }
    /** Smallest positive addition that funds a stack of at least the minimum buy-in. */
    public long getMinimumTopUp() { return Math.max(1, BuyInLimits.MINIMUM - tableChips); }

    /** Largest affordable addition, or zero when the minimum cannot be reached. */
    public long getMaximumTopUp() {
        return fundingLimit < getMinimumTopUp() ? 0 : fundingLimit;
    }

    public boolean needsTopUp() { return tableChips == 0 && !needsRecovery(); }
    public boolean needsRecovery() { return tableChips == 0 && balance < BuyInLimits.MINIMUM; }
}
