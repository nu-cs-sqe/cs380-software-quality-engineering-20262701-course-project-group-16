package domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayStatsCopyTest {
    @Test
    void copyKeepsTotalsPendingActionsAndCompletedIdsIndependent() {
        PlayStats original = new PlayStats();
        original.beginHand("first", true);
        original.completeHand("first", false, false, 0, 0);
        original.beginHand("second", true);
        original.recordAction(new StatsAction(true, BettingStreet.PREFLOP,
                ResolvedActionType.CALL, 20, new StatsContext(PreflopRaiseType.NONE,
                false, false, false)));
        PlayStats copy = original.copy();
        copy.completeHand("second", false, false, 0, 0);
        assertEquals(2, copy.getHandsPlayed());
        assertEquals(50, copy.getVpipPercent().orElseThrow());
        assertEquals(1, original.getHandsPlayed());
        assertThrows(IllegalStateException.class, () -> copy.beginHand("first", true));
        copy.beginHand("third", true);
        copy.completeHand("third", false, false, 0, 0);
        original.completeHand("second", false, false, 0, 0);
        original.beginHand("third", false);
        original.completeHand("third", false, false, 0, 0);
        assertEquals(2, original.getHandsPlayed());
        assertEquals(3, copy.getHandsPlayed());
    }
    @Test
    void rejectsContradictoryCBetContextsInEitherObservationOrder() {
        PlayStats stats = new PlayStats();
        stats.beginHand("context", true);
        stats.recordAction(new StatsAction(true, BettingStreet.FLOP, ResolvedActionType.CALL, 5,
                new StatsContext(PreflopRaiseType.NONE, false, false, true)));
        assertThrows(IllegalArgumentException.class, () -> stats.recordAction(new StatsAction(true,
                BettingStreet.FLOP, ResolvedActionType.CHECK, 0,
                new StatsContext(PreflopRaiseType.NONE, false, true, false))));
        stats.recordAction(new StatsAction(true, BettingStreet.RIVER, ResolvedActionType.CHECK, 0,
                new StatsContext(PreflopRaiseType.NONE, false, false, false)));
        stats.completeHand("context", true, true, 0, 0);
        assertEquals(0, stats.getFoldToFlopCBetPercent().orElseThrow());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2, 3})
    void rejectsEveryOutcomeForAnUndealtSeatWithoutChangingTotals(int fact) {
        PlayStats stats = new PlayStats();
        stats.beginHand("not-dealt", false);
        assertThrows(IllegalArgumentException.class, () -> stats.completeHand("not-dealt",
                fact == 0, fact == 1, fact == 2 ? 1 : 0, fact == 3 ? 1 : 0));
        stats.completeHand("not-dealt", false, false, 0, 0);
        assertEquals(0, stats.getHandsPlayed());
    }

}
