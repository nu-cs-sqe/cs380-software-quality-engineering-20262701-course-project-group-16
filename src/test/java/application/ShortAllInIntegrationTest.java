package application;

import domain.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShortAllInIntegrationTest {
    @Test
    void cumulativeReopeningRunsThroughTheRealSessionAndSettlement() {
        SetupSession session = started(60, 1000, 1000, 1000, 50, 2890);
        act(session, ActionType.RAISE, 40); // seat3
        act(session, ActionType.RAISE, 50); // seat4 short all-in
        act(session, ActionType.CALL, 0); // seat5 at50
        act(session, ActionType.RAISE, 60); // seat0 short all-in
        act(session, ActionType.FOLD, 0); // seat1
        act(session, ActionType.FOLD, 0); // seat2
        assertEquals(3, view(session).getCurrentActorSeat());
        int count = session.getAcceptedActions().size();
        assertThrows(IllegalArgumentException.class, () -> act(session, ActionType.RAISE, 79));
        assertEquals(count, session.getAcceptedActions().size());
        assertEquals(3, view(session).getCurrentActorSeat());
        act(session, ActionType.RAISE, 80); // seat3 reopened at60
        act(session, ActionType.CALL, 0); // seat5
        assertEquals(GamePhase.FLOP, view(session).getPhase());
        assertEquals(300, view(session).getPot());
        for (int countDown = 0; countDown < 6 && session.getResult().isEmpty(); countDown++) {
            act(session, ActionType.CHECK, 0);
        }
        assertTrue(session.getResult().isPresent());
        assertEquals(4, session.getResult().orElseThrow().getWinnerIds().size());
        session.settleHand();
        assertEquals(67, view(session).getPlayers().get(0).getChipStack());
        assertEquals(58, view(session).getPlayers().get(4).getChipStack());
        assertEquals(1008, view(session).getPlayers().get(3).getChipStack());
        assertEquals(2897, view(session).getPlayers().get(5).getChipStack());
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 1));
        session.nextHand();
        assertEquals(GamePhase.PREFLOP, view(session).getPhase());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertEquals(6000, total(session));
    }

    @Test
    void shortAllInsAndCappedCallsRunOutWithoutExtraActiveDecisions() {
        SetupSession session = started(5901, 15, 20, 21, 22, 21);
        AcceptedAction first = act(session, ActionType.RAISE, 21);
        assertTrue(first.becameAllIn());
        act(session, ActionType.RAISE, 22);
        AcceptedAction call = act(session, ActionType.CALL, 0); // seat5 has21
        assertEquals(ResolvedActionType.CALL, call.getType());
        assertEquals(21, call.getChipsAdded());
        assertTrue(call.becameAllIn());
        act(session, ActionType.CALL, 0); // seat0 matches22
        act(session, ActionType.CALL, 0); // seat1 uses remaining5
        assertEquals(GamePhase.SHOWDOWN, view(session).getPhase());
        assertEquals(-1, view(session).getCurrentActorSeat());
        assertEquals(121, view(session).getPot());
        assertEquals(5, session.getAcceptedActions().size());
        assertTrue(session.getResult().isPresent());
        session.settleHand();
        assertEquals(GamePhase.COMPLETION, view(session).getPhase());
        assertEquals(6000, total(session));
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 1));
    }

    private static SetupSession started(long... stacks) {
        Random dealer = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return 0; }
        };
        Random deck = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return bound - 1; }
        };
        GameSetup setup = new GameSetup(new GameConfig(), dealer);
        GameInitializer initializer = new GameInitializer(humans -> restack(setup.create(humans), stacks),
                new InitialDeal(deck)::deal);
        SetupSession session = new SetupSession(initializer);
        session.start(6);
        return session;
    }

    private static GameState restack(GameState state, long[] stacks) {
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState before : state.getPlayerStates()) {
            Player old = before.getPlayer();
            long remaining = stacks[old.getSeat()] - before.getHandContribution();
            players.add(new PlayerState(new Player(old.getId(), old.getSeat(), old.getType(), remaining),
                    remaining == 0 ? PlayerStatus.ALL_IN : PlayerStatus.ACTIVE, List.of(),
                    before.getRoundContribution(), before.getHandContribution()));
        }
        java.util.Set<String> pending = new java.util.HashSet<>();
        players.stream().filter(PlayerState::canAct).forEach(p -> pending.add(p.getPlayer().getId()));
        return new GameState(state.getConfig(), state.getHandId(), state.getPhase(), players,
                state.getPositions(), state.getCurrentActorId().orElseThrow(), List.of(), List.of(),
                new BettingProgress(20, 20, pending, pending));
    }

    private static AcceptedAction act(SetupSession session, ActionType type, long target) {
        SetupView before = view(session);
        String actor = before.getPlayers().get(before.getCurrentActorSeat()).getId();
        AcceptedAction result = session.submitAction(type == ActionType.BET || type == ActionType.RAISE
                ? new ActionRequest(actor, type, target) : new ActionRequest(actor, type));
        assertEquals(6000, total(session));
        assertTrue(view(session).getHoleCards().isEmpty());
        return result;
    }

    private static long total(SetupSession session) {
        return view(session).getPot() + view(session).getPlayers().stream().mapToLong(p -> p.getChipStack()).sum();
    }

    private static SetupView view(SetupSession session) { return session.getView().orElseThrow(); }
}
