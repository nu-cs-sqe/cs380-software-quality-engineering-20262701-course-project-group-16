package domain.model;

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
     * @throws IllegalArgumentException if fields are logically incompatible
     */
    public StatsContext(PreflopRaiseType preflopRaiseType, boolean threeBetOpportunity,
            boolean flopCBetOpportunity, boolean facedFlopCBetOpportunity) {

        requireExclusiveFlopRole(flopCBetOpportunity, facedFlopCBetOpportunity);
        requireSingleStreetContext(preflopRaiseType, threeBetOpportunity,
                flopCBetOpportunity || facedFlopCBetOpportunity);
        requireThreeBetClassification(preflopRaiseType, threeBetOpportunity);
        this.preflopRaiseType = preflopRaiseType;
        this.threeBetOpportunity = threeBetOpportunity;
        this.flopCBetOpportunity = flopCBetOpportunity;
        this.facedFlopCBetOpportunity = facedFlopCBetOpportunity;
    }

    private static void requireExclusiveFlopRole(boolean opening, boolean facing) {
        if (opening && facing) {
            throw new IllegalArgumentException("Opening and facing a flop c-bet are incompatible.");
        }
    }

    private static void requireSingleStreetContext(PreflopRaiseType raise,
            boolean threeBetOpportunity, boolean flopContext) {
        if (flopContext && (threeBetOpportunity || raise != PreflopRaiseType.NONE)) {
            throw new IllegalArgumentException("Preflop and flop context cannot be combined.");
        }
    }

    private static void requireThreeBetClassification(PreflopRaiseType raise,
            boolean opportunity) {
        if (raise == PreflopRaiseType.THREE_BET && !opportunity) {
            throw new IllegalArgumentException("A three-bet requires an eligible opportunity.");
        }
        if (opportunity && raise != PreflopRaiseType.NONE && raise != PreflopRaiseType.THREE_BET) {
            throw new IllegalArgumentException("This raise is not a three-bet decision.");
        }
    }

    public PreflopRaiseType getPreflopRaiseType() {
        return preflopRaiseType;
    }

    public boolean hasThreeBetOpportunity() {
        return threeBetOpportunity;
    }

    public boolean hasFlopCBetOpportunity() {
        return flopCBetOpportunity;
    }

    public boolean hasFacedFlopCBetOpportunity() {
        return facedFlopCBetOpportunity;
    }
}
