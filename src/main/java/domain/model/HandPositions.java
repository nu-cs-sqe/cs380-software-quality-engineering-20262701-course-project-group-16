package domain.model;

import static domain.model.GameConfig.SEAT_COUNT;

import java.util.Comparator;
import java.util.Set;

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

    /** Last active seat in street order; a lone player has no response-position advantage. */
    public boolean isInPosition(int seat, Set<Integer> activeSeats, boolean preflop) {
        requireDecisionSeats(seat, activeSeats);
        if (activeSeats.size() < 2) {
            return false;
        }
        int anchor = preflop ? bigBlindSeat : dealerSeat;
        return activeSeats.stream().max(Comparator.comparingInt(
                actor -> Math.floorMod(actor - anchor - 1, SEAT_COUNT))).orElseThrow() == seat;
    }

    private static void requireDecisionSeats(int seat, Set<Integer> activeSeats) {
        if (seat < 0 || seat >= SEAT_COUNT
                || activeSeats.stream().anyMatch(actor -> actor < 0 || actor >= SEAT_COUNT)) {
            throw new IllegalArgumentException("Decision seats must belong to the table.");
        }
    }

    public int getDealerSeat() { return dealerSeat; }
    public int getSmallBlindSeat() { return smallBlindSeat; }
    public int getBigBlindSeat() { return bigBlindSeat; }
}
