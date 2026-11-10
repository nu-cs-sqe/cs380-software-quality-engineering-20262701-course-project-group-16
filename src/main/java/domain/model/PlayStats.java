package domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Completed-hand statistics for one player/profile, fed by an engine event thread.
 * Pending actions never affect lifetime results. Instances are not thread-safe.
 * Percentage values are unrounded; unavailable denominators use OptionalDouble.empty().
 */
public final class PlayStats {
    private final Set<String> completedHandIds = new HashSet<>();
    private final List<StatsAction> pendingActions = new ArrayList<>();
    private Optional<String> activeHandId = Optional.empty();
    private boolean dealtIn;
    private long[] totals = new long[Statistic.values().length];

    /** Creates empty statistics with no active hand. */
    public PlayStats() {
    }

    /** Independent transactional copy, including pending observations and duplicate-ID history. */
    public PlayStats copy() {
        PlayStats copy = new PlayStats();
        copy.completedHandIds.addAll(completedHandIds);
        copy.pendingActions.addAll(pendingActions);
        copy.activeHandId = activeHandId;
        copy.dealtIn = dealtIn;
        copy.totals = totals.clone();
        return copy;
    }

    /** Exports completed data only, even while observations for another hand are pending. */
    public CompletedStatistics completedSnapshot() {
        java.util.Map<Statistic, Long> counts = new java.util.EnumMap<>(Statistic.class);
        for (Statistic counter : Statistic.values()) {
            counts.put(counter, count(counter));
        }
        return new CompletedStatistics(counts, completedHandIds);
    }

    /** Restores totals and deduplication history without restoring an unfinished hand. */
    public static PlayStats fromCompleted(CompletedStatistics completed) {
        if (completed == null) {
            throw new IllegalArgumentException("Completed statistics are required.");
        }
        PlayStats restored = new PlayStats();
        restored.completedHandIds.addAll(completed.getCompletedHandIds());
        for (Statistic counter : Statistic.values()) {
            restored.totals[counter.ordinal()] = completed.getCounts().get(counter);
        }
        return restored;
    }

    /**
     * Begins a hand without changing lifetime totals.
     *
     * @param handId nonblank, globally unique across matches for this instance
     * @param dealtIn whether this player was actually dealt into the hand
     * @throws IllegalArgumentException if the ID is null or blank
     * @throws IllegalStateException if another hand is active or the ID was completed
     */
    public void beginHand(String handId, boolean dealtIn) {
        if (activeHandId.isPresent()) {
            throw new IllegalStateException("A hand is already active.");
        }
        if (handId == null || handId.isBlank()) {
            throw new IllegalArgumentException("Hand ID must not be null or blank.");
        }
        if (completedHandIds.contains(handId)) {
            throw new IllegalStateException("This hand has already been completed.");
        }
        activeHandId = Optional.of(handId);
        this.dealtIn = dealtIn;
    }

    /**
     * Records one engine observation; rejected actions are ignored.
     *
     * @param action non-null immutable observation, delivered once per engine action
     * @throws IllegalStateException if no hand is active
     * @throws IllegalArgumentException if input contradicts participation or action order
     */
    public void recordAction(StatsAction action) {
        requireActiveHand();
        if (action == null) {
            throw new IllegalArgumentException("Action must not be null.");
        }
        if (!action.isAccepted()) {
            return;
        }
        if (!dealtIn) {
            throw new IllegalArgumentException(
                "A nonparticipant cannot contribute accepted actions.");
        }
        if (!pendingActions.isEmpty()) {
            StatsAction previous = pendingActions.get(pendingActions.size() - 1);
            if (previous.getType() == ResolvedActionType.FOLD
                    || action.getStreet().compareTo(previous.getStreet()) < 0) {
                throw new IllegalArgumentException(
                    "Action contradicts the accepted hand lifecycle.");
            }
        }
        for (StatsAction previous : pendingActions) {
            StatsContext before = previous.getContext();
            StatsContext next = action.getContext();
            if ((before.hasFlopCBetOpportunity() && next.hasFacedFlopCBetOpportunity())
                    || (before.hasFacedFlopCBetOpportunity() && next.hasFlopCBetOpportunity())) {
                throw new IllegalArgumentException(
                    "A player cannot both open and face a flop c-bet.");
            }
        }
        pendingActions.add(action);
    }

    /**
     * Atomically aggregates a completed hand. Invalid input leaves the active hand
     * and existing totals intact so the engine can supply corrected facts.
     *
     * @param handId ID of the active hand
     * @param sawFlop player remained in the hand when the flop was dealt, including all-in
     * @param reachedShowdown player remained in the hand through showdown
     * @param contestedShowdownPayout sum of payouts from contested showdown pots only
     * @param uncalledBetRefund separate refund amount, never counted as W$SD success
     * @throws IllegalStateException if no hand is active
     * @throws IllegalArgumentException if the ID or completion facts are inconsistent
     * @throws ArithmeticException if lifetime counters would overflow; nothing is committed
     */
    public void completeHand(String handId, boolean sawFlop, boolean reachedShowdown,
            long contestedShowdownPayout, long uncalledBetRefund) {
        String activeId = requireActiveHand();
        if (!activeId.equals(handId)) {
            throw new IllegalArgumentException("Completion ID does not match the active hand.");
        }
        validateCompletion(sawFlop, reachedShowdown, contestedShowdownPayout, uncalledBetRefund);
        long[] delta = summarizeHand(sawFlop, reachedShowdown, contestedShowdownPayout);
        long[] nextTotals = totals.clone();
        for (int index = 0; index < nextTotals.length; index++) {
            nextTotals[index] = Math.addExact(nextTotals[index], delta[index]);
        }
        completedHandIds.add(activeId);
        totals = nextTotals;
        activeHandId = Optional.empty();
        dealtIn = false;
        pendingActions.clear();
    }

    /** @return completed hands in which this player was dealt in */
    public long getHandsPlayed() {
        return count(Statistic.HANDS);
    }

    /** @return 100 * voluntary preflop hands / hands played, or unavailable */
    public OptionalDouble getVpipPercent() {
        return percentage(count(Statistic.VPIP), count(Statistic.HANDS));
    }

    /** @return 100 * preflop raise hands / hands played, or unavailable */
    public OptionalDouble getPfrPercent() {
        return percentage(count(Statistic.PFR), count(Statistic.HANDS));
    }

    /** @return the VPIP minus PFR gap in percentage points, or unavailable */
    public OptionalDouble getVpipPfrGapPercentagePoints() {
        return percentage(count(Statistic.VPIP) - count(Statistic.PFR), count(Statistic.HANDS));
    }

    /** @return 100 * three-bet hands / eligible opportunity hands, or unavailable */
    public OptionalDouble getThreeBetPercent() {
        return percentage(count(Statistic.THREE_BET), count(Statistic.THREE_BET_OPPORTUNITY));
    }

    /** @return postflop (bets + raises) / calls; unavailable whenever calls are zero */
    public OptionalDouble getAggressionFactor() {
        long calls = count(Statistic.POSTFLOP_CALLS);
        return calls == 0 ? OptionalDouble.empty()
                : OptionalDouble.of(aggressiveActions() / calls);
    }

    /** @return postflop aggression frequency as a percentage, excluding checks */
    public OptionalDouble getAggressionFrequencyPercent() {
        double aggressive = aggressiveActions();
        double decisions = aggressive + count(Statistic.POSTFLOP_CALLS)
                + count(Statistic.POSTFLOP_FOLDS);
        return percentage(aggressive, decisions);
    }

    /** @return flop c-bet hands / eligible opportunity hands, as a percentage */
    public OptionalDouble getFlopCBetPercent() {
        return percentage(count(Statistic.FLOP_CBET), count(Statistic.FLOP_CBET_OPPORTUNITY));
    }

    /** @return folded-to-c-bet hands / eligible faced-c-bet hands, as a percentage */
    public OptionalDouble getFoldToFlopCBetPercent() {
        return percentage(count(Statistic.FOLD_TO_CBET), count(Statistic.FACED_CBET));
    }

    /** @return showdown hands / saw-flop hands, as a percentage */
    public OptionalDouble getWtsdPercent() {
        return percentage(count(Statistic.SHOWDOWN), count(Statistic.SAW_FLOP));
    }

    /** @return positive contested-payout hands / showdown hands, as a percentage */
    public OptionalDouble getWonAtShowdownPercent() {
        return percentage(count(Statistic.SHOWDOWN_PAYOUT), count(Statistic.SHOWDOWN));
    }

    private void validateCompletion(boolean sawFlop, boolean reachedShowdown,
            long contestedShowdownPayout, long uncalledBetRefund) {
        if (contestedShowdownPayout < 0 || uncalledBetRefund < 0) {
            throw new IllegalArgumentException("Payouts and refunds must not be negative.");
        }
        if (!dealtIn && (sawFlop || reachedShowdown || contestedShowdownPayout != 0
                || uncalledBetRefund != 0)) {
            throw new IllegalArgumentException("A nonparticipant cannot have hand outcomes.");
        }
        if (reachedShowdown && !sawFlop) {
            throw new IllegalArgumentException("Showdown requires flop participation.");
        }
        if (contestedShowdownPayout > 0 && !reachedShowdown) {
            throw new IllegalArgumentException("A contested showdown payout requires showdown.");
        }
        for (StatsAction action : pendingActions) {
            boolean preflop = action.getStreet() == BettingStreet.PREFLOP;
            if (!preflop && !sawFlop) {
                throw new IllegalArgumentException("Postflop actions require seeing the flop.");
            }
            if (action.getType() == ResolvedActionType.FOLD
                    && (reachedShowdown || (preflop && sawFlop))) {
                throw new IllegalArgumentException("Folded-hand facts contradict participation.");
            }
        }
    }

    private long[] summarizeHand(boolean sawFlop, boolean reachedShowdown,
            long contestedShowdownPayout) {
        long[] delta = new long[Statistic.values().length];
        if (!dealtIn) {
            return delta;
        }
        delta[Statistic.HANDS.ordinal()] = 1L;
        delta[Statistic.SAW_FLOP.ordinal()] = sawFlop ? 1L : 0L;
        delta[Statistic.SHOWDOWN.ordinal()] = reachedShowdown ? 1L : 0L;
        delta[Statistic.SHOWDOWN_PAYOUT.ordinal()] = contestedShowdownPayout > 0 ? 1L : 0L;
        for (StatsAction action : pendingActions) {
            if (action.getStreet() == BettingStreet.PREFLOP) {
                summarizePreflopAction(action, delta);
            } else {
                summarizePostflopAction(action, delta);
            }
        }
        return delta;
    }

    private static void summarizePreflopAction(StatsAction action, long[] delta) {
        if (action.getType() == ResolvedActionType.CALL
                || action.getType() == ResolvedActionType.RAISE) {
            delta[Statistic.VPIP.ordinal()] = 1L;
        }
        if (action.getType() == ResolvedActionType.RAISE) {
            delta[Statistic.PFR.ordinal()] = 1L;
        }
        if (action.getContext().hasThreeBetOpportunity()) {
            delta[Statistic.THREE_BET_OPPORTUNITY.ordinal()] = 1L;
        }
        if (action.getContext().getPreflopRaiseType() == PreflopRaiseType.THREE_BET) {
            delta[Statistic.THREE_BET.ordinal()] = 1L;
        }
    }

    private static void summarizePostflopAction(StatsAction action, long[] delta) {
        switch (action.getType()) {
            case BET:
                delta[Statistic.POSTFLOP_BETS.ordinal()]++;
                break;
            case RAISE:
                delta[Statistic.POSTFLOP_RAISES.ordinal()]++;
                break;
            case CALL:
                delta[Statistic.POSTFLOP_CALLS.ordinal()]++;
                break;
            case FOLD:
                delta[Statistic.POSTFLOP_FOLDS.ordinal()]++;
                break;
            default:
                break;
        }
        StatsContext context = action.getContext();
        if (context.hasFlopCBetOpportunity()) {
            delta[Statistic.FLOP_CBET_OPPORTUNITY.ordinal()] = 1L;
            if (action.getType() == ResolvedActionType.BET) {
                delta[Statistic.FLOP_CBET.ordinal()] = 1L;
            }
        }
        if (context.hasFacedFlopCBetOpportunity()) {
            delta[Statistic.FACED_CBET.ordinal()] = 1L;
            if (action.getType() == ResolvedActionType.FOLD) {
                delta[Statistic.FOLD_TO_CBET.ordinal()] = 1L;
            }
        }
    }

    private double aggressiveActions() {
        return (double) count(Statistic.POSTFLOP_BETS) + count(Statistic.POSTFLOP_RAISES);
    }

    private long count(Statistic counter) {
        return totals[counter.ordinal()];
    }

    private static OptionalDouble percentage(double numerator, double denominator) {
        return denominator == 0.0 ? OptionalDouble.empty()
                : OptionalDouble.of(100.0 * numerator / denominator);
    }

    private String requireActiveHand() {
        return activeHandId.orElseThrow(() -> new IllegalStateException("No hand is active."));
    }
}
