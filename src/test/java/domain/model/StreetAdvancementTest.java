package domain.model;

import application.GameInitializer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class StreetAdvancementTest {
    private final BettingEngine engine = new BettingEngine();

    @ParameterizedTest(name = "{0}: {1} to {2}")
    @CsvSource({
        "BE-SA01, PREFLOP, FLOP,     3, 36,  0, 20",
        "BE-SA02, FLOP,    TURN,     4, 34,  0, 20",
        "BE-SA03, TURN,    RIVER,    5, 32,  0, 20",
        "BE-SA04, RIVER,   SHOWDOWN, 5, 32, 40, 40"
    })
    void advancementPreservesTheHandAndUsesTheCorrectBoardOrder(String caseId,
            GamePhase phase, GamePhase next, int boardSize, int deckSize,
            long roundContribution, long raiseSize) {
        GameState before = closedRound(phase).build();
        List<Object> original = snapshot(before);
        Card[] cards = FoundationFixtures.cards();
        List<Card> expectedBoard = List.of(cards[13], cards[14], cards[15], cards[17], cards[19]);

        GameState after = engine.advanceStreet(before);

        assertNotSame(before, after);
        assertEquals(next, after.getPhase(), caseId);
        assertEquals(expectedBoard.subList(0, boardSize), after.getCommunityCards());
        assertEquals(Arrays.asList(cards).subList(52 - deckSize, 52), after.getRemainingDeck());
        assertEquals(before.getHandId(), after.getHandId());
        assertEquals(before.getDealerSeat(), after.getDealerSeat());
        assertEquals(before.getSmallBlindSeat(), after.getSmallBlindSeat());
        assertEquals(before.getBigBlindSeat(), after.getBigBlindSeat());
        assertEquals(before.getConfig().getStartingChips(), after.getConfig().getStartingChips());
        assertEquals(before.getConfig().getSmallBlind(), after.getConfig().getSmallBlind());
        assertEquals(before.getConfig().getBigBlind(), after.getConfig().getBigBlind());
        assertEquals(roundContribution, after.getCurrentBet());
        assertEquals(raiseSize, after.getLastFullRaiseSize());
        assertEquals(phase == GamePhase.PREFLOP ? 360 : 600, after.getPot());
        assertEquals(6000, after.getTotalChips());
        for (int seat = 0; seat < 6; seat++) {
            PlayerState old = before.getPlayerStates().get(seat);
            PlayerState player = after.getPlayerStates().get(seat);
            assertEquals(old.getPlayer().getId(), player.getPlayer().getId());
            assertEquals(old.getPlayer().getSeat(), player.getPlayer().getSeat());
            assertEquals(old.getPlayer().getType(), player.getPlayer().getType());
            assertEquals(old.getPlayer().getChipStack(), player.getPlayer().getChipStack());
            assertEquals(old.getStatus(), player.getStatus());
            assertEquals(old.getHoleCards(), player.getHoleCards());
            assertEquals(old.getHandContribution(), player.getHandContribution());
            assertEquals(roundContribution, player.getRoundContribution());
        }
        if (next == GamePhase.SHOWDOWN) {
            assertTrue(after.getCurrentActorId().isEmpty());
            assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
            assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        } else {
            assertEquals(1, after.getCurrentActorSeat().orElseThrow());
            assertEquals(FoundationFixtures.allIds(), after.getBettingProgress().getPlayersToAct());
            assertEquals(FoundationFixtures.allIds(), after.getBettingProgress().getPlayersAllowedToRaise());
        }
        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "{0}: {1} with {2} remaining cards")
    @CsvSource({
        "BE-SA05-0,  PREFLOP, 0, false, 4",
        "BE-SA05-1,  PREFLOP, 1, false, 4",
        "BE-SA05-3,  PREFLOP, 3, false, 4",
        "BE-SA05-4,  PREFLOP, 4, true,  4",
        "BE-SA05-5,  PREFLOP, 5, true,  4",
        "BE-SA05-40, PREFLOP,40, true,  4",
        "BE-SA06-0,  FLOP,    0, false, 2",
        "BE-SA06-1,  FLOP,    1, false, 2",
        "BE-SA06-2,  FLOP,    2, true,  2",
        "BE-SA06-3,  FLOP,    3, true,  2",
        "BE-SA07-0,  TURN,    0, false, 2",
        "BE-SA07-1,  TURN,    1, false, 2",
        "BE-SA07-2,  TURN,    2, true,  2",
        "BE-SA07-3,  TURN,    3, true,  2"
    })
    void dealingRequiresTheBurnCardAndAllCardsForThatStreet(String caseId,
            GamePhase phase, int available, boolean sufficient, int consumed) {
        FoundationFixtures.Fixture fixture = closedRound(phase);
        fixture.deck = new ArrayList<>(fixture.deck.subList(0, available));
        GameState before = fixture.build();
        List<Object> original = snapshot(before);

        if (sufficient) {
            GameState after = engine.advanceStreet(before);
            assertEquals(available - consumed, after.getRemainingDeck().size(), caseId);
            assertEquals(before.getRemainingDeck().subList(consumed, available), after.getRemainingDeck());
            assertFalse(after.getCommunityCards().contains(before.getRemainingDeck().get(0)));
            assertEquals(6000, after.getTotalChips());
        } else {
            assertThrows(IllegalArgumentException.class, () -> engine.advanceStreet(before), caseId);
        }
        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "BE-SA08-{0}: reject advancement with pending decisions")
    @ValueSource(ints = {1, 6})
    void aPendingDecisionPreventsAdvancement(int pending) {
        Random dealer = new SeatZeroRandom();
        GameState state = new GameInitializer(new GameConfig(), dealer, new Random(7)).start(2);
        if (pending == 1) {
            for (int seat : new int[]{3, 4, 5, 0, 1}) {
                state = engine.apply(state, new ActionRequest(state.getPlayers().get(seat).getId(),
                        ActionType.CALL)).getState();
            }
        }
        GameState before = state;
        List<Object> original = snapshot(before);
        assertEquals(pending, before.getBettingProgress().getPlayersToAct().size());

        assertThrows(IllegalArgumentException.class, () -> engine.advanceStreet(before));

        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "BE-SA09-{0}: a hand needs at least two participants to advance")
    @ValueSource(ints = {0, 1})
    void anUncontestedOrEmptyHandDoesNotDealMoreCards(int remaining) {
        FoundationFixtures.Fixture fixture = closedRound(GamePhase.PREFLOP);
        for (int seat = 0; seat < 6; seat++) {
            fixture.player(seat, 940, seat == 5 && remaining == 1
                    ? PlayerStatus.ACTIVE : PlayerStatus.FOLDED, 60, 60);
        }
        GameState before = fixture.build();
        List<Object> original = snapshot(before);

        assertThrows(IllegalArgumentException.class, () -> engine.advanceStreet(before));

        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "BE-SA10-{0}: empty pending set cannot hide an unpaid active player")
    @ValueSource(longs = {1, 20})
    void anActivePlayerOwingChipsPreventsAdvancement(long owed) {
        FoundationFixtures.Fixture fixture = closedRound(GamePhase.PREFLOP);
        fixture.player(3, 940 + owed, PlayerStatus.ACTIVE, 60 - owed, 60 - owed);
        GameState before = fixture.build();
        List<Object> original = snapshot(before);

        assertThrows(IllegalArgumentException.class, () -> engine.advanceStreet(before));

        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "BE-SA11-{0}: this phase has no next betting street")
    @EnumSource(value = GamePhase.class, names = {"SETUP", "SHOWDOWN", "COMPLETION"})
    void setupAndTerminalPhasesCannotAdvance(GamePhase phase) {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        if (phase != GamePhase.SETUP) {
            fixture.board(phase, 5);
        }
        GameState before = fixture.build();
        List<Object> original = snapshot(before);

        assertThrows(UnsupportedOperationException.class, () -> engine.advanceStreet(before));

        assertEquals(original, snapshot(before));
    }

    @Test
    @DisplayName("BE-SA12: reject a null advancement input")
    void nullStateIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> engine.advanceStreet(null));
    }

    @Test
    @DisplayName("BE-SA13: postflop actor selection skips folded/all-in/eliminated seats")
    void postflopActorsExcludeIneligibleSeats() {
        FoundationFixtures.Fixture fixture = closedRound(GamePhase.PREFLOP);
        fixture.player(1, 940, PlayerStatus.FOLDED, 60, 60);
        fixture.player(2, 0, PlayerStatus.ALL_IN, 60, 60);
        eliminate(fixture, 3);
        fixture.player(0, 2880, PlayerStatus.ACTIVE, 60, 60);
        GameState before = fixture.build();
        List<Object> original = snapshot(before);

        GameState after = engine.advanceStreet(before);

        assertEquals(4, after.getCurrentActorSeat().orElseThrow());
        assertEquals(Set.of("p0", "p4", "p5"), after.getBettingProgress().getPlayersToAct());
        assertEquals(Set.of("p0", "p4", "p5"), after.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(6, after.getPlayers().size());
        for (int seat = 0; seat < 6; seat++) {
            assertEquals(before.getPlayerStates().get(seat).getStatus(), after.getPlayerStates().get(seat).getStatus());
            assertEquals(before.getCommittedChips(seat), after.getCommittedChips(seat));
            assertEquals(0, after.getPlayerStates().get(seat).getRoundContribution());
        }
        assertEquals(300, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertEquals(original, snapshot(before));
    }

    @Test
    @DisplayName("BE-SA14: heads-up postflop action starts with the big blind")
    void headsUpActionStartsWithTheNonDealer() {
        FoundationFixtures.Fixture fixture = closedRound(GamePhase.PREFLOP);
        for (int seat = 1; seat <= 4; seat++) {
            eliminate(fixture, seat);
        }
        fixture.player(0, 2940, PlayerStatus.ACTIVE, 60, 60);
        fixture.player(5, 2940, PlayerStatus.ACTIVE, 60, 60);
        fixture.positions = new HandPositions(0, 0, 5);

        GameState after = engine.advanceStreet(fixture.build());

        assertEquals(5, after.getCurrentActorSeat().orElseThrow());
        assertEquals(Set.of("p0", "p5"), after.getBettingProgress().getPlayersToAct());
        assertEquals(Set.of("p0", "p5"), after.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(120, after.getPot());
        assertEquals(6000, after.getTotalChips());
    }

    @ParameterizedTest(name = "BE-SA15-{0}: fewer than two active players need no new decisions")
    @ValueSource(ints = {0, 1})
    void allInRunoutDoesNotCreateUnnecessaryChecks(int active) {
        FoundationFixtures.Fixture fixture = closedRound(GamePhase.PREFLOP);
        for (int seat = 0; seat < 6; seat++) {
            fixture.player(seat, 0, PlayerStatus.ALL_IN, 20, 20);
        }
        if (active == 0) {
            fixture.config = new GameConfig(20, 10, 20);
        } else {
            fixture.player(0, 5880, PlayerStatus.ACTIVE, 20, 20);
        }
        fixture.betting = new BettingProgress(20, 20, Set.of(), Set.of());
        GameState before = fixture.build();

        GameState after = engine.advanceStreet(before);

        assertEquals(GamePhase.FLOP, after.getPhase());
        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(6, after.getPlayerStates().stream().filter(PlayerState::isParticipating).count());
        assertEquals(active, after.getPlayerStates().stream().filter(PlayerState::canAct).count());
        assertEquals(120, after.getPot());
        assertEquals(active == 0 ? 120 : 6000, after.getTotalChips());
    }

    @Test
    @DisplayName("BE-SA16: river-to-showdown needs no cards and retains the river accounting")
    void showdownDoesNotBurnOrDealAnotherCard() {
        FoundationFixtures.Fixture fixture = closedRound(GamePhase.RIVER);
        fixture.deck.clear();
        GameState before = fixture.build();

        GameState after = engine.advanceStreet(before);

        assertEquals(GamePhase.SHOWDOWN, after.getPhase());
        assertEquals(before.getCommunityCards(), after.getCommunityCards());
        assertTrue(after.getRemainingDeck().isEmpty());
        assertEquals(40, after.getCurrentBet());
        assertEquals(40, after.getLastFullRaiseSize());
        assertEquals(600, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
    }

    private static FoundationFixtures.Fixture closedRound(GamePhase phase) {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.deal();
        Card[] cards = FoundationFixtures.cards();
        List<Card> board = List.of(cards[13], cards[14], cards[15], cards[17], cards[19]);
        int boardSize = phase == GamePhase.PREFLOP ? 0 : phase == GamePhase.FLOP ? 3
                : phase == GamePhase.TURN ? 4 : 5;
        int consumed = phase == GamePhase.PREFLOP ? 0 : phase == GamePhase.FLOP ? 4
                : phase == GamePhase.TURN ? 6 : 8;
        fixture.community = new ArrayList<>(board.subList(0, boardSize));
        fixture.deck = new ArrayList<>(Arrays.asList(cards).subList(12 + consumed, 52));
        fixture.phase = phase;
        long round = phase == GamePhase.PREFLOP ? 60 : 40;
        long hand = phase == GamePhase.PREFLOP ? 60 : 100;
        for (int seat = 0; seat < 6; seat++) {
            fixture.player(seat, 1000 - hand, PlayerStatus.ACTIVE, round, hand);
        }
        fixture.actor = null;
        fixture.betting = new BettingProgress(round, 40, Set.of(), Set.of());
        return fixture;
    }

    private static void eliminate(FoundationFixtures.Fixture fixture, int seat) {
        fixture.deck.addAll(fixture.players.get(seat).getHoleCards());
        fixture.player(seat, 0, PlayerStatus.ELIMINATED, 0, 0);
    }

    private static List<Object> snapshot(GameState state) {
        List<Object> values = new ArrayList<>(List.of(state.getHandId(), state.getPhase(),
                state.getConfig().getStartingChips(), state.getConfig().getSmallBlind(),
                state.getConfig().getBigBlind(), state.getDealerSeat(), state.getSmallBlindSeat(),
                state.getBigBlindSeat(), state.getCurrentActorId(), state.getCommunityCards(),
                state.getRemainingDeck(), state.getCurrentBet(), state.getLastFullRaiseSize(),
                state.getBettingProgress().getPlayersToAct(), state.getBettingProgress().getPlayersAllowedToRaise()));
        for (PlayerState player : state.getPlayerStates()) {
            values.add(List.of(player.getPlayer().getId(), player.getPlayer().getSeat(),
                    player.getPlayer().getType(), player.getPlayer().getChipStack(), player.getStatus(),
                    player.getHoleCards(), player.getRoundContribution(), player.getHandContribution()));
        }
        return values;
    }

    private static final class SeatZeroRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
