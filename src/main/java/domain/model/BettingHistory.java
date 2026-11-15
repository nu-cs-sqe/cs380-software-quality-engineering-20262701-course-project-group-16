package domain.model;

import java.util.Optional;
import java.util.OptionalLong;

/** Immutable engine provenance, with explicit aggression absence and exact raise counts. */
public final class BettingHistory {
    private final Optional<String> finalPreflopAggressorId;
    private final long flopCBetAmount;
    private final long preflopRaiseCount;

    /** Creates an unraised hand with no recorded flop continuation bet. */
    public BettingHistory() {
        this(Optional.empty(), 0);
    }

    /** Creates history for a known preflop aggressor. */
    public BettingHistory(String aggressor, long flopCBetAmount) {
        this(Optional.of(aggressor), flopCBetAmount);
    }

    /** Creates history for a known aggressor and exact accepted raise count. */
    public BettingHistory(String aggressor, long flopCBetAmount, long preflopRaiseCount) {
        this(Optional.of(aggressor), flopCBetAmount, preflopRaiseCount);
    }

    public BettingHistory(Optional<String> aggressor, long flopCBetAmount) {
        this(aggressor, flopCBetAmount, aggressor.isPresent() ? 1 : 0);
    }

    public BettingHistory(Optional<String> aggressor, long flopCBetAmount,
            long preflopRaiseCount) {
        requireAggression(aggressor, flopCBetAmount, preflopRaiseCount);
        finalPreflopAggressorId = aggressor;
        this.flopCBetAmount = flopCBetAmount;
        this.preflopRaiseCount = preflopRaiseCount;
    }

    private static void requireAggression(Optional<String> aggressor, long cBet, long raises) {
        if (aggressor.filter(String::isBlank).isPresent() || cBet < 0
                || (cBet > 0 && aggressor.isEmpty())) {
            throw new IllegalArgumentException(
                    "A c-bet needs an aggressor; IDs and amounts must be valid.");
        }
        if (raises < 0 || (raises == 0) != aggressor.isEmpty()) {
            throw new IllegalArgumentException("A positive raise count requires an aggressor.");
        }
    }

    public long getPreflopRaiseCount() { return preflopRaiseCount; }

    BettingHistory after(AcceptedAction action) {
        if (action.getStreet() == BettingStreet.PREFLOP
                && action.getType() == ResolvedActionType.RAISE) {
            return new BettingHistory(Optional.of(action.getPlayerId()), 0,
                    Math.addExact(preflopRaiseCount, 1));
        }
        if (action.getStreet() == BettingStreet.FLOP && action.getType() == ResolvedActionType.BET
                && action.getStatsContext().hasFlopCBetOpportunity()) {
            return new BettingHistory(finalPreflopAggressorId,
                    action.getRoundContributionAfter(), preflopRaiseCount);
        }
        return this;
    }

    public Optional<String> getFinalPreflopAggressorId() { return finalPreflopAggressorId; }

    public OptionalLong getFlopCBetAmount() {
        return flopCBetAmount == 0 ? OptionalLong.empty() : OptionalLong.of(flopCBetAmount);
    }
}
