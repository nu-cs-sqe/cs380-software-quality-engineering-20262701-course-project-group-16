package domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PotSettlementTest {
    @Test
    void paysEachSidePotOnlyToEligiblePlayersAndReturnsUncalledChips() {
        GameState before = table(new long[]{10, 20, 30, 20, 0, 0}, Set.of(0, 1, 2));
        // Lower seat is stronger: seat0 wins main40, seat1 wins side30, seat2 refunded10.
        Settlement paid = new PotSettlement((a, b) -> Integer.compare(seat(b), seat(a)))
                .settle(before);
        assertEquals(40L, paid.getPayouts().get("p0"));
        assertEquals(30L, paid.getPayouts().get("p1"));
        assertEquals(10L, paid.getRefunds().get("p2"));
        assertEquals(0L, paid.getPayouts().get("p2"));
        assertEquals(40L, paid.getContestedPayouts().get("p0"));
        assertEquals(30L, paid.getContestedPayouts().get("p1"));
        assertEquals(GamePhase.COMPLETION, paid.getState().getPhase());
        assertEquals(0, paid.getState().getPot());
        assertEquals(6000, paid.getState().getTotalChips());
        assertEquals(80, before.getPot());
        assertEquals(970, before.getPlayers().get(2).getChipStack());
        assertEquals(980, paid.getState().getPlayers().get(2).getChipStack());
        assertThrows(UnsupportedOperationException.class, () -> paid.getPayouts().put("p0", 0L));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5})
    void splitsTiedPotAndAwardsOddChipClockwiseAfterDealer(int dealer) {
        GameState base = table(new long[]{5, 5, 5, 0, 0, 0}, Set.of(0, 1));
        GameState before = withDealer(base, dealer);
        Settlement paid = new PotSettlement((a, b) -> 0).settle(before);
        assertEquals(dealer == 0 ? 7L : 8L, paid.getPayouts().get("p0"));
        assertEquals(dealer == 0 ? 8L : 7L, paid.getPayouts().get("p1"));
        assertEquals(0L, paid.getPayouts().get("p2"));
        assertEquals(6000, paid.getState().getTotalChips());
    }

    @Test
    void uncontestedAwardAndRefundNeverCountAsContestedShowdownPayouts() {
        GameState before = table(new long[]{10, 20, 0, 0, 0, 0}, Set.of(1));
        Settlement paid = new PotSettlement((a, b) -> { throw new AssertionError("No comparison"); })
                .settle(before);
        assertEquals(20L, paid.getPayouts().get("p1"));
        assertEquals(10L, paid.getRefunds().get("p1"));
        assertTrue(paid.getContestedPayouts().values().stream().allMatch(n -> n == 0));
    }

    @Test
    void soleEligibleSidePotIsPaidWithoutCreditingAContestedWin() {
        GameState before = table(new long[]{10, 20, 20, 0, 0, 0}, Set.of(0, 1));
        Settlement paid = new PotSettlement((a, b) -> Integer.compare(seat(b), seat(a)))
                .settle(before);
        assertEquals(30L, paid.getPayouts().get("p0"));
        assertEquals(20L, paid.getPayouts().get("p1"));
        assertEquals(0L, paid.getContestedPayouts().get("p1"));
    }

    @Test
    void zeroPotFinishesWithoutInventingAPayout() {
        Settlement paid = new PotSettlement((a, b) -> {
            throw new AssertionError("A zero pot needs no comparison");
        }).settle(table(new long[6], Set.of(0, 1)));
        assertEquals(0, paid.getState().getPot());
        assertTrue(paid.getPayouts().values().stream().allMatch(n -> n == 0));
    }

    @Test
    void rejectsUnfinishedAndAlreadyCompletedHands() {
        PotSettlement service = new PotSettlement();
        assertThrows(IllegalStateException.class, () -> service.settle(new GameSetup().create(2)));
        GameState completed = service.settle(table(new long[6], Set.of(0, 1))).getState();
        assertThrows(IllegalStateException.class, () -> service.settle(completed));
        assertThrows(NullPointerException.class, () -> service.settle(null));
        assertThrows(NullPointerException.class, () -> new PotSettlement(null));
    }

    @Test
    void failedComparisonDoesNotModifyTheUnpaidState() {
        GameState before = table(new long[]{10, 10, 0, 0, 0, 0}, Set.of(0, 1));
        assertThrows(IllegalStateException.class, () -> new PotSettlement((a, b) -> {
            throw new IllegalStateException("Comparison failed");
        }).settle(before));
        assertEquals(20, before.getPot());
        assertEquals(GamePhase.SHOWDOWN, before.getPhase());
    }

    @Test
    void safeLongTotalIsSplitWithoutOverflow() {
        long starting = Long.MAX_VALUE / 6;
        GameConfig config = new GameConfig(starting, 1, 2);
        List<Card> cards = new Deck().deal(52);
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            players.add(new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN, 0),
                    PlayerStatus.ALL_IN, cards.subList(5 + seat * 2, 7 + seat * 2),
                    0, starting));
        }
        GameState before = new GameState(config, "large", GamePhase.SHOWDOWN, players,
                new HandPositions(0, 1, 2), null, cards.subList(0, 5), List.of(),
                new BettingProgress(0, 2, Set.of(), Set.of()));
        Settlement paid = new PotSettlement((a, b) -> 0).settle(before);
        assertEquals(starting * 6, paid.getState().getTotalChips());
        assertTrue(paid.getState().getPlayers().stream().allMatch(p -> p.getChipStack() == starting));
    }

    @Test
    void invalidSidePotWithoutAnEligibleContributorRejectsWithoutLosingChips() {
        GameState before = table(new long[]{10, 10, 20, 20, 0, 0}, Set.of(0, 1));
        assertThrows(IllegalStateException.class, () -> new PotSettlement().settle(before));
        assertEquals(60, before.getPot());
    }

    @Test
    void laterStrongerContenderReplacesTheEarlierWinner() {
        Settlement paid = new PotSettlement((a, b) -> Integer.compare(seat(a), seat(b)))
                .settle(table(new long[]{10, 10, 0, 0, 0, 0}, Set.of(0, 1)));
        assertEquals(0L, paid.getPayouts().get("p0"));
        assertEquals(20L, paid.getPayouts().get("p1"));
    }

    @Test
    void oddChipOrderWrapsAcrossNonAdjacentTiedSeats() {
        GameState before = withDealer(table(new long[]{5, 0, 0, 5, 5, 0}, Set.of(0, 3)), 2);
        Settlement paid = new PotSettlement((a, b) -> 0).settle(before);
        assertEquals(7L, paid.getPayouts().get("p0"));
        assertEquals(8L, paid.getPayouts().get("p3"));
    }

    @Test
    void rejectsPendingDecisionsRaiseRightsNoContendersAndEarlyContestedBoard() {
        GameState base = table(new long[]{10, 10, 0, 0, 0, 0}, Set.of(0, 1));
        PotSettlement service = new PotSettlement();
        for (int kind = 0; kind < 3; kind++) {
            Set<String> pending = kind == 0 ? Set.of("p0") : Set.of();
            Set<String> raising = kind == 1 ? Set.of("p0") : Set.of();
            GameState invalid = new GameState(base.getConfig(), "early", GamePhase.PREFLOP,
                    base.getPlayerStates(), base.getPositions(), kind == 0 ? "p0" : null,
                    List.of(), List.of(), new BettingProgress(0, 20, pending, raising));
            assertThrows(IllegalStateException.class, () -> service.settle(invalid));
        }
        assertThrows(IllegalStateException.class,
                () -> service.settle(table(new long[]{10, 10, 0, 0, 0, 0}, Set.of())));
    }

    private static int seat(List<Card> holding) {
        return (holding.get(5).getRank().ordinal() + holding.get(5).getSuit().ordinal() * 13 - 5) / 2;
    }

    private static GameState withDealer(GameState state, int dealer) {
        return new GameState(state.getConfig(), state.getHandId(), state.getPhase(),
                state.getPlayerStates(), new SeatOrder(state.getPlayerStates()).positionsFor(dealer),
                null, state.getCommunityCards(), List.of(), state.getBettingProgress());
    }

    static GameState table(long[] contributions, Set<Integer> live) {
        List<Card> cards = new Deck().deal(52);
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            Player player = new Player("p" + seat, seat, PlayerType.HUMAN, 1000 - contributions[seat]);
            players.add(new PlayerState(player, live.contains(seat) ? PlayerStatus.ACTIVE
                    : PlayerStatus.FOLDED, cards.subList(5 + seat * 2, 7 + seat * 2),
                    0, contributions[seat]));
        }
        return new GameState(new GameConfig(), "hand", GamePhase.SHOWDOWN, players,
                new SeatOrder(players).positionsFor(0), null, cards.subList(0, 5), List.of(),
                new BettingProgress(0, 20, Set.of(), Set.of()));
    }
}
