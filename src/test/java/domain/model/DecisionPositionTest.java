package domain.model;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecisionPositionTest {
    @Test
    void dp01UsesStreetOrderAndSkipsMissingSeats() {
        HandPositions positions = new HandPositions(0, 1, 2);
        Set<Integer> active = Set.of(0, 2, 3, 5);
        assertTrue(positions.isInPosition(2, active, true));
        assertFalse(positions.isInPosition(3, active, true));
        assertTrue(positions.isInPosition(0, active, false));
        assertFalse(positions.isInPosition(2, active, false));
        assertTrue(positions.isInPosition(5, Set.of(3, 5), false));
        assertFalse(positions.isInPosition(1, active, false));
    }

    @Test
    void dp02HeadsUpAndNoResponders() {
        HandPositions positions = new HandPositions(0, 0, 3);
        assertTrue(positions.isInPosition(3, Set.of(0, 3), true));
        assertFalse(positions.isInPosition(0, Set.of(0, 3), true));
        assertTrue(positions.isInPosition(0, Set.of(0, 3), false));
        assertFalse(positions.isInPosition(3, Set.of(0, 3), false));
        assertFalse(positions.isInPosition(0, Set.of(), false));
        assertFalse(positions.isInPosition(0, Set.of(0), false));
    }

    @Test
    void dp03RejectsSeatsOutsideTheTable() {
        HandPositions positions = new HandPositions(0, 1, 2);
        for (int seat : new int[] {-1, 6}) {
            assertThrows(IllegalArgumentException.class,
                    () -> positions.isInPosition(seat, Set.of(0, 2), true));
            assertThrows(IllegalArgumentException.class,
                    () -> positions.isInPosition(0, Set.of(0, seat), true));
        }
    }
}
