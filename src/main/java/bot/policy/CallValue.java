package bot.policy;

import java.util.OptionalDouble;

/** Public call valuation; pots exclude refunds and ineligible layers. */
public final class CallValue {
    private final double equity;
    private final long eligiblePotAfterCall;
    private final long callRefund;

    public CallValue(double equity, long eligiblePotAfterCall, long callRefund) {
        requireValues(equity, eligiblePotAfterCall, callRefund);
        this.equity = equity;
        this.eligiblePotAfterCall = eligiblePotAfterCall;
        this.callRefund = callRefund;
    }

    private static void requireValues(double equity, long pot, long refund) {
        if (!Double.isFinite(equity) || equity < 0 || equity > 1 || pot < 0 || refund < 0) {
            throw new IllegalArgumentException("Use finite equity in [0,1] and nonnegative chips.");
        }
    }

    public double getEquity() { return equity; }
    public long getEligiblePotAfterCall() { return eligiblePotAfterCall; }
    public long getCallRefund() { return callRefund; }

    public long riskedChips(long grossCallCost) {
        if (grossCallCost < callRefund || grossCallCost - callRefund > eligiblePotAfterCall) {
            throw new IllegalArgumentException("Risked call chips must fit the eligible pot.");
        }
        return grossCallCost - callRefund;
    }

    /** Empty only for a cost-free decision with no awardable pot. */
    public OptionalDouble breakEvenEquity(long grossCallCost) {
        long risk = riskedChips(grossCallCost);
        return eligiblePotAfterCall == 0 ? OptionalDouble.empty()
                : OptionalDouble.of(risk / (double) eligiblePotAfterCall);
    }
}
