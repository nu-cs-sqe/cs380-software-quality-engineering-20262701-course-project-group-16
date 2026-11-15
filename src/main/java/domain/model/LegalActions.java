package domain.model;

import java.util.EnumSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/** Immutable options for the current actor. Amounts are in chips. */
public final class LegalActions {
    private static final LegalActions NONE = new LegalActions(Set.of(), 0, Optional.empty());
    private final Set<ActionType> actions;
    private final long callCost;
    private final Optional<Wager> wager;

    private LegalActions(Set<ActionType> actions, long callCost, Optional<Wager> wager) {
        this.actions = Set.copyOf(actions);
        this.callCost = callCost;
        this.wager = wager;
    }

    public static LegalActions none() { return NONE; }

    static LegalActions forDecision(long callCost, Optional<Wager> wager) {
        Set<ActionType> actions = EnumSet.of(ActionType.FOLD,
                callCost == 0 ? ActionType.CHECK : ActionType.CALL);
        wager.ifPresent(limits -> actions.add(limits.getType()));
        return new LegalActions(actions, callCost, wager);
    }

    public Set<ActionType> getActions() { return actions; }
    /** Actual capped chip deduction, or zero when a call is unavailable. */
    public long getCallCost() { return callCost; }
    public Optional<Wager> getWager() { return wager; }

    /** Legal total contribution targets for either BET or RAISE. */
    public static final class Wager {
        private final ActionType type;
        private final long maximumTarget;
        private final OptionalLong minimumFullTarget;
        private final OptionalLong shortAllInTarget;

        Wager(ActionType type, long current, long increment, long maximumTarget) {
            this.type = type;
            this.maximumTarget = maximumTarget;
            minimumFullTarget = increment <= Long.MAX_VALUE - current
                    ? OptionalLong.of(current + increment) : OptionalLong.empty();
            shortAllInTarget = maximumTarget - current < increment
                    ? OptionalLong.of(maximumTarget) : OptionalLong.empty();
        }

        public ActionType getType() { return type; }
        /** Nominal full minimum, even if unaffordable; empty if it cannot fit in a long. */
        public OptionalLong getMinimumFullTarget() { return minimumFullTarget; }
        public long getMaximumTarget() { return maximumTarget; }
        public OptionalLong getShortAllInTarget() { return shortAllInTarget; }
        public boolean allows(long target) {
            return shortAllInTarget.isPresent() ? target == shortAllInTarget.getAsLong()
                    : target >= minimumFullTarget.orElseThrow() && target <= maximumTarget;
        }
    }
}
