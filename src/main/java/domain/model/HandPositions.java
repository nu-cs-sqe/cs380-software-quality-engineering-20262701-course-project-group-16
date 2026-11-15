package domain.model;

import static domain.model.GameConfig.SEAT_COUNT;

/** Immutable positions for one hand. In heads-up play the dealer is also the small blind. */
public final class HandPositions {
    private final int dealerSeat;
    private final int smallBlindSeat;
    private final int bigBlindSeat;

    /** @throws IllegalArgumentException for out-of-range or incompatible positions */
    public HandPositions(int dealer, int smallBlind, int bigBlind) {
        requirePositions(dealer, smallBlind, bigBlind);
        this.dealerSeat = dealer;
        this.smallBlindSeat = smallBlind;
        this.bigBlindSeat = bigBlind;
    }

    private static void requirePositions(int dealer, int smallBlind, int bigBlind) {
        if (dealer < 0 || dealer >= SEAT_COUNT
                || smallBlind < 0 || smallBlind >= SEAT_COUNT
                || bigBlind < 0 || bigBlind >= SEAT_COUNT
                || bigBlind == smallBlind || bigBlind == dealer) {
            throw new IllegalArgumentException("Invalid six-seat hand positions.");
        }
    }

    public int getDealerSeat() { return dealerSeat; }
    public int getSmallBlindSeat() { return smallBlindSeat; }
    public int getBigBlindSeat() { return bigBlindSeat; }
}
