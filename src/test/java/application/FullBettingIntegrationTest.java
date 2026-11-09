package application;

import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.GameConfig;
import domain.model.GamePhase;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FullBettingIntegrationTest {
    @Test
    void raisedPreflopAndBetsCallsRaisesOnLaterStreetsReachPaidStatistics() {
        SetupSession session = started();
        act(session, ActionType.RAISE, 40);
        while (view(session).getPhase() == GamePhase.PREFLOP) respond(session);
        act(session, ActionType.BET, 20); // seat1
        act(session, ActionType.RAISE, 60); // seat2
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL, 0);
        assertEquals(GamePhase.TURN, view(session).getPhase());
        assertEquals(600, view(session).getPot());
        act(session, ActionType.BET, 80);
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL, 0);
        assertEquals(GamePhase.RIVER, view(session).getPhase());
        act(session, ActionType.BET, 100); // seat1
        act(session, ActionType.FOLD, 0); // seat2
        act(session, ActionType.CALL, 0); // seat3
        act(session, ActionType.FOLD, 0); // seat4
        act(session, ActionType.CALL, 0); // seat5
        act(session, ActionType.CALL, 0); // seat0
        assertEquals(1480, view(session).getPot());
        assertEquals(4, session.getResult().orElseThrow().getWinnerIds().size());
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 0));
        session.settleHand();
        for (int seat : new int[]{0, 1, 3, 5}) {
            assertEquals(1090, view(session).getPlayers().get(seat).getChipStack());
            assertEquals(100, stats(session, seat).getRates().get(9).orElseThrow());
        }
        assertEquals(820, view(session).getPlayers().get(2).getChipStack());
        assertEquals(820, view(session).getPlayers().get(4).getChipStack());
        assertEquals(100, stats(session, 3).getRates().get(1).orElseThrow());
        assertEquals(3, stats(session, 1).getRates().get(4).orElseThrow());
        assertEquals(75, stats(session, 1).getRates().get(5).orElseThrow());
        assertEquals(0, stats(session, 2).getRates().get(8).orElseThrow());
        assertTrue(stats(session, 2).getRates().get(9).isEmpty());
        assertEquals(1, stats(session, 0).getHandsPlayed());
        session.nextHand();
        assertEquals(1, stats(session, 0).getHandsPlayed());
        assertEquals(6000, view(session).getPot() + view(session).getPlayers().stream()
                .mapToLong(p -> p.getChipStack()).sum());
    }

    @Test
    void actualThreeBetAndContinuationBetHistoryFeedExistingStatistics() {
        SetupSession session = started();
        act(session, ActionType.RAISE, 40); // seat3 opens
        act(session, ActionType.RAISE, 80); // seat4 three-bets
        while (view(session).getPhase() == GamePhase.PREFLOP) respond(session);
        assertEquals(480, view(session).getPot());
        for (int i = 0; i < 3; i++) act(session, ActionType.CHECK, 0);
        act(session, ActionType.BET, 40); // final aggressor seat4
        act(session, ActionType.FOLD, 0); // seat5 faces actual c-bet
        for (int i = 0; i < 4; i++) act(session, ActionType.CALL, 0);
        while (session.getResult().isEmpty()) act(session, ActionType.CHECK, 0);
        assertEquals(680, view(session).getPot());
        session.settleHand();
        assertEquals(100, stats(session, 4).getRates().get(3).orElseThrow());
        assertEquals(100, stats(session, 4).getRates().get(6).orElseThrow());
        assertEquals(100, stats(session, 5).getRates().get(7).orElseThrow());
        assertEquals(0, stats(session, 0).getRates().get(7).orElseThrow());
    }

    private static SetupSession started() {
        Random dealer = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return 0; }
        };
        Random deck = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return bound - 1; }
        };
        SetupSession session = new SetupSession(new GameInitializer(new GameConfig(), dealer, deck));
        session.start(6);
        return session;
    }

    private static StatisticsView stats(SetupSession session, int seat) {
        return session.getStatistics().get(view(session).getPlayers().get(seat).getId());
    }

    private static SetupView view(SetupSession session) { return session.getView().orElseThrow(); }

    private static void respond(SetupSession session) {
        act(session, session.getAvailableActions().contains(ActionType.CALL)
                ? ActionType.CALL : ActionType.CHECK, 0);
    }

    private static void act(SetupSession session, ActionType type, long target) {
        SetupView before = view(session);
        String id = before.getPlayers().get(before.getCurrentActorSeat()).getId();
        session.submitAction(type == ActionType.BET || type == ActionType.RAISE
                ? new ActionRequest(id, type, target) : new ActionRequest(id, type));
        assertEquals(6000, view(session).getPot() + view(session).getPlayers().stream()
                .mapToLong(p -> p.getChipStack()).sum());
        assertTrue(view(session).getHoleCards().isEmpty());
    }
}
