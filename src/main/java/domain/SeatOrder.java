package domain;
import java.util.List;
import java.util.OptionalInt;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Stable clockwise seats. Position selection does not decide whether a betting round is over. */
public final class SeatOrder {
    private static final int SEAT_COUNT = 6;
    private final PlayerState[] bySeat = new PlayerState[SEAT_COUNT];

    /** @throws IllegalArgumentException unless all six seats and IDs are present exactly once */
    public SeatOrder(List<PlayerState> players) {
        if (players == null || players.size() != SEAT_COUNT) {
            throw new IllegalArgumentException("Six stable seat records are required.");
        }
        Set<String> ids = new HashSet<>();
        for (PlayerState state : players) {
            if (state == null || bySeat[state.getPlayer().getSeat()] != null
                    || !ids.add(state.getPlayer().getId())) {
                throw new IllegalArgumentException("Players, seats and IDs must be unique and non-null.");
            }
            bySeat[state.getPlayer().getSeat()] = state;
        }
    }

    /**
     * Assigns blinds among remaining players; folded/all-in seats retain their hand positions.
     * @throws IllegalArgumentException if the dealer seat is invalid or eliminated
     * @throws IllegalStateException if fewer than two players remain
     */
    public HandPositions positionsFor(int dealer) {
        requireSeat(dealer);
        long remaining = java.util.Arrays.stream(bySeat)
                .filter(state -> state.getStatus() != PlayerStatus.ELIMINATED).count();
        if (remaining < 2) {
            throw new IllegalStateException("A hand requires at least two remaining players.");
        }
        if (bySeat[dealer].getStatus() == PlayerStatus.ELIMINATED) {
            throw new IllegalArgumentException("The dealer must be a remaining player.");
        }
        int smallBlind = remaining == 2 ? dealer : nextDealerAfter(dealer).orElseThrow();
        return new HandPositions(dealer, smallBlind, nextDealerAfter(smallBlind).orElseThrow());
    }

    /** Moving-button policy; an empty result means no player remains. */
    public OptionalInt nextDealerAfter(int seat) {
        return nextAfter(seat, state -> state.getStatus() != PlayerStatus.ELIMINATED);
    }

    /** Skips eliminated, folded and all-in players; empty means nobody can act. */
    public OptionalInt nextActorAfter(int seat) {
        return nextAfter(seat, PlayerState::canAct);
    }

    public OptionalInt firstPreflopActor(int dealer) {
        return nextActorAfter(positionsFor(dealer).getBigBlindSeat());
    }

    public OptionalInt firstPostflopActor(int dealer) {
        positionsFor(dealer);
        return nextActorAfter(dealer);
    }

    private OptionalInt nextAfter(int seat, Predicate<PlayerState> eligible) {
        requireSeat(seat);
        for (int distance = 1; distance <= SEAT_COUNT; distance++) {
            int next = (seat + distance) % SEAT_COUNT;
            if (eligible.test(bySeat[next])) {
                return OptionalInt.of(next);
            }
        }
        return OptionalInt.empty();
    }

    private static void requireSeat(int seat) {
        if (seat < 0 || seat >= SEAT_COUNT) {
            throw new IllegalArgumentException("Seat must be between zero and five.");
        }
    }
}
