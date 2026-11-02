package domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class HandPositionsTest {

    @Test
    void HP_01() {
        assertThrows(IllegalArgumentException.class, () -> new HandPositions(-1, 1, 2));
    }

    @Test
    void HP_02() {
        assertThrows(IllegalArgumentException.class, () -> new HandPositions(6, 1, 2));
    }

    @Test
    void HP_03() {
        assertThrows(IllegalArgumentException.class, () -> new HandPositions(0, -1, 2));
    }

    @Test
    void HP_04() {
        assertThrows(IllegalArgumentException.class, () -> new HandPositions(0, 6, 2));
    }

    @Test
    void HP_05() {
        assertThrows(IllegalArgumentException.class, () -> new HandPositions(0, 1, -1));
    }

    @Test
    void HP_06() {
        assertThrows(IllegalArgumentException.class, () -> new HandPositions(0, 1, 6));
    }

    @Test
    void HP_07() {
        assertThrows(IllegalArgumentException.class, () -> new HandPositions(0, 1, 1));
    }

    @Test
    void HP_08() {
        assertThrows(IllegalArgumentException.class, () -> new HandPositions(0, 1, 0));
    }

    @Test
    void HP_09() {
        HandPositions p = new HandPositions(0, 1, 2);
        assertEquals(0, p.getDealerSeat()); assertEquals(1, p.getSmallBlindSeat()); assertEquals(2, p.getBigBlindSeat());
    }

    @Test
    void HP_10() {
        HandPositions p = new HandPositions(5, 0, 1);
        assertEquals(5, p.getDealerSeat()); assertEquals(0, p.getSmallBlindSeat()); assertEquals(1, p.getBigBlindSeat());
    }

    @Test
    void HP_11() {
        HandPositions p = new HandPositions(0, 0, 5);
        assertEquals(0, p.getDealerSeat()); assertEquals(0, p.getSmallBlindSeat()); assertEquals(5, p.getBigBlindSeat());
    }

}
