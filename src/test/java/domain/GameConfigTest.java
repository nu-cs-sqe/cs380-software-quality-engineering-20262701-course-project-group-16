package domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GameConfigTest {

    @Test
    void GC_01() {
        GameConfig c = new GameConfig();
        assertAll(() -> assertEquals(1000, c.getStartingChips()),
            () -> assertEquals(10, c.getSmallBlind()), () -> assertEquals(20, c.getBigBlind()),
            () -> assertEquals(6, c.getSeatCount()), () -> assertEquals(0, c.getAnte()),
            () -> assertFalse(c.isBlindIncreaseEnabled()), () -> assertEquals(6000, c.getInitialTotalChips()));
    }

    @Test
    void GC_02() {
        GameConfig c = new GameConfig(2, 1, 2);
        assertEquals(2, c.getStartingChips()); assertEquals(1, c.getSmallBlind());
        assertEquals(2, c.getBigBlind()); assertEquals(12, c.getInitialTotalChips());
    }

    @Test
    void GC_03() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(-1, 1, 2));
    }

    @Test
    void GC_04() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(0, 1, 2));
    }

    @Test
    void GC_05() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1, 1, 2));
    }

    @Test
    void GC_06() {
        long maximum = Long.MAX_VALUE / 6;
        GameConfig c = new GameConfig(maximum, 10, 20);
        assertEquals(maximum * 6, c.getInitialTotalChips());
    }

    @Test
    void GC_07() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(Long.MAX_VALUE / 6 + 1, 10, 20));
    }

    @Test
    void GC_08() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(Long.MAX_VALUE, 10, 20));
    }

    @Test
    void GC_09() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1000, -1, 20));
    }

    @Test
    void GC_10() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1000, 0, 20));
    }

    @Test
    void GC_11() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1000, 20, 20));
    }

    @Test
    void GC_12() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1000, 21, 20));
    }

    @Test
    void GC_13() {
        assertEquals(19, new GameConfig(1000, 19, 20).getSmallBlind());
    }

    @Test
    void GC_14() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1000, 10, -1));
    }

    @Test
    void GC_15() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1000, 10, 0));
    }

    @Test
    void GC_16() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1000, 10, 9));
    }

    @Test
    void GC_17() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1000, 10, 10));
    }

    @Test
    void GC_18() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(1000, 10, 1001));
    }

    @Test
    void GC_19() {
        assertEquals(11, new GameConfig(1000, 10, 11).getBigBlind());
    }

    @Test
    void GC_20() {
        assertEquals(1000, new GameConfig(1000, 10, 1000).getBigBlind());
    }

    @Test
    void GC_21() {
        GameConfig first = new GameConfig(); new GameConfig(2000, 20, 40);
        assertEquals(1000, first.getStartingChips()); assertEquals(10, first.getSmallBlind()); assertEquals(20, first.getBigBlind());
    }

}
