package bot;

import domain.model.Card;
import domain.model.Rank;
import domain.model.Suit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class EquityRequestTest {
    private static final List<Card> HOLE = List.of(card(Rank.ACE), card(Rank.KING));
    private static final List<Card> BOARD = List.of(card(Rank.TWO), card(Rank.THREE),
            card(Rank.FOUR), card(Rank.FIVE), card(Rank.SIX));
    private static final List<EquityPot> POTS = List.of(new EquityPot("main", 100, Set.of("hero", "p1")));

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 4, 5})
    void erC01AcceptsEverySupportedStreet(int boardSize) {
        EquityRequest request = request(HOLE, BOARD.subList(0, boardSize), List.of("p1"), POTS, 1);
        assertEquals("hero", request.getHeroId());
        assertEquals(boardSize, request.getBoard().size());
        assertEquals(HOLE, request.getHoleCards());
        assertEquals(List.of("p1"), request.getOpponents());
        assertEquals(POTS, request.getPots());
        assertEquals(1, request.getSimulations());
        assertEquals(100, request.getEligiblePotAmount());
        assertEquals(100, request.getTotalPotAmount());
    }
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 6})
    void erC02RejectsImpossibleBoardSizes(int count) {
        List<Card> board = new ArrayList<>(BOARD);
        board.add(card(Rank.SEVEN));
        assertThrows(IllegalArgumentException.class,
                () -> request(HOLE, board.subList(0, count), List.of("p1"), POTS, 1));
    }
    @Test
    void erC03RequiresTwoUniqueHoleCardsAndNoKnownDuplicates() {
        for (List<Card> holes : List.of(List.<Card>of(), List.of(HOLE.get(0)), BOARD.subList(0, 3),
                List.of(HOLE.get(0), HOLE.get(0)))) {
            assertThrows(IllegalArgumentException.class, () -> request(holes, List.of(), List.of("p1"), POTS, 1));
        }
        assertThrows(IllegalArgumentException.class,
                () -> request(HOLE, List.of(HOLE.get(0), BOARD.get(0), BOARD.get(1)), List.of("p1"), POTS, 1));
        assertThrows(IllegalArgumentException.class,
                () -> request(HOLE, List.of(BOARD.get(0), BOARD.get(0), BOARD.get(1)), List.of("p1"), POTS, 1));
    }
    @Test
    void erC04RejectsMissingCollectionsAndNullElements() {
        assertThrows(IllegalArgumentException.class, () -> request(null, List.of(), List.of("p1"), POTS, 1));
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, null, List.of("p1"), POTS, 1));
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), null, POTS, 1));
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of("p1"), null, 1));
        assertThrows(IllegalArgumentException.class, () -> request(Arrays.asList(null, HOLE.get(0)), List.of(), List.of("p1"), POTS, 1));
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, Arrays.asList(null, BOARD.get(0), BOARD.get(1)), List.of("p1"), POTS, 1));
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of("p1"), Arrays.asList((EquityPot) null), 1));
    }
    @Test
    void erC05ValidatesParticipantIdentityAndSixSeatLimit() {
        for (String id : Arrays.asList(null, "", " ")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new EquityRequest(id, HOLE, List.of(), List.of("p1"), POTS, 1));
        }
        for (List<String> ids : List.of(List.of("hero"), List.of("p1", "p1"), List.of(" "),
                Arrays.asList((String) null), List.of("p1", "p2", "p3", "p4", "p5", "p6"))) {
            assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), ids, POTS, 1));
        }
        EquityRequest six = request(HOLE, List.of(), List.of("p1", "p2", "p3", "p4", "p5"), POTS, 1);
        assertEquals(5, six.getOpponents().size());
        assertEquals(0, request(HOLE, List.of(), List.of(),
                List.of(new EquityPot("only", 1, Set.of("hero"))), 1).getOpponents().size());
    }
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 10001})
    void erC06RejectsUnboundedOrEmptySimulationCounts(int count) {
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of("p1"), POTS, count));
    }
    @Test
    void erC07AcceptsTheSimulationUpperBoundaryAndPreservesImmutableInputs() {
        List<Card> holes = new ArrayList<>(HOLE);
        List<Card> board = new ArrayList<>(BOARD);
        List<String> opponents = new ArrayList<>(List.of("p1"));
        List<EquityPot> pots = new ArrayList<>(POTS);
        EquityRequest request = request(holes, board, opponents, pots, EquityRequest.MAX_SIMULATIONS);
        holes.clear(); board.clear(); opponents.clear(); pots.clear();
        assertEquals(10000, request.getSimulations());
        assertEquals(HOLE, request.getHoleCards());
        assertEquals(BOARD, request.getBoard());
        assertEquals(List.of("p1"), request.getOpponents());
        assertEquals(POTS, request.getPots());
        assertThrows(UnsupportedOperationException.class, () -> request.getHoleCards().clear());
        assertThrows(UnsupportedOperationException.class, () -> request.getBoard().clear());
        assertThrows(UnsupportedOperationException.class, () -> request.getOpponents().clear());
        assertThrows(UnsupportedOperationException.class, () -> request.getPots().clear());
    }
    @Test
    void erC08ValidatesPotsAndCountsOnlyHeroEligibleAmounts() {
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of("p1"), List.of(), 1));
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of("p1"), Collections.nCopies(7, POTS.get(0)), 1));
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of("p1"), List.of(POTS.get(0), POTS.get(0)), 1));
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of("p1"), List.of(new EquityPot("x", 1, Set.of("unknown"))), 1));
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of("p1"), List.of(new EquityPot("x", 1, Set.of("p1"))), 1));
        EquityRequest side = request(HOLE, List.of(), List.of("p1", "p2"), List.of(POTS.get(0),
                new EquityPot("side", 200, Set.of("p1", "p2"))), 1);
        assertEquals(100, side.getEligiblePotAmount());
        assertEquals(300, side.getTotalPotAmount());
        assertThrows(ArithmeticException.class, () -> request(HOLE, List.of(), List.of("p1"),
                List.of(new EquityPot("a", Long.MAX_VALUE, Set.of("hero")),
                        new EquityPot("b", 1, Set.of("hero"))), 1));
    }
    @Test
    void erC09IsolatesPotCountAndTotalOverflowBoundaries() {
        List<EquityPot> pots = new ArrayList<>();
        for (int i = 0; i < 7; i++) pots.add(new EquityPot("pot" + i, 1, Set.of("hero")));
        assertEquals(6, request(HOLE, List.of(), List.of(), pots.subList(0, 6), 1).getTotalPotAmount());
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of(), pots, 1));
        assertThrows(ArithmeticException.class, () -> request(HOLE, List.of(), List.of("p1"),
                List.of(new EquityPot("main", 1, Set.of("hero")),
                        new EquityPot("other", Long.MAX_VALUE, Set.of("p1"))), 1));
    }
    @Test
    void erC10RejectsInvalidOpponentIdsWithoutRelyingOnPotMembership() {
        List<EquityPot> onlyHero = List.of(new EquityPot("main", 1, Set.of("hero")));
        for (String invalid : Arrays.asList(null, "", " ", "\t")) {
            assertThrows(IllegalArgumentException.class,
                    () -> request(HOLE, List.of(), Arrays.asList(invalid), onlyHero, 1));
        }
    }
    @Test
    void erC11RejectsUnknownEligibilityEvenWhenHeroHasAnotherValidPot() {
        assertThrows(IllegalArgumentException.class, () -> request(HOLE, List.of(), List.of("p1"),
                List.of(POTS.get(0), new EquityPot("unknown", 1, Set.of("outsider"))), 1));
    }
    private static EquityRequest request(List<Card> holes, List<Card> board, List<String> opponents,
            List<EquityPot> pots, int simulations) {
        return new EquityRequest("hero", holes, board, opponents, pots, simulations);
    }
    private static Card card(Rank rank) { return new Card(rank, Suit.CLUBS); }
}
