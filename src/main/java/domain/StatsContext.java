package domain;

/** Immutable eligibility and raise classification supplied by the betting engine. */
public final class StatsContext {
    private final PreflopRaiseType preflopRaiseType;
    private final boolean threeBetOpportunity;
    private final boolean flopCBetOpportunity;
    private final boolean facedFlopCBetOpportunity;

    /**
     * Creates context for one decision, not an entire hand.
     * A c-bet opportunity means the final preflop aggressor can legally open the
     * flop betting. Facing another opening bet or being all-in excludes that flag.
     * A faced-c-bet opportunity means this player faces an actual flop c-bet and
     * has a legal response decision. Raise level is authoritative engine context.
     *
     * @throws IllegalArgumentException if fields are null or logically incompatible
     */
    public StatsContext(PreflopRaiseType preflopRaiseType, boolean threeBetOpportunity,
            boolean flopCBetOpportunity, boolean facedFlopCBetOpportunity) {
        if (preflopRaiseType == null) {
            throw new IllegalArgumentException("Preflop raise type must not be null.");
        }

        if (flopCBetOpportunity && facedFlopCBetOpportunity) {
            throw new IllegalArgumentException("Opening and facing a flop c-bet are incompatible.");
        }

        boolean flopContext = flopCBetOpportunity || facedFlopCBetOpportunity;
        if (flopContext && (threeBetOpportunity || preflopRaiseType != PreflopRaiseType.NONE)) {
            throw new IllegalArgumentException("Preflop and flop context cannot be combined.");
        }

        if (preflopRaiseType == PreflopRaiseType.THREE_BET && !threeBetOpportunity) {
            throw new IllegalArgumentException("A three-bet requires an eligible opportunity.");
        }

        if (threeBetOpportunity && preflopRaiseType != PreflopRaiseType.NONE
                && preflopRaiseType != PreflopRaiseType.THREE_BET) {
            throw new IllegalArgumentException("This raise is not a three-bet decision.");
        }

        this.preflopRaiseType = preflopRaiseType;
        this.threeBetOpportunity = threeBetOpportunity;
        this.flopCBetOpportunity = flopCBetOpportunity;
        this.facedFlopCBetOpportunity = facedFlopCBetOpportunity;
    }

    PreflopRaiseType getPreflopRaiseType() {
        return preflopRaiseType;
    }

    boolean hasThreeBetOpportunity() {
        return threeBetOpportunity;
    }

    boolean hasFlopCBetOpportunity() {
        return flopCBetOpportunity;
    }

    boolean hasFacedFlopCBetOpportunity() {
        return facedFlopCBetOpportunity;
    }
}
