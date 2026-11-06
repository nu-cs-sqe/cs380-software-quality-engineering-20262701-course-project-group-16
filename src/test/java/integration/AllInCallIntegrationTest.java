package integration;

import application.GameInitializer;
import domain.model.AcceptedAction;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.BettingResult;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.ResolvedActionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AllInCallIntegrationTest {
    private final BettingEngine engine = new BettingEngine();

    @Test
    @DisplayName("BE-AIC11: actual setup/dealing and five calls leave six all-in participants")
    void everyoneCanBecomeAllInWithoutAnExtraBlindDecision() {
        GameState initial = start(new GameConfig(20, 10, 20));
        assertEquals(PlayerStatus.ALL_IN, initial.getPlayerStates().get(2).getStatus());
        assertEquals(0, initial.getPlayers().get(2).getChipStack());
        assertFalse(initial.getBettingProgress().getPlayersToAct().contains(initial.getPlayers().get(2).getId()));
        GameState state = initial;

        for (int seat : new int[]{3, 4, 5, 0, 1}) {
            assertEquals(seat, state.getCurrentActorSeat().orElseThrow());
            BettingResult result = act(state, seat, ActionType.CALL);
            AcceptedAction action = result.getAcceptedAction();
            assertEquals(ResolvedActionType.CALL, action.getType());
            assertEquals(seat == 1 ? 10 : 20, action.getChipsAdded());
            assertEquals(20, action.getRoundContributionAfter());
            assertTrue(action.becameAllIn());
            state = result.getState();
            assertEquals(120, state.getTotalChips());
        }

        assertTrue(state.getCurrentActorId().isEmpty());
        assertTrue(state.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(state.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(120, state.getPot());
        assertEquals(6, state.getPlayerStates().stream().filter(PlayerState::isParticipating).count());
        assertEquals(0, state.getPlayerStates().stream().filter(PlayerState::canAct).count());
        for (int seat = 0; seat < 6; seat++) {
            PlayerState player = state.getPlayerStates().get(seat);
            assertEquals(PlayerStatus.ALL_IN, player.getStatus());
            assertEquals(0, player.getPlayer().getChipStack());
            assertEquals(20, player.getRoundContribution());
            assertEquals(20, player.getHandContribution());
            assertEquals(initial.getPlayerStates().get(seat).getHoleCards(), player.getHoleCards());
        }
        assertEquals(20, state.getCurrentBet());
        assertEquals(20, state.getLastFullRaiseSize());
        assertEquals(GamePhase.PREFLOP, state.getPhase());
        assertTrue(state.getCommunityCards().isEmpty());
        assertEquals(initial.getRemainingDeck(), state.getRemainingDeck());
        assertEquals(40, state.getRemainingDeck().size());
        assertEquals(30, initial.getPot());
        assertEquals(20, initial.getPlayers().get(3).getChipStack());
        assertEquals(PlayerStatus.ACTIVE, initial.getPlayerStates().get(3).getStatus());
        assertEquals(5, initial.getBettingProgress().getPlayersToAct().size());
        assertEquals(3, initial.getCurrentActorSeat().orElseThrow());
    }

    @Test
    @DisplayName("BE-AIC12: short all-in call plus fold/calls preserves the big blind option")
    void shortAllInCallLeavesTheBettingLevelAndOtherDecisionsUnchanged() {
        GameState initial = withShortStack(start(new GameConfig()));
        GameState state = act(initial, 3, ActionType.CALL).getState();
        assertEquals(49, state.getPot());
        assertEquals(20, state.getCurrentBet());
        assertEquals(PlayerStatus.ALL_IN, state.getPlayerStates().get(3).getStatus());

        state = act(state, 4, ActionType.FOLD).getState();
        for (int seat : new int[]{5, 0, 1}) {
            assertEquals(seat, state.getCurrentActorSeat().orElseThrow());
            state = act(state, seat, ActionType.CALL).getState();
            assertEquals(6000, state.getTotalChips());
        }
        assertEquals(2, state.getCurrentActorSeat().orElseThrow());
        assertEquals(1, state.getBettingProgress().getPlayersToAct().size());
        GameState after = act(state, 2, ActionType.CHECK).getState();

        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(99, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertEquals(19, after.getCommittedChips(3));
        assertEquals(0, after.getPlayers().get(3).getChipStack());
        assertEquals(PlayerStatus.ALL_IN, after.getPlayerStates().get(3).getStatus());
        assertEquals(PlayerStatus.FOLDED, after.getPlayerStates().get(4).getStatus());
        assertEquals(5, after.getPlayerStates().stream().filter(PlayerState::isParticipating).count());
        for (int seat : new int[]{0, 1, 2, 5}) {
            assertEquals(20, after.getPlayerStates().get(seat).getRoundContribution());
            assertEquals(20, after.getCommittedChips(seat));
        }
        assertEquals(20, after.getCurrentBet());
        assertEquals(20, after.getLastFullRaiseSize());
        assertEquals(GamePhase.PREFLOP, after.getPhase());
        assertTrue(after.getCommunityCards().isEmpty());
        assertEquals(initial.getRemainingDeck(), after.getRemainingDeck());
        for (int seat = 0; seat < 6; seat++) {
            assertEquals(initial.getPlayerStates().get(seat).getHoleCards(),
                    after.getPlayerStates().get(seat).getHoleCards());
        }
        assertEquals(30, initial.getPot());
        assertEquals(19, initial.getPlayers().get(3).getChipStack());
        assertEquals(0, initial.getCommittedChips(3));
        assertEquals(PlayerStatus.ACTIVE, initial.getPlayerStates().get(3).getStatus());
        assertEquals(3, initial.getCurrentActorSeat().orElseThrow());
    }

    private BettingResult act(GameState state, int seat, ActionType type) {
        return engine.apply(state, new ActionRequest(state.getPlayers().get(seat).getId(), type));
    }

    private static GameState start(GameConfig config) {
        Random dealer = new Random() {
            @Override public int nextInt(int bound) { return 0; }
        };
        return new GameInitializer(config, dealer, new Random(7)).start(2);
    }

    private static GameState withShortStack(GameState before) {
        List<PlayerState> players = new ArrayList<>(before.getPlayerStates());
        PlayerState caller = players.get(3);
        PlayerState donor = players.get(0);
        players.set(3, withStack(caller, 19));
        players.set(0, withStack(donor, 1981));
        return new GameState(before.getConfig(), before.getHandId(), before.getPhase(), players,
                before.getPositions(), before.getCurrentActorId().orElseThrow(),
                before.getCommunityCards(), before.getRemainingDeck(), before.getBettingProgress());
    }

    private static PlayerState withStack(PlayerState before, long stack) {
        Player player = before.getPlayer();
        return new PlayerState(new Player(player.getId(), player.getSeat(), player.getType(), stack),
                before.getStatus(), before.getHoleCards(), before.getRoundContribution(), before.getHandContribution());
    }
}
