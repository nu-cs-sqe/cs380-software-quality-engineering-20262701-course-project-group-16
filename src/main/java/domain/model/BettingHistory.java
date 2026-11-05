package domain.model;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Immutable engine provenance, not a history inferred from the current bet size.
 * An actual flop c-bettor is the final preflop aggressor; zero means no c-bet recorded.
 */
public final class BettingHistory {
    private final String finalPreflopAggressorId;
    private final long flopCBetAmount;

    /** Creates an unraised hand with no recorded flop continuation bet. */
    public BettingHistory() {
        this(null, 0);
    }

    /**
     * Records authoritative engine history; GameState validates it against the hand.
     * @param finalPreflopAggressorId null for no preflop aggressor
     * @param flopCBetAmount original flop c-bet round target; zero if none occurred
     * @throws IllegalArgumentException for blank IDs, negative amounts or a c-bet without an aggressor
     */
    public BettingHistory(String finalPreflopAggressorId, long flopCBetAmount) {
        if ((finalPreflopAggressorId != null && finalPreflopAggressorId.isBlank())
                || flopCBetAmount < 0 || (flopCBetAmount > 0 && finalPreflopAggressorId == null)) {
            throw new IllegalArgumentException("A c-bet needs an aggressor; IDs and amounts must be valid.");
        }
        this.finalPreflopAggressorId = finalPreflopAggressorId;
        this.flopCBetAmount = flopCBetAmount;
    }

    public Optional<String> getFinalPreflopAggressorId() {
        return Optional.ofNullable(finalPreflopAggressorId);
    }

    public OptionalLong getFlopCBetAmount() {
        return flopCBetAmount == 0 ? OptionalLong.empty() : OptionalLong.of(flopCBetAmount);
    }
}
