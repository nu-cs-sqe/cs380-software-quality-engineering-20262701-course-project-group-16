package domain;

/**
 * Immutable engine observation. All-in is resolved to CALL, RAISE, BET, or
 * FORCED_POST before construction; chip amount alone never determines raise level.
 */
public final class StatsAction {
    private final boolean accepted;
    private final BettingStreet street;
    private final ResolvedActionType type;
    private final long chipsAdded;
    private final StatsContext context;

    /**
     * Creates one resolved observation. Both accepted and rejected observations
     * must be structurally valid; PlayStats ignores rejected observations.
     *
     * @param accepted whether the engine accepted the game action
     * @param street street on which the decision occurred
     * @param type resolved betting meaning, including the meaning of any all-in
     * @param chipsAdded newly committed chips, not a raise-to amount
     * @param context engine-provided eligibility and raise classification
     * @throws IllegalArgumentException if the observation is inconsistent
     */
    public StatsAction(boolean accepted, BettingStreet street, ResolvedActionType type,
            long chipsAdded, StatsContext context) {
        if (street == null || type == null || context == null || chipsAdded < 0) {
            throw new IllegalArgumentException("Action fields must be present and chips nonnegative.");
        }

        boolean addsChips = type == ResolvedActionType.CALL || type == ResolvedActionType.BET
                || type == ResolvedActionType.RAISE || type == ResolvedActionType.FORCED_POST;
        if ((addsChips && chipsAdded == 0) || (!addsChips && chipsAdded != 0)) {
            throw new IllegalArgumentException("Chip amount does not match the resolved action.");
        }

        validateStreet(street, type);
        validateContext(street, type, context);
        this.accepted = accepted;
        this.street = street;
        this.type = type;
        this.chipsAdded = chipsAdded;
        this.context = context;
    }

    boolean isAccepted() {
        return accepted;
    }

    BettingStreet getStreet() {
        return street;
    }

    ResolvedActionType getType() {
        return type;
    }

    long getChipsAdded() {
        return chipsAdded;
    }

    StatsContext getContext() {
        return context;
    }

    private static void validateStreet(BettingStreet street, ResolvedActionType type) {
        if (street == BettingStreet.PREFLOP && type == ResolvedActionType.BET) {
            throw new IllegalArgumentException("A preflop opening raise must be resolved as RAISE.");
        }

        if (street != BettingStreet.PREFLOP && type == ResolvedActionType.FORCED_POST) {
            throw new IllegalArgumentException("Forced posts belong to preflop.");
        }

    }

    private static void validateContext(BettingStreet street, ResolvedActionType type,
            StatsContext context) {
        boolean preflopRaise = street == BettingStreet.PREFLOP && type == ResolvedActionType.RAISE;
        if (preflopRaise != (context.getPreflopRaiseType() != PreflopRaiseType.NONE)) {
            throw new IllegalArgumentException("Preflop raise context must match the action.");
        }

        if (context.hasThreeBetOpportunity() && (street != BettingStreet.PREFLOP
                || (type != ResolvedActionType.CALL && type != ResolvedActionType.FOLD
                && type != ResolvedActionType.RAISE))) {
            throw new IllegalArgumentException("Invalid three-bet opportunity context.");
        }

        if ((context.hasFlopCBetOpportunity() || context.hasFacedFlopCBetOpportunity())
                && street != BettingStreet.FLOP) {
            throw new IllegalArgumentException("Flop opportunity context belongs to the flop.");
        }

        if (context.hasFlopCBetOpportunity() && type != ResolvedActionType.BET
                && type != ResolvedActionType.CHECK && type != ResolvedActionType.FOLD) {
            throw new IllegalArgumentException("A c-bet opportunity requires an opening decision.");
        }

        if (context.hasFacedFlopCBetOpportunity() && type != ResolvedActionType.CALL
                && type != ResolvedActionType.RAISE && type != ResolvedActionType.FOLD) {
            throw new IllegalArgumentException("Facing a c-bet requires a response decision.");
        }

    }
}
