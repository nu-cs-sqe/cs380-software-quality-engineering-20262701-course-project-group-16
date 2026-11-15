package domain.model;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class CompletedStatisticsTest {
    @Test
    void csC01EmptySnapshotHasAllZeroCountersAndNoHistory() {
        CompletedStatistics empty = new CompletedStatistics();
        assertEquals(16, empty.getCounts().size());
        assertTrue(empty.getCounts().values().stream().allMatch(n -> n == 0));
        assertTrue(empty.getCompletedHandIds().isEmpty());
        assertTrue(PlayStats.fromCompleted(empty).getVpipPercent().isEmpty());
    }

    @Test
    void csC02SnapshotCollectionsAreDetachedAndImmutable() {
        Map<Statistic, Long> counts = counts(Statistic.HANDS, 1);
        Set<String> ids = new HashSet<>(Set.of(" hand-1 "));
        CompletedStatistics snapshot = new CompletedStatistics(counts, ids);
        counts.put(Statistic.HANDS, 0L);
        ids.clear();
        assertEquals(1L, snapshot.getCounts().get(Statistic.HANDS));
        assertEquals(Set.of(" hand-1 "), snapshot.getCompletedHandIds());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getCounts().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getCompletedHandIds().clear());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSnapshots")
    void rejectsInvalidSnapshots(String id, Map<Statistic, Long> counts, Set<String> hands) {
        assertThrows(IllegalArgumentException.class,
                () -> new CompletedStatistics(counts, hands), id);
    }

    static Stream<Arguments> invalidSnapshots() {
        Map<Statistic, Long> missing = counts();
        missing.remove(Statistic.VPIP);
        Map<Statistic, Long> nullValue = counts();
        nullValue.put(Statistic.HANDS, null);
        Set<String> nullId = new HashSet<>();
        nullId.add(null);
        return Stream.of(
                Arguments.of("CS-C03", null, Set.of()),
                Arguments.of("CS-C04", counts(), null),
                Arguments.of("CS-C05", missing, Set.of()),
                Arguments.of("CS-C06", nullValue, Set.of()),
                Arguments.of("CS-C07", counts(Statistic.HANDS, -1), Set.of()),
                Arguments.of("CS-C08", counts(Statistic.HANDS, 1), Set.of()),
                Arguments.of("CS-C09", counts(), nullId),
                Arguments.of("CS-C10", counts(), Set.of("")),
                Arguments.of("CS-C11", counts(), Set.of(" ")),
                Arguments.of("CS-C12", counts(Statistic.VPIP, 1), Set.of("h")),
                Arguments.of("CS-C13", counts(Statistic.PFR, 1), Set.of("h")),
                Arguments.of("CS-C14", counts(Statistic.HANDS, 1, Statistic.VPIP, 1,
                        Statistic.THREE_BET_OPPORTUNITY, 1, Statistic.THREE_BET, 1), Set.of("h")),
                Arguments.of("CS-C15", counts(Statistic.HANDS, 1, Statistic.VPIP, 1,
                        Statistic.PFR, 1, Statistic.THREE_BET, 1), Set.of("h")),
                Arguments.of("CS-C16", counts(Statistic.THREE_BET_OPPORTUNITY, 1), Set.of("h")),
                Arguments.of("CS-C17", counts(Statistic.SAW_FLOP, 1), Set.of("h")),
                Arguments.of("CS-C18", counts(Statistic.SHOWDOWN, 1), Set.of("h")),
                Arguments.of("CS-C19", counts(Statistic.SHOWDOWN_PAYOUT, 1), Set.of("h")),
                Arguments.of("CS-C20", counts(Statistic.FLOP_CBET_OPPORTUNITY, 1), Set.of("h")),
                Arguments.of("CS-C21", counts(Statistic.FACED_CBET, 1), Set.of("h")),
                Arguments.of("CS-C22", counts(Statistic.HANDS, 1, Statistic.SAW_FLOP, 1,
                        Statistic.FLOP_CBET, 1, Statistic.POSTFLOP_BETS, 1), Set.of("h")),
                Arguments.of("CS-C23", counts(Statistic.HANDS, 1, Statistic.SAW_FLOP, 1,
                        Statistic.FOLD_TO_CBET, 1, Statistic.POSTFLOP_FOLDS, 1), Set.of("h")),
                Arguments.of("CS-C24", counts(Statistic.POSTFLOP_CALLS, 1), Set.of("h")),
                Arguments.of("CS-C29", counts(Statistic.HANDS, 1, Statistic.SAW_FLOP, 1,
                        Statistic.FLOP_CBET_OPPORTUNITY, 1, Statistic.FACED_CBET, 1), Set.of("h")),
                Arguments.of("CS-C30", counts(Statistic.HANDS, 1, Statistic.SAW_FLOP, 1,
                        Statistic.FLOP_CBET_OPPORTUNITY, 1, Statistic.FLOP_CBET, 1), Set.of("h")),
                Arguments.of("CS-C31", counts(Statistic.HANDS, 1, Statistic.SAW_FLOP, 1,
                        Statistic.FACED_CBET, 1, Statistic.FOLD_TO_CBET, 1), Set.of("h")),
                Arguments.of("CS-C32", counts(Statistic.POSTFLOP_BETS, 1), Set.of("h")),
                Arguments.of("CS-C33", counts(Statistic.POSTFLOP_RAISES, 1), Set.of("h")),
                Arguments.of("CS-C34", counts(Statistic.POSTFLOP_FOLDS, 1), Set.of("h")));
    }

    @Test
    void csC25AddsOneHandWithoutMutatingEarlierSnapshotsAndIgnoresRetries() {
        CompletedStatistics first = new CompletedStatistics(counts(Statistic.HANDS, 1,
                Statistic.VPIP, 1), Set.of("one"));
        CompletedStatistics second = new CompletedStatistics(counts(Statistic.HANDS, 1,
                Statistic.SAW_FLOP, 1, Statistic.SHOWDOWN, 1), Set.of("two"));
        CompletedStatistics combined = first.addHand(second);
        assertEquals(2L, combined.getCounts().get(Statistic.HANDS));
        assertEquals(Set.of("one", "two"), combined.getCompletedHandIds());
        assertSame(combined, combined.addHand(second));
        assertEquals(1L, first.getCounts().get(Statistic.HANDS));
        assertEquals(50, PlayStats.fromCompleted(combined).getVpipPercent().orElseThrow());
    }

    @Test
    void csC26RejectsNonHandInputAndPreservesStateOnOverflow() {
        CompletedStatistics empty = new CompletedStatistics();
        assertThrows(IllegalArgumentException.class, () -> empty.addHand(null));
        assertThrows(IllegalArgumentException.class, () -> empty.addHand(empty));
        CompletedStatistics several = new CompletedStatistics(counts(Statistic.HANDS, 1),
                Set.of("one", "two"));
        assertThrows(IllegalArgumentException.class, () -> empty.addHand(several));
        CompletedStatistics largest = new CompletedStatistics(counts(Statistic.HANDS, 1,
                Statistic.SAW_FLOP, 1, Statistic.POSTFLOP_CALLS, Long.MAX_VALUE), Set.of("one"));
        CompletedStatistics oneMore = new CompletedStatistics(counts(Statistic.HANDS, 1,
                Statistic.SAW_FLOP, 1, Statistic.POSTFLOP_CALLS, 1), Set.of("two"));
        assertThrows(ArithmeticException.class, () -> largest.addHand(oneMore));
        assertEquals(Long.MAX_VALUE, largest.getCounts().get(Statistic.POSTFLOP_CALLS));
        assertEquals(Set.of("one"), largest.getCompletedHandIds());
    }

    @Test
    void csC27RestorationRetainsHistoryAndNeverExportsUnfinishedObservations() {
        PlayStats original = new PlayStats();
        original.beginHand("done", true);
        original.completeHand("done", false, false, 0, 0);
        original.beginHand("unfinished", true);
        CompletedStatistics snapshot = original.completedSnapshot();
        PlayStats restored = PlayStats.fromCompleted(snapshot);
        assertEquals(1, restored.getHandsPlayed());
        assertEquals(Set.of("done"), snapshot.getCompletedHandIds());
        assertThrows(IllegalStateException.class, () -> restored.beginHand("done", true));
        restored.beginHand("unfinished", true);
        restored.completeHand("unfinished", false, false, 0, 0);
        assertEquals(2, restored.getHandsPlayed());
        assertEquals(1, original.getHandsPlayed());
        assertThrows(IllegalArgumentException.class, () -> PlayStats.fromCompleted(null));
    }

    @Test
    void csC28NonparticipatingCompletedHandsRetainDeduplicationWithoutInflatingHands() {
        PlayStats stats = new PlayStats();
        stats.beginHand("eliminated", false);
        stats.completeHand("eliminated", false, false, 0, 0);
        assertEquals(0L, stats.completedSnapshot().getCounts().get(Statistic.HANDS));
        assertEquals(Set.of("eliminated"), stats.completedSnapshot().getCompletedHandIds());
    }

    @Test
    void csC35RejectsExtraNullCounterKeysAndNegativeUnrelatedActionCounts() {
        Map<Statistic, Long> extra = new java.util.HashMap<>(counts());
        extra.put(null, 0L);
        assertThrows(IllegalArgumentException.class,
                () -> new CompletedStatistics(extra, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new CompletedStatistics(counts(Statistic.POSTFLOP_RAISES, -1), Set.of()));
    }

    private static Map<Statistic, Long> counts(Object... pairs) {
        Map<Statistic, Long> counts = new EnumMap<>(Statistic.class);
        for (Statistic key : Statistic.values()) counts.put(key, 0L);
        for (int i = 0; i < pairs.length; i += 2) {
            counts.put((Statistic) pairs[i], ((Number) pairs[i + 1]).longValue());
        }
        return counts;
    }
}
