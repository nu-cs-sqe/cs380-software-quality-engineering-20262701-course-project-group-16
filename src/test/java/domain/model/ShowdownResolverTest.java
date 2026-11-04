package domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static domain.model.PokerTestCards.cards;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowdownResolverTest {
    private static final String BOARD = "2C 3D 4H 8S 9C";
    private static final String[] HANDS = {"AH AD", "KH KD", "QH QD", "JH JD", "TH TD", "6H 6D"};

    @Test void SR_C01_rejectsNullEvaluator() {
        assertThrows(NullPointerException.class, () -> new ShowdownResolver(null));
    }

    @Test void SR_N01_rejectsNullState() {
        assertThrows(NullPointerException.class, () -> new ShowdownResolver().resolve(null));
    }

    @Test void SR_V01_realEvaluatorFindsWinnerWithoutSettling() {
        GameState state = state(GamePhase.SHOWDOWN, statuses(), HANDS, BOARD);
        List<PlayerState> players = state.getPlayerStates();
        ShowdownResult result = new ShowdownResolver().resolve(state);
        assertNotNull(result);
        assertEquals("result-hand", result.getHandId());
        assertEquals(List.of("p0"), result.getWinnerIds());
        assertFalse(result.isUncontested());
        assertEquals(ShowdownResult.Status.AWAITING_SETTLEMENT, result.getStatus());
        assertSame(players, state.getPlayerStates());
        assertEquals(60, state.getPot());
        assertEquals(6000, state.getTotalChips());
        assertEquals(GamePhase.SHOWDOWN, state.getPhase());
        assertEquals(cards(BOARD), state.getCommunityCards());
        assertThrows(UnsupportedOperationException.class, () -> result.getWinnerIds().clear());
    }

    @Test void SR_V02_includesAllInButNotFoldedOrEliminated() {
        PlayerStatus[] status = statuses();
        status[0] = PlayerStatus.FOLDED;
        status[1] = PlayerStatus.ALL_IN;
        status[2] = PlayerStatus.ELIMINATED;
        GameState state = state(GamePhase.SHOWDOWN, status, HANDS, BOARD);
        ShowdownResult result = new ShowdownResolver().resolve(state);
        assertNotNull(result);
        assertEquals(List.of("p1"), result.getWinnerIds());
        assertEquals(6000, state.getTotalChips());
    }

    @Test void SR_V03_sharedBoardCanTieAllSixPlayers() {
        ShowdownResult result = new ShowdownResolver().resolve(
                state(GamePhase.SHOWDOWN, statuses(), HANDS, "AC KC QC JC TC"));
        assertNotNull(result);
        assertEquals(List.of("p0", "p1", "p2", "p3", "p4", "p5"), result.getWinnerIds());
        assertFalse(result.isUncontested());
    }

    @Test void SR_V04_twoWinnersRetainedInSeatOrder() {
        String[] holdings = HANDS.clone();
        holdings[1] = "AS AC";
        ShowdownResult result = new ShowdownResolver().resolve(
                state(GamePhase.SHOWDOWN, statuses(), holdings, BOARD));
        assertNotNull(result);
        assertEquals(List.of("p0", "p1"), result.getWinnerIds());
    }

    @Test void SR_V05_laterStrongerHandClearsEarlierTies() {
        String[] holdings = {"AH AD", "AS AC", "5C 6C", "QH QD", "JH JD", "TH TD"};
        ShowdownResult result = new ShowdownResolver().resolve(
                state(GamePhase.SHOWDOWN, statuses(), holdings, "2C 3C 4C 8S 9D"));
        assertNotNull(result);
        assertEquals(List.of("p2"), result.getWinnerIds());
    }

    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = {"PREFLOP", "FLOP", "TURN", "RIVER", "SHOWDOWN"})
    void SR_U01_uncontestedNeedsNoEvaluationOrCompleteBoard(GamePhase phase) {
        PlayerStatus[] status = statuses();
        Arrays.fill(status, PlayerStatus.FOLDED);
        status[4] = PlayerStatus.ACTIVE;
        HandEvaluator mustNotRun = (first, second) -> {
            throw new AssertionError("An uncontested result needs no hand comparison.");
        };
        GameState state = state(phase, status, HANDS, BOARD);
        ShowdownResult result = new ShowdownResolver(mustNotRun).resolve(state);
        assertNotNull(result);
        assertEquals(List.of("p4"), result.getWinnerIds());
        assertTrue(result.isUncontested());
        assertEquals(ShowdownResult.Status.AWAITING_SETTLEMENT, result.getStatus());
        assertEquals(6000, state.getTotalChips());
    }

    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = {"SETUP", "COMPLETION", "PREFLOP", "FLOP", "TURN", "RIVER"})
    void SR_I01_rejectsUnreadyOrCompletedContestedStates(GamePhase phase) {
        GameState state = state(phase, statuses(), HANDS, BOARD);
        assertThrows(IllegalStateException.class, () -> new ShowdownResolver().resolve(state));
        assertEquals(6000, state.getTotalChips());
    }

    @Test void SR_I02_rejectsNoEligiblePlayer() {
        PlayerStatus[] status = statuses();
        Arrays.fill(status, PlayerStatus.FOLDED);
        GameState state = state(GamePhase.SHOWDOWN, status, HANDS, BOARD);
        assertThrows(IllegalStateException.class, () -> new ShowdownResolver().resolve(state));
    }

    @Test void SR_I03_rejectsPendingActionBeforeUncontestedResult() {
        PlayerStatus[] status = statuses();
        Arrays.fill(status, PlayerStatus.FOLDED);
        status[0] = PlayerStatus.ACTIVE;
        GameState base = state(GamePhase.PREFLOP, status, HANDS, BOARD);
        GameState pending = new GameState(base.getConfig(), base.getHandId(), base.getPhase(),
                base.getPlayerStates(), base.getPositions(), "p0", base.getCommunityCards(),
                base.getRemainingDeck(), new BettingProgress(0, 20, Set.of("p0"), Set.of()));
        assertThrows(IllegalStateException.class, () -> new ShowdownResolver().resolve(pending));
    }

    @Test void SR_I04_rejectsUnclearedRaiseRights() {
        PlayerStatus[] status = statuses();
        Arrays.fill(status, PlayerStatus.FOLDED);
        status[0] = PlayerStatus.ACTIVE;
        GameState base = state(GamePhase.PREFLOP, status, HANDS, BOARD);
        GameState pending = new GameState(base.getConfig(), base.getHandId(), base.getPhase(),
                base.getPlayerStates(), base.getPositions(), null, base.getCommunityCards(),
                base.getRemainingDeck(), new BettingProgress(0, 20, Set.of(), Set.of("p0")));
        assertThrows(IllegalStateException.class, () -> new ShowdownResolver().resolve(pending));
    }

    private static PlayerStatus[] statuses() {
        PlayerStatus[] result = new PlayerStatus[6];
        Arrays.fill(result, PlayerStatus.ACTIVE);
        return result;
    }

    private static GameState state(GamePhase phase, PlayerStatus[] statuses,
            String[] holdings, String boardText) {
        List<PlayerState> players = new ArrayList<>();
        long eliminatedChips = Arrays.stream(statuses).filter(s -> s == PlayerStatus.ELIMINATED).count() * 1000;
        for (int seat = 0; seat < 6; seat++) {
            PlayerStatus status = statuses[seat];
            long committed = phase == GamePhase.COMPLETION || phase == GamePhase.SETUP
                    || status == PlayerStatus.ELIMINATED ? 0 : status == PlayerStatus.ALL_IN ? 1000 : 10;
            long stack = status == PlayerStatus.ELIMINATED ? 0 : 1000 - committed;
            if (eliminatedChips > 0 && status == PlayerStatus.ACTIVE) {
                stack += eliminatedChips;
                eliminatedChips = 0;
            }
            List<Card> hand = phase == GamePhase.SETUP || status == PlayerStatus.ELIMINATED
                    ? List.of() : cards(holdings[seat]);
            players.add(new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN, stack),
                    status, hand, 0, committed));
        }
        int boardSize = phase == GamePhase.SETUP || phase == GamePhase.PREFLOP ? 0
                : phase == GamePhase.FLOP ? 3 : phase == GamePhase.TURN ? 4 : 5;
        return new GameState(new GameConfig(), "result-hand", phase, players,
                new SeatOrder(players).positionsFor(0), null, cards(boardText).subList(0, boardSize),
                List.of(), new BettingProgress(0, 20, Set.of(), Set.of()));
    }
}
