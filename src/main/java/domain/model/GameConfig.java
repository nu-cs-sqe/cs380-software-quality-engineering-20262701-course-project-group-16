package domain.model;
/** Immutable match settings. Seats, antes and blind progression are fixed in this version. */
public final class GameConfig {
    public static final int SEAT_COUNT = 6;
    private static final long DEFAULT_STARTING_CHIPS = 1000;
    private static final long DEFAULT_SMALL_BLIND = 10;
    private static final long DEFAULT_BIG_BLIND = 20;
    private final long startingChips;
    private final long smallBlind;
    private final long bigBlind;
    private final long initialTotalChips;

    /** Creates the agreed 1,000-chip, 10/20-blind configuration. */
    public GameConfig() {
        this(DEFAULT_STARTING_CHIPS, DEFAULT_SMALL_BLIND, DEFAULT_BIG_BLIND);
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
        this.initialTotalChips = startingChips * SEAT_COUNT;
    }

    private GameConfig(GameConfig stakes, long tableTotal) {
        if (tableTotal <= 0) {
            throw new IllegalArgumentException("The table requires positive funded chips.");
        }
        this.startingChips = stakes.startingChips;
        this.smallBlind = stakes.smallBlind;
        this.bigBlind = stakes.bigBlind;
        this.initialTotalChips = tableTotal;
    }

    /** Retains stakes while setting the funded total for the next account-table hand. */
    public GameConfig withTableTotal(long chips) {
        return new GameConfig(this, chips);
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
    public long getInitialTotalChips() { return initialTotalChips; }
}
