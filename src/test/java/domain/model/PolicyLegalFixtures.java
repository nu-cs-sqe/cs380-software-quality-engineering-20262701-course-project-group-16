package domain.model;

import java.util.Optional;

/** Explicit immutable option values for isolated policy tests. */
public final class PolicyLegalFixtures {
    private PolicyLegalFixtures() { }

    public static LegalActions passive(long cost) {
        return LegalActions.forDecision(cost, Optional.empty());
    }

    public static LegalActions wager(long cost, ActionType type, long current,
            long increment, long maximum) {
        return LegalActions.forDecision(cost,
                Optional.of(new LegalActions.Wager(type, current, increment, maximum)));
    }
}
