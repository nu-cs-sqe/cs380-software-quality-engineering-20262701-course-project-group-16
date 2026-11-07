package application;

import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.Settlement;
import domain.model.ShowdownResolver;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompletedStatisticsIntegrationTest {
    @Test
    void settlementCompletesOnceAndNextHandKeepsIdentityChipsAndTotals() {
        SetupSession session = session();
        assertTrue(session.getStatistics().isEmpty());
        assertThrows(IllegalStateException.class, session::settleHand);
        session.start(6);
        List<String> ids = view(session).getPlayers().stream().map(p -> p.getId())
                .collect(java.util.stream.Collectors.toList());
        assertEquals(0, session.getStatistics().get(ids.get(0)).getHandsPlayed());
        assertThrows(IllegalStateException.class, session::settleHand);
        assertThrows(IllegalStateException.class, session::nextHand);
        for (int i = 0; i < 5; i++) act(session, ActionType.FOLD);
        var pending = session.getStatistics();
        Settlement paid = session.settleHand();
        assertEquals(GamePhase.COMPLETION, view(session).getPhase());
        assertEquals(0, view(session).getPot());
        assertEquals(1010, view(session).getPlayers().get(2).getChipStack());
        assertEquals(10L, paid.getRefunds().get(ids.get(2)));
        assertEquals(20L, paid.getPayouts().get(ids.get(2)));
        assertEquals(0, pending.get(ids.get(0)).getHandsPlayed());
        for (var stats : session.getStatistics().values()) {
            assertEquals(1, stats.getHandsPlayed());
            assertEquals(0, stats.getRates().get(0).orElseThrow());
            assertTrue(stats.getRates().get(9).isEmpty());
        }
        assertThrows(IllegalStateException.class, session::settleHand);
        var first = session.getStatistics();
        String hand = view(session).getHandId();
        session.nextHand();
        assertNotEquals(hand, view(session).getHandId());
        assertEquals(ids, view(session).getPlayers().stream().map(p -> p.getId())
                .collect(java.util.stream.Collectors.toList()));
        assertEquals(1, view(session).getPositions().getDealerSeat());
        assertEquals(40, view(session).getRemainingCardCount());
        assertTrue(view(session).getHoleCards().isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertTrue(session.getSettlement().isEmpty());
        assertEquals(1, session.getStatistics().get(ids.get(0)).getHandsPlayed());
        for (int i = 0; i < 5; i++) act(session, ActionType.FOLD);
        session.settleHand();
        assertEquals(2, session.getStatistics().get(ids.get(0)).getHandsPlayed());
        assertEquals(1, first.get(ids.get(0)).getHandsPlayed());
        assertThrows(UnsupportedOperationException.class, () -> first.clear());
        session.start(6);
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 0));
    }

    @Test
    void realShowdownCountsCallsSeeingFlopAndTiedPayouts() {
        SetupSession session = session();
        session.start(6);
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL);
        act(session, ActionType.CHECK);
        for (int i = 0; i < 18; i++) act(session, ActionType.CHECK);
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 0));
        session.settleHand();
        for (int seat = 0; seat < 6; seat++) {
            var stats = session.getStatistics().get(view(session).getPlayers().get(seat).getId());
            assertEquals(1, stats.getHandsPlayed());
            assertEquals(seat == 2 ? 0 : 100, stats.getRates().get(0).orElseThrow());
            assertEquals(100, stats.getRates().get(8).orElseThrow());
            // No-op shuffle yields a shared board flush, so everyone receives a split pot.
            assertEquals(100, stats.getRates().get(9).orElseThrow());
        }
        assertEquals(6000, view(session).getPlayers().stream().mapToLong(p -> p.getChipStack()).sum());
    }

    @Test
    void preflopAndPostflopFoldersHaveDifferentFlopParticipation() {
        SetupSession session = session();
        session.start(6);
        act(session, ActionType.FOLD); // seat3 never sees flop.
        for (int i = 0; i < 4; i++) act(session, ActionType.CALL);
        act(session, ActionType.CHECK);
        for (int i = 0; i < 4; i++) act(session, ActionType.FOLD);
        session.settleHand();
        var pre = session.getStatistics().get(view(session).getPlayers().get(3).getId());
        var post = session.getStatistics().get(view(session).getPlayers().get(1).getId());
        assertTrue(pre.getRates().get(8).isEmpty());
        assertEquals(0, post.getRates().get(8).orElseThrow());
        assertEquals(1, post.getHandsPlayed());
        assertTrue(post.getRates().get(9).isEmpty());
    }

    private static SetupSession session() {
        Random dealer = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return 0; }
        };
        Random deck = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return bound - 1; }
        };
        return new SetupSession(new GameInitializer(new GameConfig(), dealer, deck),
                new BettingEngine(), new ShowdownResolver());
    }

    private static SetupView view(SetupSession session) { return session.getView().orElseThrow(); }
    private static void act(SetupSession session, ActionType type) {
        SetupView before = view(session);
        session.submitAction(new ActionRequest(before.getPlayers()
                .get(before.getCurrentActorSeat()).getId(), type));
    }
}
