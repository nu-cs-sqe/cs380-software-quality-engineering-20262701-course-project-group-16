package application;

import domain.model.CompletedStatistics;

/** Authoritative chip and statistics changes for one account in a completed hand. */
public final class AccountHandResult {
    private final long chipChange;
    private final CompletedStatistics statistics;

    public AccountHandResult(long chipChange, CompletedStatistics statistics) {
        this.chipChange = chipChange;
        this.statistics = statistics;
    }

    public long getChipChange() { return chipChange; }
    public CompletedStatistics getStatistics() { return statistics; }
}
