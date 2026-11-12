package application;

import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.GameConfig;
import domain.model.PlayerStatus;
import domain.model.ResolvedActionType;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuitIntegrationTest {
    @Test
    void departingPlayersFoldInTurnAndKeepTheirStacksWithoutBeingDealtAgain() {
        SetupSession session = start(6, new GameConfig());
        String queued = id(session, 4);
        session.selectViewer(queued);
        session.revealCards();
        session.quit(queued);
        assertEquals(3, view(session).getCurrentActorSeat());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertTrue(view(session).getHoleCards().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> session.selectViewer(queued));

        act(session, ActionType.CALL);
        assertThrows(IllegalArgumentException.class, () -> act(session, ActionType.CALL));
        assertEquals(1, session.getAcceptedActions().size());
        assertTrue(session.getLegalActions().getActions().isEmpty());
        assertEquals(ResolvedActionType.FOLD, session.playBotTurns().get(0).getType());
        assertEquals(queued, session.getAcceptedActions().get(1).getPlayerId());
        assertEquals(5, view(session).getCurrentActorSeat());
        session.quit(id(session, 5));
        assertEquals(0, view(session).getCurrentActorSeat());
        assertEquals(0, view(session).getViewerSeat());
        finishWithFolds(session);
        assertEquals(1, session.getStatistics().get(queued).getHandsPlayed());

        session.nextHand();
        assertEquals(44, view(session).getRemainingCardCount());
        assertEquals(queued, id(session, 4));
        assertEquals(PlayerStatus.DEPARTED, view(session).getStatuses().get(4));
        assertEquals(PlayerStatus.DEPARTED, view(session).getStatuses().get(5));
        assertEquals(1000, view(session).getPlayers().get(4).getChipStack());
        assertEquals(6000, view(session).getPot() + view(session).getPlayers().stream()
                .mapToLong(player -> player.getChipStack()).sum());
        finishWithFolds(session);
        assertEquals(PlayerStatus.DEPARTED, view(session).getStatuses().get(4));
        assertEquals(1, session.getStatistics().get(queued).getHandsPlayed());
    }

    @Test
    void anAllInDepartureKeepsTheCurrentHandAndItsPayout() {
        SetupSession session = start(6, new GameConfig(20, 10, 20));
        String allIn = id(session, 2);
        session.quit(allIn);
        assertEquals(PlayerStatus.ALL_IN, view(session).getStatuses().get(2));
        assertTrue(session.getAcceptedActions().isEmpty());
        finishWithFolds(session);
        assertTrue(session.getResult().orElseThrow().getWinnerIds().contains(allIn));
        assertEquals(30, view(session).getPlayers().get(2).getChipStack());
        session.nextHand();
        assertEquals(PlayerStatus.DEPARTED, view(session).getStatuses().get(2));
        assertEquals(30, view(session).getPlayers().get(2).getChipStack());
    }

    @Test
    void paidMatchStopsWhenFewerThanTwoFundedPlayersRemainAndRestartClearsDepartures() {
        SetupSession session = start(6, new GameConfig());
        finishWithFolds(session);
        for (int seat = 1; seat < 6; seat++) session.quit(id(session, seat));
        assertEquals(id(session, 0), session.getMatchWinner().orElseThrow().getId());
        assertFalse(session.canStartNextHand());
        assertThrows(IllegalStateException.class, session::nextHand);
        String lastId = id(session, 0);
        session.quit(lastId);
        assertTrue(session.getMatchWinner().isEmpty());
        assertFalse(session.canStartNextHand());
        assertThrows(IllegalStateException.class, session::nextHand);
        assertThrows(IllegalStateException.class, session::revealCards);
        assertTrue(view(session).getHoleCards().isEmpty());
        session.start(6);
        assertFalse(session.hasDeparted(lastId));
        assertEquals(40, view(session).getRemainingCardCount());
    }

    @Test
    void invalidDeparturePreservesTheTurnAndAcceptedActions() {
        SetupSession session = start(1, new GameConfig());
        assertThrows(IllegalArgumentException.class, () -> session.quit("unknown"));
        assertThrows(IllegalArgumentException.class, () -> session.quit(id(session, 3)));
        assertEquals(3, view(session).getCurrentActorSeat());
        session.quit(id(session, 0));
        assertThrows(IllegalArgumentException.class, () -> session.quit(id(session, 0)));
        assertTrue(session.getAcceptedActions().isEmpty());
    }

    @Test
    void leavingConcealsAnotherViewersCardsWithoutChangingTheirTurn() {
        SetupSession session = start(6, new GameConfig());
        session.selectViewer(id(session, 3));
        session.revealCards();
        assertEquals(2, view(session).getHoleCards().size());
        session.quit(id(session, 4));
        assertEquals(3, view(session).getCurrentActorSeat());
        assertEquals(3, view(session).getViewerSeat());
        assertTrue(view(session).getHoleCards().isEmpty());
    }

    @Test
    void aDepartedViewerHandsOffToARemainingHumanDuringABotTurn() {
        SetupSession session = start(2, new GameConfig());
        session.revealCards();
        session.quit(id(session, 0));
        assertEquals(3, view(session).getCurrentActorSeat());
        assertEquals(1, view(session).getViewerSeat());
        assertTrue(view(session).getHoleCards().isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
    }

    private static void finishWithFolds(SetupSession session) {
        for (int i = 0; i < 6 && session.getResult().isEmpty(); i++) act(session, ActionType.FOLD);
        assertTrue(session.getResult().isPresent());
        session.settleHand();
    }

    private static SetupSession start(int humans, GameConfig config) {
        Random dealer = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return 0; }
        };
        SetupSession session = new SetupSession(new GameInitializer(config, dealer, new Random(1)));
        session.start(humans);
        return session;
    }

    private static void act(SetupSession session, ActionType type) {
        session.submitAction(new ActionRequest(id(session, view(session).getCurrentActorSeat()), type));
    }

    private static String id(SetupSession session, int seat) {
        return view(session).getPlayers().get(seat).getId();
    }

    private static SetupView view(SetupSession session) { return session.getView().orElseThrow(); }
}
