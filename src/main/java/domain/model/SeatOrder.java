package domain.model;

import static domain.model.GameConfig.SEAT_COUNT;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.OptionalInt;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Stable clockwise seats. Position selection does not decide whether a betting round is over. */
public final class SeatOrder {
    private final List<PlayerState> bySeat;

    /** @throws IllegalArgumentException unless all six seats and IDs are present exactly once */
    public SeatOrder(List<PlayerState> players) {
        bySeat = indexPlayers(players);
    }

    private static List<PlayerState> indexPlayers(List<PlayerState> players) {
        if (players.size() != SEAT_COUNT) {
            throw new IllegalArgumentException("Six stable seat records are required.");
        }
        Set<String> ids = new HashSet<>();
        Set<Integer> seats = new HashSet<>();
        for (PlayerState state : players) {
            if (!seats.add(state.getPlayer().getSeat())
                    || !ids.add(state.getPlayer().getId())) {
                throw new IllegalArgumentException(
                    "Seats and player IDs must be unique.");
            }
        }
        List<PlayerState> indexed = new ArrayList<>(players);
        indexed.sort(Comparator.comparingInt(state -> state.getPlayer().getSeat()));
        return List.copyOf(indexed);
    }

    /**
     * Assigns blinds among remaining players; folded/all-in seats retain their hand positions.
     * @throws IllegalArgumentException if the dealer seat is invalid or inactive for the hand
     * @throws IllegalStateException if fewer than two players remain
     */
    public HandPositions positionsFor(int dealer) {
        requireSeat(dealer);
        long remaining = bySeat.stream().filter(PlayerState::isDealtSeat).count();
        if (remaining < 2) {
            throw new IllegalStateException("A hand requires at least two remaining players.");
        }
        if (!bySeat.get(dealer).isDealtSeat()) {
            throw new IllegalArgumentException("The dealer must be a remaining player.");
        }
        int smallBlind = remaining == 2 ? dealer : nextDealerAfter(dealer).orElseThrow();
        return new HandPositions(dealer, smallBlind, nextDealerAfter(smallBlind).orElseThrow());
    }

    /** Moving-button policy; an empty result means no player remains. */
    public OptionalInt nextDealerAfter(int seat) {
        return nextAfter(seat, PlayerState::isDealtSeat);
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
            if (eligible.test(bySeat.get(next))) {
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
