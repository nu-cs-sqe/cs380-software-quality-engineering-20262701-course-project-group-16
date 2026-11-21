package domain.model;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PotLayoutTest {
    @Test
    void pl01BuildsLayersAndSeparatesUncalledRefunds() {
        GameState state = table(new long[] {10, 20, 30, 0, 0, 0}, Set.of(0, 1, 2));
        PotLayout layout = PotLayout.from(state);
        assertEquals(2, layout.getLayers().size());
        assertEquals(30, layout.getLayers().get(0).getAmount());
        assertEquals(Set.of("p0", "p1", "p2"), layout.getLayers().get(0).getEligiblePlayers());
        assertEquals(20, layout.getLayers().get(1).getAmount());
        assertEquals(Set.of("p1", "p2"), layout.getLayers().get(1).getEligiblePlayers());
        assertEquals(10, layout.getRefunds().get("p2"));
        assertEquals(6, layout.getRefunds().size());
        assertEquals(state.getPot(), layout.getLayers().stream()
                .mapToLong(PotLayout.Layer::getAmount).sum()
                + layout.getRefunds().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(60, state.getPot());
        assertThrows(UnsupportedOperationException.class, () -> layout.getLayers().clear());
        assertThrows(UnsupportedOperationException.class, () -> layout.getRefunds().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> layout.getLayers().get(0).getEligiblePlayers().clear());
    }

    @Test
    void pl02FoldedMoneyStaysInPotsAndFoldedUncalledMoneyIsRefunded() {
        PotLayout layout = PotLayout.from(table(new long[] {10, 20, 30, 0, 0, 0},
                Set.of(0, 2)));
        assertEquals(30, layout.getLayers().get(0).getAmount());
        assertEquals(Set.of("p0", "p2"), layout.getLayers().get(0).getEligiblePlayers());
        assertEquals(Set.of("p2"), layout.getLayers().get(1).getEligiblePlayers());
        PotLayout folded = PotLayout.from(table(new long[] {10, 20, 30, 0, 0, 0},
                Set.of(0, 1)));
        assertEquals(10, folded.getRefunds().get("p2"));
    }

    @Test
    void pl03ZeroAndOneChipBoundariesAndSixDistinctLevels() {
        assertTrue(PotLayout.from(table(new long[6], Set.of(0, 1))).getLayers().isEmpty());
        PotLayout one = PotLayout.from(table(new long[] {1, 1, 1, 1, 1, 1},
                Set.of(0, 1, 2, 3, 4, 5)));
        assertEquals(1, one.getLayers().size());
        assertEquals(6, one.getLayers().get(0).getAmount());
        PotLayout six = PotLayout.from(table(new long[] {1, 2, 3, 4, 5, 6},
                Set.of(0, 1, 2, 3, 4, 5)));
        assertEquals(List.of(6L, 5L, 4L, 3L, 2L), six.getLayers().stream()
                .map(PotLayout.Layer::getAmount).collect(java.util.stream.Collectors.toList()));
        assertEquals(1, six.getRefunds().get("p5"));
    }

    @Test
    void pl04DeadOpeningPotBelongsToTheRemainingLiveHands() {
        GameState hand = table(new long[] {10, 10, 0, 0, 0, 0}, Set.of(2, 3));
        PotLayout layout = PotLayout.from(hand);
        assertEquals(1, layout.getLayers().size());
        assertEquals(20, layout.getLayers().get(0).getAmount());
        assertEquals(Set.of("p2", "p3"), layout.getLayers().get(0).getEligiblePlayers());
        Settlement paid = new PotSettlement((first, second) -> 0).settle(hand);
        assertEquals(10, paid.getPayouts().get("p2"));
        assertEquals(10, paid.getPayouts().get("p3"));
    }

    @Test
    void pl05SettlementConsumesTheSameLayout() {
        Settlement paid = new PotSettlement((first, second) -> 0)
                .settle(table(new long[] {10, 20, 30, 0, 0, 0}, Set.of(0, 1, 2)));
        assertEquals(10, paid.getPayouts().get("p0"));
        assertEquals(20, paid.getPayouts().get("p1"));
        assertEquals(20, paid.getPayouts().get("p2"));
        assertEquals(10, paid.getRefunds().get("p2"));
        assertEquals(6000, paid.getState().getTotalChips());
    }

    @Test
    void deadSidePotMovesToTheNearestLowerEligibleLayer() {
        GameState state = table(new long[] {10, 20, 40, 50, 0, 0}, Set.of(0, 1));
        PotLayout layout = PotLayout.from(state);
        assertEquals(List.of(40L, 70L), layout.getLayers().stream()
                .map(PotLayout.Layer::getAmount).collect(java.util.stream.Collectors.toList()));
        assertEquals(Set.of("p1"), layout.getLayers().get(1).getEligiblePlayers());
        assertEquals(10, layout.getRefunds().get("p3"));
        Settlement paid = new PotSettlement((first, second) -> 0).settle(state);
        assertEquals(20, paid.getPayouts().get("p0"));
        assertEquals(90, paid.getPayouts().get("p1"));
        assertEquals(10, paid.getRefunds().get("p3"));
        assertEquals(6000, paid.getState().getTotalChips());
    }

    @Test
    void deadPotWithoutAnyRemainingHandRejectsInvalidInput() {
        GameState hand = table(new long[] {10, 10, 0, 0, 0, 0}, Set.of());
        assertThrows(IllegalStateException.class, () -> PotLayout.from(hand));
    }

    private static GameState table(long[] contributions, Set<Integer> live) {
        List<Card> cards = new Deck().deal(52);
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            Player player = new Player("p" + seat, seat, PlayerType.HUMAN,
                    1000 - contributions[seat]);
            players.add(new PlayerState(player, live.contains(seat)
                    ? PlayerStatus.ACTIVE : PlayerStatus.FOLDED,
                    cards.subList(seat * 2, seat * 2 + 2), 0, contributions[seat]));
        }
        return GameState.create(new GameConfig(), "layout", GamePhase.SHOWDOWN, players,
                new HandPositions(0, 1, 2), Optional.empty(), cards.subList(12, 17),
                List.of(), new BettingProgress(0, 20, Set.of(), Set.of()));
    }
}
