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
}
