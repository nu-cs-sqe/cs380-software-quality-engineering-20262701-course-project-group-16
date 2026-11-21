package application;

import domain.model.CompletedStatistics;
import java.util.Map;

/** Completed hand data for atomic account storage; contains no cards or credentials. */
public final class HandCompletion {
    private final String sessionId;
    private final String handId;
    private final Map<String, AccountChange> accounts;
    private final Map<String, Long> botChanges;

    HandCompletion(String sessionId, String handId, Map<String, AccountChange> accounts,
            Map<String, Long> botChanges) {
        this.sessionId = sessionId;
        this.handId = handId;
        this.accounts = Map.copyOf(accounts);
        this.botChanges = Map.copyOf(botChanges);
    }

    public String getSessionId() { return sessionId; }
    public String getHandId() { return handId; }
    public Map<String, AccountChange> getAccounts() { return accounts; }
    public Map<String, Long> getBotChanges() { return botChanges; }

    /** Starting stack includes blinds. Net change includes awards and uncalled refunds. */
    public static final class AccountChange {
        private final String accountId;
        private final long balanceBefore;
        private final long startingStack;
        private final long endingStack;
        private final long balanceAfter;
        private final CompletedStatistics statistics;

        AccountChange(String accountId, long balanceBefore, long startingStack,
                long endingStack, CompletedStatistics statistics) {
            this.accountId = accountId;
            this.balanceBefore = balanceBefore;
            this.startingStack = startingStack;
            this.endingStack = endingStack;
            this.statistics = statistics;
            this.balanceAfter = Math.addExact(balanceBefore,
                    Math.subtractExact(endingStack, startingStack));
        }

        public String getAccountId() { return accountId; }
        public long getBalanceBefore() { return balanceBefore; }
        public long getStartingStack() { return startingStack; }
        public long getEndingStack() { return endingStack; }
        public long getNetChange() { return Math.subtractExact(endingStack, startingStack); }
        public long getBalanceAfter() { return balanceAfter; }
        public CompletedStatistics getStatistics() { return statistics; }
    }
}
