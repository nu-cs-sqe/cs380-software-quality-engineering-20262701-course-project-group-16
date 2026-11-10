package bot;

import java.util.Map;

/** Unrounded expected shares and payout; equity is normalized over hero-eligible pots. */
public final class EquityEstimate {
    private final int simulations;
    private final Map<String, Double> potShares;
    private final double equity;
    private final double expectedPayout;

    // Only the validated calculation boundary publishes results.
    EquityEstimate(int simulations, Map<String, Double> potShares, double equity, double payout) {
        this.simulations = simulations;
        this.potShares = Map.copyOf(potShares);
        this.equity = equity;
        expectedPayout = payout;
    }

    public int getSimulations() { return simulations; }
    public Map<String, Double> getPotShares() { return potShares; }
    public double getEquity() { return equity; }
    public double getExpectedPayout() { return expectedPayout; }
}
