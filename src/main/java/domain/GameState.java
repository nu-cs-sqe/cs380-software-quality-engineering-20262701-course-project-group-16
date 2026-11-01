package domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable six-seat table snapshot with per-hand chip commitments.
 * Role selection, action legality, and chip transfers belong to game logic.
 */
public final class GameState {

    private static final int SEAT_COUNT = 6;

    private final List<Player> players;
    private final int dealerSeat;
    private final int smallBlindSeat;
    private final int bigBlindSeat;
    private final int currentActorSeat;
    private final long[] committedChips;
    private final long pot;
    private final long totalChips;

    /**
     * Creates a snapshot without changing the supplied players or commitments.
     *
     * @param players six non-null players with unique IDs and seats, in any order
     * @param dealerSeat recorded dealer seat, from 0 through 5
     * @param smallBlindSeat recorded small-blind seat, from 0 through 5
     * @param bigBlindSeat recorded big-blind seat, different from the small blind
     * @param currentActorSeat recorded current actor's seat, from 0 through 5
     * @param committedChips six nonnegative hand commitments indexed by seat
     * @throws IllegalArgumentException if the snapshot is structurally invalid
     *         or its chip totals exceed the supported long range
     */
    public GameState(List<Player> players, int dealerSeat, int smallBlindSeat,
            int bigBlindSeat, int currentActorSeat, long[] committedChips) {
        this.players = copyPlayers(players);
        requireValidSeat(dealerSeat);
        requireValidSeat(smallBlindSeat);
        requireValidSeat(bigBlindSeat);
        requireValidSeat(currentActorSeat);
        if (smallBlindSeat == bigBlindSeat) {
            throw new IllegalArgumentException("Small and big blinds must use different seats.");
        }
        this.dealerSeat = dealerSeat;
        this.smallBlindSeat = smallBlindSeat;
        this.bigBlindSeat = bigBlindSeat;
        this.currentActorSeat = currentActorSeat;
        this.committedChips = copyCommitments(committedChips);
        try {
            this.pot = calculatePot(this.committedChips);
            this.totalChips = calculateTotalChips(this.players, this.pot);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Chip totals exceed the supported long range.",
                    exception);
        }
    }

    /** @return an unmodifiable list of immutable players in seat order */
    public List<Player> getPlayers() {
        return players;
    }

    /** @return the recorded dealer seat */
    public int getDealerSeat() {
        return dealerSeat;
    }

    /** @return the recorded small-blind seat */
    public int getSmallBlindSeat() {
        return smallBlindSeat;
    }

    /** @return the recorded big-blind seat */
    public int getBigBlindSeat() {
        return bigBlindSeat;
    }

    /** @return the recorded current actor's seat */
    public int getCurrentActorSeat() {
        return currentActorSeat;
    }

    /**
     * Returns a seat's commitment for the current hand.
     *
     * @param seat seat number, from 0 through 5
     * @return the nonnegative commitment at that seat
     * @throws IllegalArgumentException if the seat is outside 0 through 5
     */
    public long getCommittedChips(int seat) {
        requireValidSeat(seat);
        return committedChips[seat];
    }

    /** @return the sum of hand commitments, excluding remaining player stacks */
    public long getPot() {
        return pot;
    }

    /** @return remaining player stacks plus the pot, counting commitments once */
    public long getTotalChips() {
        return totalChips;
    }

    private static List<Player> copyPlayers(List<Player> players) {
        if (players == null || players.size() != SEAT_COUNT) {
            throw new IllegalArgumentException("Exactly six players are required.");
        }
        Player[] playersBySeat = new Player[SEAT_COUNT];
        Set<String> playerIds = new HashSet<>();
        for (Player player : players) {
            if (player == null) {
                throw new IllegalArgumentException("Players must not be null.");
            }
            int seat = player.getSeat();
            if (playersBySeat[seat] != null || !playerIds.add(player.getId())) {
                throw new IllegalArgumentException("Player seats and IDs must be unique.");
            }
            playersBySeat[seat] = player;
        }
        return List.of(playersBySeat);
    }

    private static void requireValidSeat(int seat) {
        if (seat < 0 || seat >= SEAT_COUNT) {
            throw new IllegalArgumentException("Seat must be between 0 and 5.");
        }
    }

    private static long[] copyCommitments(long[] committedChips) {
        if (committedChips == null || committedChips.length != SEAT_COUNT) {
            throw new IllegalArgumentException("Exactly six chip commitments are required.");
        }
        long[] snapshot = committedChips.clone();
        for (long amount : snapshot) {
            if (amount < 0) {
                throw new IllegalArgumentException("Committed chips must not be negative.");
            }
        }
        return snapshot;
    }

    private static long calculatePot(long[] committedChips) {
        long total = 0L;
        for (long amount : committedChips) {
            total = Math.addExact(total, amount);
        }
        return total;
    }

    private static long calculateTotalChips(List<Player> players, long pot) {
        long total = pot;
        for (Player player : players) {
            total = Math.addExact(total, player.getChipStack());
        }
        return total;
    }
}
