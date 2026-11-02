package domain;

/** Immutable player identity, seat, control type, and chip stack. */
public final class Player {

    private static final int SEAT_COUNT = 6;

    private final String id;
    private final int seat;
    private final PlayerType type;
    private final long chipStack;

    /**
     * Creates a player. Uniqueness of IDs across a table is a setup responsibility.
     *
     * @param id non-null, nonblank identifier, retained without normalization
     * @param seat zero-based seat in the six-seat table
     * @param type human or bot control type
     * @param chipStack nonnegative count of virtual chips; zero is allowed
     * @throws IllegalArgumentException if any argument violates these constraints
     */
    public Player(String id, int seat, PlayerType type, long chipStack) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Player ID must not be null or blank.");
        }
        if (seat < 0 || seat >= SEAT_COUNT) {
            throw new IllegalArgumentException("Seat must be between 0 and 5.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Player type must not be null.");
        }
        if (chipStack < 0) {
            throw new IllegalArgumentException("Chip stack must not be negative.");
        }
        this.id = id;
        this.seat = seat;
        this.type = type;
        this.chipStack = chipStack;
    }

    /** @return the player ID as supplied to the constructor */
    public String getId() {
        return id;
    }

    /** @return the seat number, from 0 through 5 */
    public int getSeat() {
        return seat;
    }

    /** @return whether this player is controlled by a human or a bot */
    public PlayerType getType() {
        return type;
    }

    /** @return the nonnegative chip stack recorded for this player */
    public long getChipStack() {
        return chipStack;
    }
}
