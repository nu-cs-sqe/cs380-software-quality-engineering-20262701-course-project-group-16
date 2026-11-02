package domain;
/** Immutable positions for one hand. In heads-up play the dealer is also the small blind. */
public final class HandPositions {
    private final int dealerSeat;
    private final int smallBlindSeat;
    private final int bigBlindSeat;

    /** @throws IllegalArgumentException for out-of-range or incompatible positions */
    public HandPositions(int dealer, int smallBlind, int bigBlind) {
        if (dealer < 0 || dealer >= 6 || smallBlind < 0 || smallBlind >= 6
                || bigBlind < 0 || bigBlind >= 6 || bigBlind == smallBlind || bigBlind == dealer) {
            throw new IllegalArgumentException("Invalid six-seat hand positions.");
        }
        this.dealerSeat = dealer;
        this.smallBlindSeat = smallBlind;
        this.bigBlindSeat = bigBlind;
    }

    public int getDealerSeat() { return dealerSeat; }
    public int getSmallBlindSeat() { return smallBlindSeat; }
    public int getBigBlindSeat() { return bigBlindSeat; }
}
