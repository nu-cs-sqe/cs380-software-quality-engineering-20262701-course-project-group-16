package domain.model;
/** Immutable match settings. Seats, antes and blind progression are fixed in this version. */
public final class GameConfig {
    private static final int SEAT_COUNT = 6;
    private final long startingChips;
    private final long smallBlind;
    private final long bigBlind;

    /** Creates the agreed 1,000-chip, 10/20-blind configuration. */
    public GameConfig() {
        this(1000, 10, 20);
    }

    /**
     * Creates integer-chip settings with 0 < small blind < big blind <= starting chips.
     * @throws IllegalArgumentException for invalid settings or a six-stack total overflow
     */
    public GameConfig(long startingChips, long smallBlind, long bigBlind) {
        validateSettings(startingChips, smallBlind, bigBlind);
        this.startingChips = startingChips;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
    }

    private static void validateSettings(long startingChips, long smallBlind, long bigBlind) {
        if (startingChips <= 0 || startingChips > Long.MAX_VALUE / SEAT_COUNT
                || smallBlind <= 0 || smallBlind >= bigBlind || bigBlind > startingChips) {
            throw new IllegalArgumentException(
                "Require positive blinds below the starting stack and a safe total.");
        }
    }

    public long getStartingChips() { return startingChips; }
    public long getSmallBlind() { return smallBlind; }
    public long getBigBlind() { return bigBlind; }
    public int getSeatCount() { return SEAT_COUNT; }
    public long getAnte() { return 0; }
    public boolean isBlindIncreaseEnabled() { return false; }
    public long getInitialTotalChips() { return startingChips * SEAT_COUNT; }
}
