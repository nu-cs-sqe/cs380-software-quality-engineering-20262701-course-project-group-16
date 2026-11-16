package domain.model;

import application.GameInitializer;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Engine departure transitions with real initialized state fixtures. */
class DepartureBettingTest {
    private final BettingEngine engine = new BettingEngine();

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 4, 5})
    void db01OutOfTurnDeparturePreservesTheActorCardsAndChipOwnership(int seat) {
        GameState before = opening(new GameConfig());
        String quitter = before.getPlayers().get(seat).getId();
        BettingResult result = engine.foldDepartingPlayer(before, quitter);
        GameState after = result.getState();
        assertEquals(PlayerStatus.FOLDED, after.getPlayerStates().get(seat).getStatus());
        assertEquals(before.getCurrentActorId(), after.getCurrentActorId());
        assertPlayerOwnership(before, after);
        assertEquals(before.getRemainingDeck(), after.getRemainingDeck());
        assertEquals(before.getCommunityCards(), after.getCommunityCards());
        assertEquals(before.getPlayerStates().get(seat).getHoleCards(),
                after.getPlayerStates().get(seat).getHoleCards());
        assertEquals(before.getCommittedChips(seat), after.getCommittedChips(seat));
        assertEquals(before.getCurrentBet(), after.getCurrentBet());
        assertEquals(before.getLastFullRaiseSize(), after.getLastFullRaiseSize());
        assertFalse(after.getBettingProgress().getPlayersToAct().contains(quitter));
        assertFalse(after.getBettingProgress().getPlayersAllowedToRaise().contains(quitter));
        assertEquals(ResolvedActionType.FOLD, result.getAcceptedAction().getType());
        assertEquals(quitter, result.getAcceptedAction().getPlayerId());
        assertEquals(0, result.getAcceptedAction().getChipsAdded());
        assertEquals(before.getPlayerStates().get(seat).getRoundContribution(),
                result.getAcceptedAction().getRoundContributionAfter());
        assertFalse(result.getAcceptedAction().becameAllIn());
        assertEquals(PlayerStatus.ACTIVE, before.getPlayerStates().get(seat).getStatus());
        assertEquals(6000, after.getTotalChips());
    }

    @Test
    void db02CurrentDepartureAdvancesToTheNextPendingPlayer() {
        GameState before = opening(new GameConfig());
        BettingResult result = engine.foldDepartingPlayer(before,
                before.getCurrentActorId().orElseThrow());
        assertEquals(4, result.getState().getCurrentActorSeat().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "unknown"})
    void db03UnknownDepartureIsRejected(String player) {
        GameState before = opening(new GameConfig());
        assertThrows(IllegalArgumentException.class,
                () -> engine.foldDepartingPlayer(before, player));
        assertEquals(3, before.getCurrentActorSeat().orElseThrow());
        assertEquals(30, before.getPot());
    }

    @Test
    void db04AnAllInPlayerCannotBeFoldedByDeparture() {
        GameState before = opening(new GameConfig(20, 10, 20));
        assertEquals(PlayerStatus.ALL_IN, before.getPlayerStates().get(2).getStatus());
        assertThrows(IllegalArgumentException.class,
                () -> engine.foldDepartingPlayer(before, before.getPlayers().get(2).getId()));
        assertEquals(PlayerStatus.ALL_IN, before.getPlayerStates().get(2).getStatus());
    }

    @Test
    void db05AnAlreadyFoldedPlayerCannotFoldAgain() {
        GameState before = opening(new GameConfig());
        String quitter = before.getCurrentActorId().orElseThrow();
        GameState folded = engine.apply(before,
                new ActionRequest(quitter, ActionType.FOLD)).getState();
        assertThrows(IllegalArgumentException.class,
                () -> engine.foldDepartingPlayer(folded, quitter));
    }

    @Test
    void db06DepartureRequiresADealtBettingStreet() {
        GameState setup = new GameSetup().create(6);
        assertThrows(UnsupportedOperationException.class,
                () -> engine.foldDepartingPlayer(setup, setup.getPlayers().get(3).getId()));
    }

    @Test
    void db07TheLastParticipantCannotFoldAnAlreadyUncontestedHand() {
        GameState hand = opening(new GameConfig());
        for (int index = 0; index < 5; index++) {
            hand = engine.apply(hand, new ActionRequest(
                    hand.getCurrentActorId().orElseThrow(), ActionType.FOLD)).getState();
        }
        GameState uncontested = hand;
        assertTrue(uncontested.getCurrentActorId().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> engine.foldDepartingPlayer(
                uncontested, uncontested.getPlayers().get(2).getId()));
    }

    @Test
    void db08DepartureAfterRoundClosurePreservesExplicitActorAbsence() {
        GameState hand = opening(new GameConfig());
        for (int index = 0; index < 5; index++) {
            hand = engine.apply(hand, new ActionRequest(
                    hand.getCurrentActorId().orElseThrow(), ActionType.CALL)).getState();
        }
        hand = engine.apply(hand, new ActionRequest(
                hand.getCurrentActorId().orElseThrow(), ActionType.CHECK)).getState();
        String quitter = hand.getPlayers().get(0).getId();
        GameState after = engine.foldDepartingPlayer(hand, quitter).getState();
        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getLastDecisionTargets().containsKey(
                after.getPlayers().get(1).getId()));
        assertFalse(after.getBettingProgress().getLastDecisionTargets().containsKey(quitter));
        assertEquals(120, after.getPot());
        assertEquals(PlayerStatus.FOLDED, after.getPlayerStates().get(0).getStatus());
    }

    private static void assertPlayerOwnership(GameState before, GameState after) {
        for (int seat = 0; seat < before.getPlayers().size(); seat++) {
            Player original = before.getPlayers().get(seat);
            Player next = after.getPlayers().get(seat);
            assertEquals(original.getId(), next.getId());
            assertEquals(original.getSeat(), next.getSeat());
            assertEquals(original.getType(), next.getType());
            assertEquals(original.getChipStack(), next.getChipStack());
            assertEquals(before.getCommittedChips(seat), after.getCommittedChips(seat));
        }
    }

    private static GameState opening(GameConfig config) {
        Random dealer = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return 0; }
        };
        return new GameInitializer(config, dealer, new Random(380)).start(6);
    }
}
