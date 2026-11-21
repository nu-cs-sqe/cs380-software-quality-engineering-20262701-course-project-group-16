package application;

import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.GameConfig;
import domain.model.PlayerStatus;
import domain.model.ResolvedActionType;
import domain.model.ShowdownResolver;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuitIntegrationTest {
    @Test
    void departingPlayersFoldImmediatelyAndKeepTheirStacksWithoutBeingDealtAgain() {
        SetupSession session = start(6, new GameConfig());
        String queued = id(session, 4);
        session.selectViewer(queued);
        session.revealCards();
        session.quit(queued);
        assertEquals(3, view(session).getCurrentActorSeat());
        assertEquals(1, session.getAcceptedActions().size());
        assertEquals(queued, session.getAcceptedActions().get(0).getPlayerId());
        assertEquals(ResolvedActionType.FOLD, session.getAcceptedActions().get(0).getType());
        assertTrue(view(session).getHoleCards().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> session.selectViewer(queued));

        act(session, ActionType.CALL);
        assertThrows(IllegalArgumentException.class, () -> session.submitAction(
                new ActionRequest(queued, ActionType.CALL)));
        assertEquals(2, session.getAcceptedActions().size());
        assertTrue(session.getLegalActions().getActions().contains(ActionType.CALL));
        assertTrue(session.playBotTurns().isEmpty());
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
    void twoDepartingHighContributorsCannotBlockPaymentOrTheNextHand() {
        SetupSession session = start(6, new GameConfig());
        String raiser = id(session, 3);
        String caller = id(session, 4);
        session.submitAction(new ActionRequest(raiser, ActionType.RAISE, 40));
        session.submitAction(new ActionRequest(caller, ActionType.CALL));
        session.quit(raiser);
        session.quit(caller);
        while (session.getResult().isEmpty()) act(session, ActionType.FOLD);
        assertEquals(110, view(session).getPot());
        assertEquals(List.of(id(session, 2)),
                session.getResult().orElseThrow().getWinnerIds());
        assertEquals(110, session.settleHand().getPayouts().get(id(session, 2)));
        assertEquals(0, view(session).getPot());
        assertEquals(1090, view(session).getPlayers().get(2).getChipStack());
        assertEquals(1, session.getStatistics().get(raiser).getHandsPlayed());
        session.nextHand();
        assertEquals(44, view(session).getRemainingCardCount());
    }

    @Test
    void aPlayerWhoHasNotInvestedCanWinAfterAllContributorsLeave() {
        SetupSession session = start(6, new GameConfig());
        session.submitAction(new ActionRequest(id(session, 3), ActionType.RAISE, 40));
        session.submitAction(new ActionRequest(id(session, 4), ActionType.CALL));
        for (int seat : new int[] {3, 4, 0, 1, 2}) session.quit(id(session, seat));
        assertEquals(List.of(id(session, 5)),
                session.getResult().orElseThrow().getWinnerIds());
        assertEquals(110, session.settleHand().getPayouts().get(id(session, 5)));
        assertEquals(1110, view(session).getPlayers().get(5).getChipStack());
        assertEquals(0, view(session).getPot());
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
    void paidMatchEndsWithoutAWinnerWhenDepartedSeatsRetainChips() {
        SetupSession session = start(6, new GameConfig());
        finishWithFolds(session);
        for (int seat = 1; seat < 6; seat++) session.quit(id(session, seat));
        assertTrue(session.getMatchWinner().isEmpty());
        assertEquals(1000, view(session).getPlayers().get(0).getChipStack());
        assertEquals(6000, view(session).getPlayers().stream()
                .mapToLong(player -> player.getChipStack()).sum());
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
        assertEquals(1, session.getAcceptedActions().size());
        assertEquals(id(session, 0), session.getAcceptedActions().get(0).getPlayerId());
        assertEquals(ResolvedActionType.FOLD, session.getAcceptedActions().get(0).getType());
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
        assertEquals(1, session.getAcceptedActions().size());
        assertEquals(id(session, 0), session.getAcceptedActions().get(0).getPlayerId());
    }

    @Test
    void dep08NonCurrentQuitFoldsImmediatelyAndPreservesTheCurrentDecision() {
        SetupSession session = start(6, new GameConfig());
        String quitter = id(session, 4);
        String actor = id(session, 3);
        session.quit(quitter);
        assertEquals(PlayerStatus.FOLDED, view(session).getStatuses().get(4));
        assertEquals(3, view(session).getCurrentActorSeat());
        assertEquals(30, view(session).getPot());
        assertEquals(1000, view(session).getPlayers().get(4).getChipStack());
        assertEquals(1, session.getAcceptedActions().size());
        assertEquals(quitter, session.getAcceptedActions().get(0).getPlayerId());
        assertEquals(ResolvedActionType.FOLD, session.getAcceptedActions().get(0).getType());
        act(session, ActionType.CALL);
        assertEquals(actor, session.getAcceptedActions().get(1).getPlayerId());
        assertEquals(5, view(session).getCurrentActorSeat());
        assertTrue(session.playDepartureTurns().isEmpty());
    }

    @Test
    void dep09AQuitterWhoAlreadyActedCannotBecomeTheUncontestedWinner() {
        SetupSession session = start(6, new GameConfig());
        for (int index = 0; index < 3; index++) {
            act(session, ActionType.FOLD);
        }
        act(session, ActionType.CALL);
        act(session, ActionType.FOLD);
        assertEquals(2, view(session).getCurrentActorSeat());
        String quitter = id(session, 0);
        String winner = id(session, 2);
        session.quit(quitter);
        assertTrue(session.getResult().isPresent());
        assertEquals(List.of(winner), session.getResult().orElseThrow().getWinnerIds());
        assertTrue(session.getResult().orElseThrow().isUncontested());
        assertEquals(ResolvedActionType.FOLD, session.getAcceptedActions().get(5).getType());
        assertEquals(quitter, session.getAcceptedActions().get(5).getPlayerId());
        session.settleHand();
        assertEquals(980, view(session).getPlayers().get(0).getChipStack());
        assertEquals(1030, view(session).getPlayers().get(2).getChipStack());
        assertEquals(6000, view(session).getPlayers().stream()
                .mapToLong(player -> player.getChipStack()).sum());
        assertEquals(1, session.getStatistics().get(quitter).getHandsPlayed());
    }

    @Test
    void dep10FailedDepartureRecordingPreservesEligibilityAndSupportsRetry() {
        AtomicBoolean reject = new AtomicBoolean(true);
        SetupSession session = new SetupSession(BotFixtures.initializer(), new BettingEngine(),
                new ShowdownResolver(), state -> action -> {
                    if (reject.get()) {
                        throw new IllegalStateException("Recorder rejected departure.");
                    }
                });
        session.start(1);
        String quitter = id(session, 0);
        String observer = id(session, 3);
        BotTurn turn = session.getBotTurn(1).orElseThrow();
        session.revealCards();
        assertThrows(IllegalStateException.class, () -> session.quit(quitter));
        assertFalse(session.hasDeparted(quitter));
        assertEquals(PlayerStatus.ACTIVE, view(session).getStatuses().get(0));
        assertTrue(session.getAcceptedActions().isEmpty());
        assertTrue(session.isCurrentBotTurn(turn));
        assertTrue(view(session).getHoleCards().isEmpty());
        reject.set(false);
        session.quit(quitter);
        assertTrue(session.hasDeparted(quitter));
        assertEquals(PlayerStatus.FOLDED, view(session).getStatuses().get(0));
        assertFalse(session.isCurrentBotTurn(turn));
        assertEquals(1, session.getAcceptedActions().size());
        assertEquals(0, session.getBotBeliefs(observer).get(quitter).getObservedDecisions());
        BotTurn fresh = session.getBotTurn(1).orElseThrow();
        assertFalse(fresh.getScenario().getOpponents().contains(quitter));
        assertEquals(turn.getView().getViewerId(), fresh.getView().getViewerId());
    }

    @Test
    void dep11AnAlreadyFoldedQuitterCreatesNoDuplicateFold() {
        SetupSession session = start(6, new GameConfig());
        String quitter = id(session, 3);
        act(session, ActionType.FOLD);
        session.quit(quitter);
        assertTrue(session.hasDeparted(quitter));
        assertEquals(1, session.getAcceptedActions().size());
        assertEquals(4, view(session).getCurrentActorSeat());
        assertEquals(PlayerStatus.FOLDED, view(session).getStatuses().get(3));
        assertEquals(1000, view(session).getPlayers().get(3).getChipStack());
    }

    @Test
    void dep12LeavingAfterAResolvedHandPreservesItsWinnerAndPendingPayment() {
        SetupSession session = start(6, new GameConfig());
        for (int index = 0; index < 5; index++) {
            act(session, ActionType.FOLD);
        }
        String winner = id(session, 2);
        assertTrue(session.getResult().isPresent());
        session.quit(winner);
        assertEquals(5, session.getAcceptedActions().size());
        assertEquals(List.of(winner), session.getResult().orElseThrow().getWinnerIds());
        assertTrue(session.getSettlement().isEmpty());
        session.settleHand();
        assertEquals(1010, view(session).getPlayers().get(2).getChipStack());
        assertEquals(1, session.getStatistics().get(winner).getHandsPlayed());
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
