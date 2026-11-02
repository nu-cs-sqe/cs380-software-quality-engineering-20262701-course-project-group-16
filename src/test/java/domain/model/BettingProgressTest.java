package domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class BettingProgressTest {

    @Test
    void BP_01() {
        BettingProgress b = new BettingProgress(20, 20, Set.of("p1"), Set.of("p1", "p2"));
        assertEquals(20, b.getCurrentBet()); assertEquals(20, b.getLastFullRaiseSize());
        assertEquals(Set.of("p1"), b.getPlayersToAct()); assertEquals(Set.of("p1", "p2"), b.getPlayersAllowedToRaise());
    }

    @Test
    void BP_02() {
        BettingProgress b = new BettingProgress(0, 1, Set.of(), Set.of());
        assertEquals(0, b.getCurrentBet()); assertTrue(b.getPlayersToAct().isEmpty()); assertTrue(b.getPlayersAllowedToRaise().isEmpty());
    }

    @Test
    void BP_03() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(-1, 20, Set.of(), Set.of()));
    }

    @Test
    void BP_04() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, -1, Set.of(), Set.of()));
    }

    @Test
    void BP_05() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, 0, Set.of(), Set.of()));
    }

    @Test
    void BP_06() {
        BettingProgress b = new BettingProgress(Long.MAX_VALUE, Long.MAX_VALUE, Set.of(), Set.of());
        assertEquals(Long.MAX_VALUE, b.getCurrentBet()); assertEquals(Long.MAX_VALUE, b.getLastFullRaiseSize());
    }

    @Test
    void BP_07() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, 20, null, Set.of()));
    }

    @Test
    void BP_08() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, 20, new HashSet<>(Arrays.asList((String) null)), Set.of()));
    }

    @Test
    void BP_09() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, 20, Set.of(""), Set.of()));
    }

    @Test
    void BP_10() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, 20, Set.of(" "), Set.of()));
    }

    @Test
    void BP_11() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, 20, Set.of(), null));
    }

    @Test
    void BP_12() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, 20, Set.of(), new HashSet<>(Arrays.asList((String) null))));
    }

    @Test
    void BP_13() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, 20, Set.of(), Set.of("")));
    }

    @Test
    void BP_14() {
        assertThrows(IllegalArgumentException.class, () -> new BettingProgress(20, 20, Set.of(), Set.of(" ")));
    }

    @Test
    void BP_15() {
        Set<String> pending = new HashSet<>(Set.of("p")); Set<String> raise = new HashSet<>(Set.of("p"));
        BettingProgress b = new BettingProgress(20, 20, pending, raise); pending.clear(); raise.clear();
        assertEquals(Set.of("p"), b.getPlayersToAct()); assertEquals(Set.of("p"), b.getPlayersAllowedToRaise());
    }

    @Test
    void BP_16() {
        BettingProgress b = new BettingProgress(20, 20, Set.of("p"), Set.of("p"));
        assertThrows(UnsupportedOperationException.class, () -> b.getPlayersToAct().clear());
        assertThrows(UnsupportedOperationException.class, () -> b.getPlayersAllowedToRaise().clear());
    }

}
