package application;

/** Account-table funding policy; upper limits come only from available funds and safe totals. */
public final class BuyInLimits {
    public static final long MINIMUM = 400;
    public static final long DEFAULT = 1000;

    private BuyInLimits() { }

    /** Returns the standard buy-in for a funded account. */
    public static long defaultAmount(long balance) {
        long amount = Math.min(DEFAULT, balance);
        requireAmount(amount, balance);
        return amount;
    }

    static void requireAmount(long amount, long balance) {
        if (amount < MINIMUM || amount > balance) {
            throw new IllegalArgumentException("Buy-in must be at least 400 and fit the balance.");
        }
    }
}
