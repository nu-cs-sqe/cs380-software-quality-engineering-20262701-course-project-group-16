package integration;

import application.GameInitializer;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.BettingProgress;
import domain.model.Card;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class StreetAdvancementIntegrationTest {
    private final BettingEngine engine = new BettingEngine();

    @Test
    @DisplayName("BE-SAI01: actual dealing, short all-in/fold/calls/check, then flop")
    void aClosedShortAllInRoundAdvancesWithoutLosingCommittedChips() {
        GameState initial = start(new GameConfig());
        List<PlayerState> players = new ArrayList<>(initial.getPlayerStates());
        players.set(3, playerWith(players.get(3), 19, PlayerStatus.ACTIVE, 0));
        players.set(0, playerWith(players.get(0), 1981, PlayerStatus.ACTIVE, 0));
        GameState state = copyWith(initial, players, initial.getCurrentActorId().orElseThrow(),
                initial.getBettingProgress());
        int[] seats = {3, 4, 5, 0, 1, 2};
        ActionType[] actions = {ActionType.CALL, ActionType.FOLD, ActionType.CALL,
                ActionType.CALL, ActionType.CALL, ActionType.CHECK};
        for (int index = 0; index < seats.length; index++) {
            assertEquals(seats[index], state.getCurrentActorSeat().orElseThrow());
            state = engine.apply(state, new ActionRequest(state.getPlayers().get(seats[index]).getId(),
                    actions[index])).getState();
        }
        GameState closed = state;

        GameState flop = engine.advanceStreet(closed);

        assertEquals(GamePhase.FLOP, flop.getPhase());
        assertEquals(closed.getRemainingDeck().subList(1, 4), flop.getCommunityCards());
        assertEquals(closed.getRemainingDeck().subList(4, 40), flop.getRemainingDeck());
        assertEquals(1, flop.getCurrentActorSeat().orElseThrow());
        Set<String> active = Set.of(flop.getPlayers().get(0).getId(), flop.getPlayers().get(1).getId(),
                flop.getPlayers().get(2).getId(), flop.getPlayers().get(5).getId());
        assertEquals(active, flop.getBettingProgress().getPlayersToAct());
        assertEquals(active, flop.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(0, flop.getCurrentBet());
        assertEquals(20, flop.getLastFullRaiseSize());
        assertEquals(99, flop.getPot());
        assertEquals(6000, flop.getTotalChips());
        assertEquals(5, flop.getPlayerStates().stream().filter(PlayerState::isParticipating).count());
        assertEquals(PlayerStatus.ALL_IN, flop.getPlayerStates().get(3).getStatus());
        assertEquals(19, flop.getCommittedChips(3));
        assertEquals(PlayerStatus.FOLDED, flop.getPlayerStates().get(4).getStatus());
        assertEquals(0, flop.getCommittedChips(4));
        for (int seat = 0; seat < 6; seat++) {
            assertEquals(0, flop.getPlayerStates().get(seat).getRoundContribution());
            assertEquals(closed.getCommittedChips(seat), flop.getCommittedChips(seat));
            assertEquals(closed.getPlayers().get(seat).getChipStack(), flop.getPlayers().get(seat).getChipStack());
            assertEquals(closed.getPlayerStates().get(seat).getHoleCards(), flop.getPlayerStates().get(seat).getHoleCards());
        }
        assertEquals(GamePhase.PREFLOP, closed.getPhase());
        assertEquals(40, closed.getRemainingDeck().size());
        assertTrue(closed.getCommunityCards().isEmpty());
        assertEquals(19, closed.getPlayerStates().get(3).getRoundContribution());
        assertEquals(20, closed.getPlayerStates().get(1).getRoundContribution());
        assertEquals(99, closed.getPot());
        assertEquals(30, initial.getPot());
        assertEquals(1000, initial.getPlayers().get(3).getChipStack());
    }

    @ParameterizedTest(name = "BE-SAI02-{0}: run out the board with {0} active players")
    @ValueSource(ints = {0, 1})
    void anAllInRunoutReachesShowdownWithoutPlayerActionsOrPayout(int active) {
        GameState closed;
        if (active == 0) {
            closed = start(new GameConfig(20, 10, 20));
            for (int seat : new int[]{3, 4, 5, 0, 1}) {
                closed = engine.apply(closed, new ActionRequest(closed.getPlayers().get(seat).getId(),
                        ActionType.CALL)).getState();
            }
        } else {
            GameState dealt = start(new GameConfig());
            List<PlayerState> players = new ArrayList<>();
            for (int seat = 0; seat < 6; seat++) {
                players.add(playerWith(dealt.getPlayerStates().get(seat), seat == 0 ? 5880 : 0,
                        seat == 0 ? PlayerStatus.ACTIVE : PlayerStatus.ALL_IN, 20));
            }
            closed = copyWith(dealt, players, null, new BettingProgress(20, 20, Set.of(), Set.of()));
        }
        List<Card> deck = closed.getRemainingDeck();
        GameState state = closed;

        for (GamePhase next : new GamePhase[]{GamePhase.FLOP, GamePhase.TURN, GamePhase.RIVER, GamePhase.SHOWDOWN}) {
            state = engine.advanceStreet(state);
            assertEquals(next, state.getPhase());
            assertTrue(state.getCurrentActorId().isEmpty());
            assertTrue(state.getBettingProgress().getPlayersToAct().isEmpty());
            assertTrue(state.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
            assertEquals(active, state.getPlayerStates().stream().filter(PlayerState::canAct).count());
            assertEquals(6, state.getPlayerStates().stream().filter(PlayerState::isParticipating).count());
            assertEquals(120, state.getPot());
            assertEquals(active == 0 ? 120 : 6000, state.getTotalChips());
        }

        assertEquals(List.of(deck.get(1), deck.get(2), deck.get(3), deck.get(5), deck.get(7)),
                state.getCommunityCards());
        assertEquals(deck.subList(8, 40), state.getRemainingDeck());
        for (int seat = 0; seat < 6; seat++) {
            PlayerState player = state.getPlayerStates().get(seat);
            assertEquals(closed.getPlayerStates().get(seat).getStatus(), player.getStatus());
            assertEquals(closed.getPlayers().get(seat).getChipStack(), player.getPlayer().getChipStack());
            assertEquals(closed.getPlayerStates().get(seat).getHoleCards(), player.getHoleCards());
            assertEquals(0, player.getRoundContribution());
            assertEquals(20, player.getHandContribution());
        }
        assertEquals(GamePhase.PREFLOP, closed.getPhase());
        assertTrue(closed.getCommunityCards().isEmpty());
        assertEquals(40, closed.getRemainingDeck().size());
        assertEquals(20, closed.getPlayerStates().get(3).getRoundContribution());
        assertEquals(120, closed.getPot());
    }

    private static GameState start(GameConfig config) {
        Random dealer = new Random() {
            @Override public int nextInt(int bound) { return 0; }
        };
        return new GameInitializer(config, dealer, new Random(7)).start(2);
    }

    private static GameState copyWith(GameState before, List<PlayerState> players, String actor,
            BettingProgress progress) {
        return new GameState(before.getConfig(), before.getHandId(), before.getPhase(), players,
                before.getPositions(), actor, before.getCommunityCards(), before.getRemainingDeck(), progress);
    }

    private static PlayerState playerWith(PlayerState before, long stack, PlayerStatus status, long contribution) {
        Player player = before.getPlayer();
        return new PlayerState(new Player(player.getId(), player.getSeat(), player.getType(), stack),
                status, before.getHoleCards(), contribution, contribution);
    }
}
