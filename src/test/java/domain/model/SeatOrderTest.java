package domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SeatOrderTest {
    private static List<PlayerState> table(int... occupied) {
        Set<Integer> live = new HashSet<>(); for (int seat : occupied) { live.add(seat); }
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            boolean remains = live.contains(seat);
            players.add(new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN, remains ? 1000 : 0),
                remains ? PlayerStatus.ACTIVE : PlayerStatus.ELIMINATED, List.of(), 0, 0));
        }
        return players;
    }
    private static PlayerState folded(int seat) {
        return new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN, 1000), PlayerStatus.FOLDED, List.of(), 0, 0);
    }
    private static PlayerState allIn(int seat) {
        return new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN, 0), PlayerStatus.ALL_IN, List.of(), 1000, 1000);
    }
    @Test
    void SO_01() {
        SeatOrder order = new SeatOrder(table(0,1,2,3,4,5)); HandPositions p = order.positionsFor(0);
        assertEquals(0, p.getDealerSeat()); assertEquals(1, p.getSmallBlindSeat()); assertEquals(2, p.getBigBlindSeat());
        assertEquals(3, order.firstPreflopActor(0).orElseThrow()); assertEquals(1, order.firstPostflopActor(0).orElseThrow());
    }

    @Test
    void SO_02() {
        SeatOrder order = new SeatOrder(table(0,1,2,3,4,5)); HandPositions p = order.positionsFor(1);
        assertEquals(1, p.getDealerSeat()); assertEquals(2, p.getSmallBlindSeat()); assertEquals(3, p.getBigBlindSeat());
        assertEquals(4, order.firstPreflopActor(1).orElseThrow()); assertEquals(2, order.firstPostflopActor(1).orElseThrow());
    }

    @Test
    void SO_03() {
        SeatOrder order = new SeatOrder(table(0,1,2,3,4,5)); HandPositions p = order.positionsFor(2);
        assertEquals(2, p.getDealerSeat()); assertEquals(3, p.getSmallBlindSeat()); assertEquals(4, p.getBigBlindSeat());
        assertEquals(5, order.firstPreflopActor(2).orElseThrow()); assertEquals(3, order.firstPostflopActor(2).orElseThrow());
    }

    @Test
    void SO_04() {
        SeatOrder order = new SeatOrder(table(0,1,2,3,4,5)); HandPositions p = order.positionsFor(3);
        assertEquals(3, p.getDealerSeat()); assertEquals(4, p.getSmallBlindSeat()); assertEquals(5, p.getBigBlindSeat());
        assertEquals(0, order.firstPreflopActor(3).orElseThrow()); assertEquals(4, order.firstPostflopActor(3).orElseThrow());
    }

    @Test
    void SO_05() {
        SeatOrder order = new SeatOrder(table(0,1,2,3,4,5)); HandPositions p = order.positionsFor(4);
        assertEquals(4, p.getDealerSeat()); assertEquals(5, p.getSmallBlindSeat()); assertEquals(0, p.getBigBlindSeat());
        assertEquals(1, order.firstPreflopActor(4).orElseThrow()); assertEquals(5, order.firstPostflopActor(4).orElseThrow());
    }

    @Test
    void SO_06() {
        SeatOrder order = new SeatOrder(table(0,1,2,3,4,5)); HandPositions p = order.positionsFor(5);
        assertEquals(5, p.getDealerSeat()); assertEquals(0, p.getSmallBlindSeat()); assertEquals(1, p.getBigBlindSeat());
        assertEquals(2, order.firstPreflopActor(5).orElseThrow()); assertEquals(0, order.firstPostflopActor(5).orElseThrow());
    }

    @Test
    void SO_07() {
        SeatOrder order = new SeatOrder(table(0,4)); HandPositions p = order.positionsFor(0);
        assertEquals(0, p.getSmallBlindSeat()); assertEquals(4, p.getBigBlindSeat());
        assertEquals(0, order.firstPreflopActor(0).orElseThrow()); assertEquals(4, order.firstPostflopActor(0).orElseThrow());
    }

    @Test
    void SO_08() {
        SeatOrder order = new SeatOrder(table(0,4)); HandPositions p = order.positionsFor(4);
        assertEquals(4, p.getSmallBlindSeat()); assertEquals(0, p.getBigBlindSeat());
        assertEquals(4, order.firstPreflopActor(4).orElseThrow()); assertEquals(0, order.firstPostflopActor(4).orElseThrow());
    }

    @Test
    void SO_09() {
        SeatOrder order = new SeatOrder(table(0,2,5)); HandPositions p = order.positionsFor(5);
        assertEquals(0, p.getSmallBlindSeat()); assertEquals(2, p.getBigBlindSeat());
        assertEquals(5, order.firstPreflopActor(5).orElseThrow()); assertEquals(0, order.firstPostflopActor(5).orElseThrow());
    }

    @Test
    void SO_10() {
        assertEquals(2, new SeatOrder(table(2,5)).nextDealerAfter(0).orElseThrow());
    }

    @Test
    void SO_11() {
        assertEquals(0, new SeatOrder(table(0,2,5)).nextDealerAfter(5).orElseThrow());
    }

    @Test
    void SO_12() {
        List<PlayerState> players = table(0,1,2,3,4,5); players.set(1, folded(1)); players.set(2, allIn(2));
        SeatOrder order = new SeatOrder(players); assertEquals(3, order.nextActorAfter(0).orElseThrow());
        assertEquals(3, order.firstPostflopActor(0).orElseThrow());
    }

    @Test
    void SO_13() {
        List<PlayerState> players = table(0,1,2,3,4,5); players.set(3, allIn(3)); players.set(4, folded(4));
        assertEquals(5, new SeatOrder(players).firstPreflopActor(0).orElseThrow());
    }

    @Test
    void SO_14() {
        List<PlayerState> players = table(0,4); players.set(0, allIn(0)); players.set(4, allIn(4));
        SeatOrder order = new SeatOrder(players); assertTrue(order.nextActorAfter(0).isEmpty());
        assertTrue(order.firstPreflopActor(0).isEmpty()); assertTrue(order.firstPostflopActor(0).isEmpty());
    }

    @Test
    void SO_15() {
        SeatOrder order = new SeatOrder(table()); assertTrue(order.nextDealerAfter(0).isEmpty()); assertTrue(order.nextActorAfter(0).isEmpty());
    }

    @Test
    void SO_16() {
        SeatOrder order = new SeatOrder(table(0));
        assertThrows(IllegalStateException.class, () -> order.positionsFor(0));
        assertThrows(IllegalStateException.class, () -> order.firstPreflopActor(0));
        assertThrows(IllegalStateException.class, () -> order.firstPostflopActor(0));
    }

    @Test
    void SO_17() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(2,5)).positionsFor(0));
    }

    @Test
    void SO_18() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).positionsFor(-1));
    }

    @Test
    void SO_19() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).positionsFor(6));
    }

    @Test
    void SO_20() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).nextDealerAfter(-1));
    }

    @Test
    void SO_21() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).nextDealerAfter(6));
    }

    @Test
    void SO_22() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).nextActorAfter(-1));
    }

    @Test
    void SO_23() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).nextActorAfter(6));
    }

    @Test
    void SO_24() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).firstPreflopActor(-1));
    }

    @Test
    void SO_25() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).firstPreflopActor(6));
    }

    @Test
    void SO_26() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).firstPostflopActor(-1));
    }

    @Test
    void SO_27() {
        assertThrows(IllegalArgumentException.class, () -> new SeatOrder(table(0,1,2,3,4,5)).firstPostflopActor(6));
    }

    @Test
    void SO_28() {
        assertThrows(IllegalArgumentException.class, () -> { new SeatOrder(null); });
    }

    @Test
    void SO_29() {
        assertThrows(IllegalArgumentException.class, () -> { new SeatOrder(table(0,1,2,3,4).subList(0,5)); });
    }

    @Test
    void SO_30() {
        assertThrows(IllegalArgumentException.class, () -> { List<PlayerState> p = table(0,1,2,3,4,5); p.add(p.get(0)); new SeatOrder(p); });
    }

    @Test
    void SO_31() {
        assertThrows(IllegalArgumentException.class, () -> { List<PlayerState> p = table(0,1,2,3,4,5); p.set(0, null); new SeatOrder(p); });
    }

    @Test
    void SO_32() {
        assertThrows(IllegalArgumentException.class, () -> { List<PlayerState> p = table(0,1,2,3,4,5); p.set(1, p.get(0)); new SeatOrder(p); });
    }

    @Test
    void SO_33() {
        assertThrows(IllegalArgumentException.class, () -> { List<PlayerState> p = table(0,1,2,3,4,5); p.set(1, new PlayerState(new Player("p0", 1, PlayerType.HUMAN, 1000), PlayerStatus.ACTIVE, List.of(), 0, 0)); new SeatOrder(p); });
    }

    @Test
    void SO_34() {
        List<PlayerState> players = table(0,2,5); Collections.reverse(players);
        SeatOrder order = new SeatOrder(players); players.clear(); assertEquals(0, order.positionsFor(5).getSmallBlindSeat()); assertEquals(2, order.nextDealerAfter(0).orElseThrow());
    }

}
