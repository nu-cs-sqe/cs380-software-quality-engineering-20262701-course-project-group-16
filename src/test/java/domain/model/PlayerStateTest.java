package domain.model;


import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PlayerStateTest {
    private static Player player(long chips) { return new Player("p", 0, PlayerType.HUMAN, chips); }
    private static PlayerState state(long chips, PlayerStatus status, List<Card> cards, long round, long hand) {
        return new PlayerState(player(chips), status, cards, round, hand);
    }
    @Test
    void PS_01() {
        Player p = player(970); List<Card> cards = List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.SPADES));
        PlayerState s = new PlayerState(p, PlayerStatus.ACTIVE, cards, 10, 30);
        assertSame(p, s.getPlayer()); assertEquals(PlayerStatus.ACTIVE, s.getStatus()); assertEquals(cards, s.getHoleCards());
        assertEquals(10, s.getRoundContribution()); assertEquals(30, s.getHandContribution());
        assertTrue(s.canAct()); assertTrue(s.isParticipating());
    }

    @Test
    void PS_02() {
        assertTrue(state(1000, PlayerStatus.ACTIVE, List.of(), 0, 0).getHoleCards().isEmpty());
    }

    @Test
    void PS_03() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerState(null, PlayerStatus.ACTIVE, List.of(), 0, 0));
    }

    @Test
    void PS_04() {
        assertThrows(IllegalArgumentException.class, () -> state(1000, null, List.of(), 0, 0));
    }

    @Test
    void PS_05() {
        assertThrows(IllegalArgumentException.class, () -> state(1000, PlayerStatus.ACTIVE, null, 0, 0));
    }

    @Test
    void PS_06() {
        assertThrows(IllegalArgumentException.class, () -> state(1000, PlayerStatus.ACTIVE, Arrays.asList(new Card(Rank.ACE, Suit.SPADES), null), 0, 0));
    }

    @Test
    void PS_07() {
        assertThrows(IllegalArgumentException.class, () -> state(1000, PlayerStatus.ACTIVE, List.of(new Card(Rank.ACE, Suit.SPADES)), 0, 0));
    }

    @Test
    void PS_08() {
        assertThrows(IllegalArgumentException.class, () -> state(1000, PlayerStatus.ACTIVE, List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.SPADES), new Card(Rank.QUEEN, Suit.SPADES)), 0, 0));
    }

    @Test
    void PS_09() {
        assertThrows(IllegalArgumentException.class, () -> state(1000, PlayerStatus.ACTIVE, List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.ACE, Suit.SPADES)), 0, 0));
    }

    @Test
    void PS_10() {
        assertThrows(IllegalArgumentException.class, () -> state(1000, PlayerStatus.ACTIVE, List.of(), -1, 0));
    }

    @Test
    void PS_11() {
        assertThrows(IllegalArgumentException.class, () -> state(1000, PlayerStatus.ACTIVE, List.of(), 0, -1));
    }

    @Test
    void PS_12() {
        assertThrows(IllegalArgumentException.class, () -> state(1000, PlayerStatus.ACTIVE, List.of(), 21, 20));
    }

    @Test
    void PS_13() {
        assertThrows(IllegalArgumentException.class, () -> state(0, PlayerStatus.ACTIVE, List.of(), 0, 0));
    }

    @Test
    void PS_14() {
        assertThrows(IllegalArgumentException.class, () -> state(0, PlayerStatus.FOLDED, List.of(), 0, 0));
    }

    @Test
    void PS_15() {
        assertThrows(IllegalArgumentException.class, () -> state(1, PlayerStatus.ALL_IN, List.of(), 20, 20));
    }

    @Test
    void PS_16() {
        assertThrows(IllegalArgumentException.class, () -> state(0, PlayerStatus.ALL_IN, List.of(), 0, 0));
    }

    @Test
    void PS_17() {
        assertThrows(IllegalArgumentException.class, () -> state(1, PlayerStatus.ELIMINATED, List.of(), 0, 0));
    }

    @Test
    void PS_18() {
        assertThrows(IllegalArgumentException.class, () -> state(0, PlayerStatus.ELIMINATED, List.of(), 0, 1));
    }

    @Test
    void PS_19() {
        PlayerState s = state(0, PlayerStatus.ALL_IN, List.of(), 20, 20);
        assertFalse(s.canAct()); assertTrue(s.isParticipating());
    }

    @Test
    void PS_20() {
        PlayerState s = state(980, PlayerStatus.FOLDED, List.of(), 20, 20);
        assertFalse(s.canAct()); assertFalse(s.isParticipating()); assertEquals(20, s.getHandContribution());
    }

    @Test
    void PS_21() {
        PlayerState s = state(0, PlayerStatus.ELIMINATED, List.of(), 0, 0);
        assertFalse(s.canAct()); assertFalse(s.isParticipating()); assertEquals(0, s.getPlayer().getSeat());
    }

    @Test
    void PS_22() {
        assertThrows(IllegalArgumentException.class, () -> state(0, PlayerStatus.ELIMINATED, List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.SPADES)), 0, 0));
    }

    @Test
    void PS_23() {
        assertEquals(20, state(980, PlayerStatus.ACTIVE, List.of(), 20, 20).getHandContribution());
    }

    @Test
    void PS_24() {
        assertEquals(20, state(980, PlayerStatus.ACTIVE, List.of(), 0, 20).getHandContribution());
    }

    @Test
    void PS_25() {
        assertThrows(IllegalArgumentException.class, () -> state(Long.MAX_VALUE, PlayerStatus.ACTIVE, List.of(), 0, 1));
    }

    @Test
    void PS_26() {
        assertEquals(Long.MAX_VALUE - 1, state(Long.MAX_VALUE - 1, PlayerStatus.ACTIVE, List.of(), 0, 1).getPlayer().getChipStack());
    }

    @Test
    void PS_27() {
        List<Card> cards = new ArrayList<>(List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.SPADES)));
        PlayerState s = state(1000, PlayerStatus.ACTIVE, cards, 0, 0); cards.clear();
        assertEquals(2, s.getHoleCards().size());
    }

    @Test
    void PS_28() {
        PlayerState s = state(1000, PlayerStatus.ACTIVE, List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.SPADES)), 0, 0);
        assertThrows(UnsupportedOperationException.class, () -> s.getHoleCards().clear());
    }

}
