package domain.model;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Immutable completed counters and duplicate-prevention IDs; no pending-hand data. */
public final class CompletedStatistics {
    private final Map<Statistic, Long> counts;
    private final Set<String> completedHandIds;

    public CompletedStatistics() {
        this(zeroCounts(), Set.of());
    }

    public CompletedStatistics(Map<Statistic, Long> counts, Set<String> completedHandIds) {
        requireCollections(counts, completedHandIds);
        requireConsistentCounts(counts, completedHandIds.size());
        this.counts = Map.copyOf(counts);
        this.completedHandIds = Set.copyOf(completedHandIds);
    }

    private static Map<Statistic, Long> zeroCounts() {
        Map<Statistic, Long> counts = new EnumMap<>(Statistic.class);
        for (Statistic counter : Statistic.values()) {
            counts.put(counter, 0L);
        }
        return counts;
    }

    private static void requireCollections(Map<Statistic, Long> counts, Set<String> ids) {
        if (counts == null || ids == null || counts.size() != Statistic.values().length) {
            throw new IllegalArgumentException("All counters and completed IDs are required.");
        }
        for (Statistic counter : Statistic.values()) {
            Long count = counts.get(counter);
            if (count == null || count < 0) {
                throw new IllegalArgumentException("Counters must be present and nonnegative.");
            }
        }
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Completed hand IDs must be nonblank.");
            }
        }
    }

    private static void requireConsistentCounts(Map<Statistic, Long> counts, int completed) {
        if (counts.get(Statistic.HANDS) > completed) {
            throw new IllegalArgumentException("Hands played require completed identities.");
        }
        requireSubset(counts, Statistic.VPIP, Statistic.HANDS);
        requireSubset(counts, Statistic.PFR, Statistic.VPIP);
        requireSubset(counts, Statistic.THREE_BET, Statistic.PFR);
        requireSubset(counts, Statistic.THREE_BET, Statistic.THREE_BET_OPPORTUNITY);
        requireSubset(counts, Statistic.THREE_BET_OPPORTUNITY, Statistic.HANDS);
        requireSubset(counts, Statistic.SAW_FLOP, Statistic.HANDS);
        requireSubset(counts, Statistic.SHOWDOWN, Statistic.SAW_FLOP);
        requireSubset(counts, Statistic.SHOWDOWN_PAYOUT, Statistic.SHOWDOWN);
        requireFlopCounts(counts);
    }

    private static void requireFlopCounts(Map<Statistic, Long> counts) {
        requireSubset(counts, Statistic.FLOP_CBET, Statistic.FLOP_CBET_OPPORTUNITY);
        requireSubset(counts, Statistic.FOLD_TO_CBET, Statistic.FACED_CBET);
        requireSubset(counts, Statistic.FLOP_CBET, Statistic.POSTFLOP_BETS);
        requireSubset(counts, Statistic.FOLD_TO_CBET, Statistic.POSTFLOP_FOLDS);
        if (counts.get(Statistic.FLOP_CBET_OPPORTUNITY)
                > counts.get(Statistic.SAW_FLOP) - counts.get(Statistic.FACED_CBET)) {
            throw new IllegalArgumentException("Opening and facing c-bets require distinct hands.");
        }
        for (Statistic counter : Set.of(Statistic.POSTFLOP_BETS, Statistic.POSTFLOP_RAISES,
                Statistic.POSTFLOP_CALLS, Statistic.POSTFLOP_FOLDS)) {
            if (counts.get(counter) > 0 && counts.get(Statistic.SAW_FLOP) == 0) {
                throw new IllegalArgumentException("Postflop actions require seeing the flop.");
            }
        }
    }

    private static void requireSubset(Map<Statistic, Long> counts, Statistic part,
            Statistic whole) {
        if (counts.get(part) > counts.get(whole)) {
            throw new IllegalArgumentException(part + " cannot exceed " + whole + ".");
        }
    }

    public Map<Statistic, Long> getCounts() { return counts; }
    public Set<String> getCompletedHandIds() { return completedHandIds; }

    /** Adds one authoritative completed hand; repeated delivery has no effect. */
    public CompletedStatistics addHand(CompletedStatistics hand) {
        if (hand == null || hand.completedHandIds.size() != 1) {
            throw new IllegalArgumentException("Supply exactly one completed hand.");
        }
        String id = hand.completedHandIds.iterator().next();
        if (completedHandIds.contains(id)) {
            return this;
        }
        Map<Statistic, Long> next = new EnumMap<>(Statistic.class);
        for (Statistic counter : Statistic.values()) {
            next.put(counter, Math.addExact(counts.get(counter), hand.counts.get(counter)));
        }
        Set<String> ids = new HashSet<>(completedHandIds);
        ids.add(id);
        return new CompletedStatistics(next, ids);
    }
}
