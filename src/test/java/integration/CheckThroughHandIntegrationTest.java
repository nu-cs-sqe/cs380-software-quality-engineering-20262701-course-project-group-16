package integration;

import application.GameInitializer;
import domain.model.AcceptedAction;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.BettingResult;
import domain.model.BettingStreet;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.PlayerStatus;
import domain.model.ResolvedActionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class CheckThroughHandIntegrationTest {
    private final BettingEngine engine = new BettingEngine();

    @ParameterizedTest(name = "{0}: complete controlled hand, flop fold = {1}")
    @CsvSource({"BE-PFI01, false, 21", "BE-PFI02, true, 19"})
    void setupAndPlayerActionsReachShowdownWithConservedChips(String caseId,
            boolean foldOnFlop, int expectedActions) {
        Random dealer = new SeatZeroRandom();
        GameState initial = new GameInitializer(new GameConfig(), dealer, new Random(7)).start(2);
        GameState state = initial;
        List<AcceptedAction> accepted = new ArrayList<>();
        int[] preflopSeats = {3, 4, 5, 0, 1, 2};
        ActionType[] preflopActions = {ActionType.CALL, ActionType.FOLD, ActionType.CALL,
                ActionType.CALL, ActionType.CALL, ActionType.CHECK};
        for (int index = 0; index < preflopSeats.length; index++) {
            int seat = preflopSeats[index];
            assertEquals(seat, state.getCurrentActorSeat().orElseThrow());
            BettingResult result = act(state, seat, preflopActions[index]);
            assertEquals(BettingStreet.PREFLOP, result.getAcceptedAction().getStreet());
            accepted.add(result.getAcceptedAction());
            state = result.getState();
            assertEquals(6000, state.getTotalChips());
        }
        GameState closedPreflop = state;
        assertEquals(100, state.getPot());

        for (GamePhase street : new GamePhase[]{GamePhase.FLOP, GamePhase.TURN, GamePhase.RIVER}) {
            state = engine.advanceStreet(state);
            assertEquals(street, state.getPhase());
            assertEquals(1, state.getCurrentActorSeat().orElseThrow());
            int[] order = foldOnFlop && street != GamePhase.FLOP
                    ? new int[]{1, 3, 5, 0} : new int[]{1, 2, 3, 5, 0};
            for (int seat : order) {
                assertEquals(seat, state.getCurrentActorSeat().orElseThrow());
                ActionType type = foldOnFlop && street == GamePhase.FLOP && seat == 2
                        ? ActionType.FOLD : ActionType.CHECK;
                BettingResult result = act(state, seat, type);
                AcceptedAction action = result.getAcceptedAction();
                assertEquals(expectedStreet(street), action.getStreet());
                assertEquals(type == ActionType.CHECK ? ResolvedActionType.CHECK : ResolvedActionType.FOLD,
                        action.getType());
                assertEquals(state.getHandId(), action.getHandId());
                assertEquals(state.getPlayers().get(seat).getId(), action.getPlayerId());
                assertEquals(0, action.getChipsAdded());
                assertEquals(0, action.getRoundContributionAfter());
                assertFalse(action.becameAllIn());
                accepted.add(action);
                state = result.getState();
                assertEquals(100, state.getPot());
                assertEquals(6000, state.getTotalChips());
                assertEquals(0, state.getCurrentBet());
                for (int player = 0; player < 6; player++) {
                    assertEquals(0, state.getPlayerStates().get(player).getRoundContribution());
                }
            }
            assertTrue(state.getCurrentActorId().isEmpty());
            assertTrue(state.getBettingProgress().getPlayersToAct().isEmpty());
            assertTrue(state.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        }
        GameState showdown = engine.advanceStreet(state);

        assertEquals(expectedActions, accepted.size(), caseId);
        assertEquals(GamePhase.SHOWDOWN, showdown.getPhase());
        assertTrue(showdown.getCurrentActorId().isEmpty());
        assertTrue(showdown.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(showdown.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(100, showdown.getPot());
        assertEquals(5900, showdown.getPlayers().stream().mapToLong(player -> player.getChipStack()).sum());
        assertEquals(6000, showdown.getTotalChips());
        assertEquals(List.of(initial.getRemainingDeck().get(1), initial.getRemainingDeck().get(2),
                initial.getRemainingDeck().get(3), initial.getRemainingDeck().get(5),
                initial.getRemainingDeck().get(7)), showdown.getCommunityCards());
        assertEquals(initial.getRemainingDeck().subList(8, 40), showdown.getRemainingDeck());
        for (int seat = 0; seat < 6; seat++) {
            assertEquals(initial.getPlayers().get(seat).getId(), showdown.getPlayers().get(seat).getId());
            assertEquals(initial.getPlayerStates().get(seat).getHoleCards(), showdown.getPlayerStates().get(seat).getHoleCards());
            assertEquals(seat == 4 ? 0 : 20, showdown.getCommittedChips(seat));
            assertEquals(seat == 4 ? 1000 : 980, showdown.getPlayers().get(seat).getChipStack());
            PlayerStatus expected = seat == 4 || (foldOnFlop && seat == 2)
                    ? PlayerStatus.FOLDED : PlayerStatus.ACTIVE;
            assertEquals(expected, showdown.getPlayerStates().get(seat).getStatus());
        }
        assertTrue(showdown.getBettingHistory().getFinalPreflopAggressorId().isEmpty());
        assertTrue(showdown.getBettingHistory().getFlopCBetAmount().isEmpty());
        assertEquals(GamePhase.PREFLOP, closedPreflop.getPhase());
        assertEquals(20, closedPreflop.getPlayerStates().get(2).getRoundContribution());
        assertEquals(PlayerStatus.ACTIVE, closedPreflop.getPlayerStates().get(2).getStatus());
        assertEquals(100, closedPreflop.getPot());
        assertEquals(30, initial.getPot());
        assertEquals(3, initial.getCurrentActorSeat().orElseThrow());
        assertTrue(initial.getCommunityCards().isEmpty());
        assertEquals(40, initial.getRemainingDeck().size());
    }

    private BettingResult act(GameState state, int seat, ActionType type) {
        return engine.apply(state, new ActionRequest(state.getPlayers().get(seat).getId(), type));
    }

    private static BettingStreet expectedStreet(GamePhase phase) {
        switch (phase) {
            case FLOP: return BettingStreet.FLOP;
            case TURN: return BettingStreet.TURN;
            case RIVER: return BettingStreet.RIVER;
            default: throw new IllegalArgumentException("Expected a postflop betting street.");
        }
    }

    private static final class SeatZeroRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
