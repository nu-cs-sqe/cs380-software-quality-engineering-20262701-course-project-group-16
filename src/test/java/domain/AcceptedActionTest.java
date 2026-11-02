package domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AcceptedActionTest {
    private static StatsContext none() { return new StatsContext(PreflopRaiseType.NONE, false, false, false); }
    private static AcceptedAction event(String hand, String player, BettingStreet street,
            ResolvedActionType type, long chips, long after, boolean allIn, StatsContext context) {
        return new AcceptedAction(hand, player, street, type, chips, after, allIn, context);
    }
    @Test
    void AA_01() {
        AcceptedAction a = event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, 20, false, none());
        assertEquals("h", a.getHandId()); assertEquals("p", a.getPlayerId());
        assertEquals(BettingStreet.PREFLOP, a.getStreet()); assertEquals(ResolvedActionType.CALL, a.getType());
        assertEquals(10, a.getChipsAdded()); assertEquals(20, a.getRoundContributionAfter()); assertFalse(a.becameAllIn());
        assertNotNull(a.getStatsContext()); StatsAction observation = a.toStatsAction();
        assertTrue(observation.isAccepted()); assertEquals(ResolvedActionType.CALL, observation.getType()); assertEquals(10, observation.getChipsAdded());
    }

    @Test
    void AA_02() {
        StatsContext context = new StatsContext(PreflopRaiseType.OPENING_RAISE, false, false, false);
        AcceptedAction a = event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.RAISE, 50, 60, false, context);
        assertEquals(50, a.getChipsAdded()); assertEquals(60, a.getRoundContributionAfter()); assertSame(context, a.getStatsContext());
        assertEquals(ResolvedActionType.RAISE, a.toStatsAction().getType());
    }

    @Test
    void AA_03() {
        assertThrows(IllegalArgumentException.class, () -> event(null, "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, 20, false, none()));
    }

    @Test
    void AA_04() {
        assertThrows(IllegalArgumentException.class, () -> event("", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, 20, false, none()));
    }

    @Test
    void AA_05() {
        assertThrows(IllegalArgumentException.class, () -> event(" ", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, 20, false, none()));
    }

    @Test
    void AA_06() {
        assertThrows(IllegalArgumentException.class, () -> event("h", null, BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, 20, false, none()));
    }

    @Test
    void AA_07() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "", BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, 20, false, none()));
    }

    @Test
    void AA_08() {
        assertThrows(IllegalArgumentException.class, () -> event("h", " ", BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, 20, false, none()));
    }

    @Test
    void AA_09() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", null, ResolvedActionType.CALL, 10, 20, false, none()));
    }

    @Test
    void AA_10() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.PREFLOP, null, 10, 20, false, none()));
    }

    @Test
    void AA_11() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, 20, false, null));
    }

    @Test
    void AA_12() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, -1, 20, false, none()));
    }

    @Test
    void AA_13() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, -1, false, none()));
    }

    @Test
    void AA_14() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, 21, 20, false, none()));
    }

    @Test
    void AA_15() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, 0, 20, false, none()));
    }

    @Test
    void AA_16() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.FLOP, ResolvedActionType.CHECK, 0, 0, true, none()));
    }

    @Test
    void AA_17() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.FLOP, ResolvedActionType.FOLD, 0, 0, true, none()));
    }

    @Test
    void AA_18() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.RAISE, 50, 60, false, none()));
    }

    @Test
    void AA_19() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.BET, 50, 50, false, none()));
    }

    @Test
    void AA_20() {
        assertThrows(IllegalArgumentException.class, () -> event("h", "p", BettingStreet.FLOP, ResolvedActionType.FORCED_POST, 10, 10, false, none()));
    }

    @Test
    void AA_21() {
        AcceptedAction a = event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, 10, 10, true, none());
        assertTrue(a.becameAllIn()); assertEquals(ResolvedActionType.CALL, a.toStatsAction().getType());
    }

    @Test
    void AA_22() {
        AcceptedAction a = event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.RAISE, 10, 10, true, new StatsContext(PreflopRaiseType.THREE_BET, true, false, false));
        assertTrue(a.becameAllIn()); assertEquals(ResolvedActionType.RAISE, a.toStatsAction().getType());
    }

    @Test
    void AA_23() {
        AcceptedAction a = event("h", "p", BettingStreet.FLOP, ResolvedActionType.BET, 10, 10, true, none());
        assertTrue(a.becameAllIn()); assertEquals(ResolvedActionType.BET, a.toStatsAction().getType());
    }

    @Test
    void AA_24() {
        AcceptedAction a = event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, 10, 10, true, none());
        assertTrue(a.becameAllIn()); assertEquals(ResolvedActionType.FORCED_POST, a.toStatsAction().getType());
    }

    @Test
    void AA_25() {
        AcceptedAction a = event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.CALL, Long.MAX_VALUE, Long.MAX_VALUE, true, none());
        assertEquals(Long.MAX_VALUE, a.getChipsAdded()); assertEquals(Long.MAX_VALUE, a.getRoundContributionAfter());
    }

    @Test
    void AA_26() {
        AcceptedAction a = event("h", "p", BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0, 20, false, none());
        assertEquals(0, a.toStatsAction().getChipsAdded()); assertEquals(20, a.getRoundContributionAfter());
    }

    @Test
    void AA_27() {
        PlayStats stats=new PlayStats(); stats.beginHand("h",true);
        AcceptedAction blind=event("h","p",BettingStreet.PREFLOP,ResolvedActionType.FORCED_POST,10,10,true,none());
        stats.recordAction(blind.toStatsAction()); stats.completeHand("h",false,false,0,0);
        assertEquals(1,stats.getHandsPlayed()); assertEquals(0,stats.getVpipPercent().orElseThrow()); assertEquals(0,stats.getPfrPercent().orElseThrow());
    }

    @Test
    void AA_28() {
        PlayStats stats=new PlayStats(); stats.beginHand("h",true);
        AcceptedAction raise=event("h","p",BettingStreet.PREFLOP,ResolvedActionType.RAISE,50,60,false,new StatsContext(PreflopRaiseType.THREE_BET,true,false,false));
        AcceptedAction cbet=event("h","p",BettingStreet.FLOP,ResolvedActionType.BET,30,30,true,new StatsContext(PreflopRaiseType.NONE,false,true,false));
        stats.recordAction(raise.toStatsAction()); stats.recordAction(cbet.toStatsAction()); stats.completeHand("h",true,false,0,0);
        assertEquals(100,stats.getVpipPercent().orElseThrow()); assertEquals(100,stats.getPfrPercent().orElseThrow());
        assertEquals(100,stats.getThreeBetPercent().orElseThrow()); assertEquals(100,stats.getFlopCBetPercent().orElseThrow());
        assertEquals(100,stats.getAggressionFrequencyPercent().orElseThrow()); assertTrue(stats.getAggressionFactor().isEmpty());
    }

}
