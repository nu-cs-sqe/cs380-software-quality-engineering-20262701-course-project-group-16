package application;

import domain.model.AcceptedAction;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.PlayerType;
import domain.model.PokerHandEvaluator;
import domain.model.ResolvedActionType;
import domain.model.ShowdownResolver;
import domain.model.ShowdownResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SessionBettingTest {
    @ParameterizedTest(name = "SS-B01-{0}: null collaborator")
    @ValueSource(ints = {0, 1, 2})
    void rejectsNullCollaborators(int missing) {
        assertThrows(IllegalArgumentException.class, () -> new SetupSession(
                missing == 0 ? null : initializer(new GameConfig()),
                missing == 1 ? null : new BettingEngine(), missing == 2 ? null : new ShowdownResolver()));
    }

    @Test
    void emptySessionHasNoActionsHistoryOrResult() {
        SetupSession session = session(new GameConfig());
        assertTrue(session.getAvailableActions().isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertTrue(session.getResult().isEmpty());
        assertThrows(IllegalStateException.class, session::playBotTurns);
    }

    @ParameterizedTest(name = "SS-B03-{0}: action before starting")
    @EnumSource(value = ActionType.class, names = {"FOLD", "CHECK", "CALL"})
    void cannotActWithoutAMatch(ActionType type) {
        assertThrows(IllegalStateException.class,
                () -> session(new GameConfig()).submitAction(new ActionRequest("missing", type)));
    }

    @Test
    void callUpdatesOneStateAndAppendsExactlyOneObservation() {
        SetupSession session = started(new GameConfig(), 6);
        SetupView before = view(session);
        session.revealCards();
        AcceptedAction accepted = act(session, ActionType.CALL);
        SetupView after = view(session);
        assertEquals(ResolvedActionType.CALL, accepted.getType());
        assertEquals(20, accepted.getChipsAdded());
        assertEquals(before.getHandId(), accepted.getHandId());
        assertEquals(980, after.getPlayers().get(3).getChipStack());
        assertEquals(20, after.getRoundContribution(3));
        assertEquals(20, after.getHandContribution(3));
        assertEquals(50, after.getPot());
        assertEquals(4, after.getCurrentActorSeat());
        assertEquals(4, after.getViewerSeat());
        assertTrue(after.getHoleCards().isEmpty());
        assertEquals(List.of(accepted), session.getAcceptedActions());
        assertEquals(30, before.getPot());
        assertEquals(1000, before.getPlayers().get(3).getChipStack());
    }

    @ParameterizedTest(name = "SS-B05-{0}: rejected action preserves the hand")
    @ValueSource(strings = {"null", "unknown", "outOfTurn", "checkOwing", "bet", "raise"})
    void rejectionDoesNotChangeStateOrHistory(String kind) {
        SetupSession session = started(new GameConfig(), 6);
        SetupView before = view(session);
        String actor = before.getPlayers().get(3).getId();
        ActionRequest request = kind.equals("null") ? null
                : kind.equals("unknown") ? new ActionRequest("missing", ActionType.CALL)
                : kind.equals("outOfTurn") ? new ActionRequest(before.getPlayers().get(4).getId(), ActionType.CALL)
                : kind.equals("bet") ? new ActionRequest(actor, ActionType.BET, 40)
                : kind.equals("raise") ? new ActionRequest(actor, ActionType.RAISE, 39)
                : new ActionRequest(actor, ActionType.CHECK);
        session.revealCards();
        Class<? extends RuntimeException> failure = IllegalArgumentException.class;
        assertThrows(failure, () -> session.submitAction(request));
        assertEquals(snapshot(before), snapshot(view(session)));
        assertTrue(view(session).getHoleCards().isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertTrue(session.getResult().isEmpty());
    }

    @Test
    void availableActionsUseEngineValidationWithoutCommittingActions() {
        SetupSession session = started(new GameConfig(), 6);
        SetupView before = view(session);
        assertEquals(Set.of(ActionType.FOLD, ActionType.CALL), session.getAvailableActions());
        assertThrows(UnsupportedOperationException.class, () -> session.getAvailableActions().clear());
        assertEquals(snapshot(before), snapshot(view(session)));
        assertTrue(session.getAcceptedActions().isEmpty());
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL);
        assertEquals(2, view(session).getCurrentActorSeat());
        assertEquals(Set.of(ActionType.FOLD, ActionType.CHECK), session.getAvailableActions());
        assertEquals(5, session.getAcceptedActions().size());
    }

    @Test
    void lastPreflopDecisionAdvancesToFlopAndPreservesCommitments() {
        SetupSession session = started(new GameConfig(), 6);
        assertEquals(20, view(session).getCurrentBet());
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL);
        assertEquals(GamePhase.PREFLOP, view(session).getPhase());
        assertEquals(2, view(session).getCurrentActorSeat());
        assertTrue(view(session).getCommunityCards().isEmpty());
        act(session, ActionType.CHECK);
        SetupView flop = view(session);
        assertEquals(GamePhase.FLOP, flop.getPhase());
        assertEquals(3, flop.getCommunityCards().size());
        assertEquals(36, flop.getRemainingCardCount());
        assertEquals(1, flop.getCurrentActorSeat());
        assertEquals(0, flop.getCurrentBet());
        assertEquals(120, flop.getPot());
        for (int seat = 0; seat < 6; seat++) {
            assertEquals(0, flop.getRoundContribution(seat));
            assertEquals(20, flop.getHandContribution(seat));
        }
        assertEquals(Set.of(ActionType.FOLD, ActionType.CHECK), session.getAvailableActions());
        assertTrue(session.getResult().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> flop.getCommunityCards().clear());
        assertThrows(UnsupportedOperationException.class, () -> flop.getStatuses().clear());
    }

    @Test
    void lastFoldResolvesUncontestedWithoutDrawingOrPaying() {
        SetupSession session = started(new GameConfig(), 6);
        for (int i = 0; i < 5; i++) act(session, ActionType.FOLD);
        SetupView after = view(session);
        ShowdownResult result = session.getResult().orElseThrow();
        assertTrue(result.isUncontested());
        assertEquals(List.of(after.getPlayers().get(2).getId()), result.getWinnerIds());
        assertEquals(ShowdownResult.Status.AWAITING_SETTLEMENT, result.getStatus());
        assertEquals(-1, after.getCurrentActorSeat());
        assertEquals(GamePhase.PREFLOP, after.getPhase());
        assertTrue(after.getCommunityCards().isEmpty());
        assertEquals(40, after.getRemainingCardCount());
        assertEquals(30, after.getPot());
        assertEquals(980, after.getPlayers().get(2).getChipStack());
        assertTrue(session.getAvailableActions().isEmpty());
        assertTrue(session.playBotTurns().isEmpty());
    }

    @Test
    void allInCallsRunTheBoardWithoutInventingCheckActions() {
        SetupSession session = started(new GameConfig(20, 10, 20), 6);
        for (int i = 0; i < 5; i++) assertTrue(act(session, ActionType.CALL).becameAllIn());
        SetupView after = view(session);
        assertEquals(GamePhase.SHOWDOWN, after.getPhase());
        assertEquals(5, after.getCommunityCards().size());
        assertEquals(32, after.getRemainingCardCount());
        assertEquals(120, after.getPot());
        assertEquals(0, after.getPlayers().stream().mapToLong(player -> player.getChipStack()).sum());
        assertEquals(5, session.getAcceptedActions().size());
        assertTrue(session.getAcceptedActions().stream().allMatch(action -> action.getType() == ResolvedActionType.CALL));
        assertEquals(ShowdownResult.Status.AWAITING_SETTLEMENT, session.getResult().orElseThrow().getStatus());
    }

    @Test
    void loneActivePlayerStillMustRespondToAnAllInOpponent() {
        SetupSession session = started(new GameConfig(20, 10, 20), 6);
        for (int i = 0; i < 4; i++) act(session, ActionType.FOLD);
        assertEquals(1, view(session).getCurrentActorSeat());
        assertTrue(session.getResult().isEmpty());
        assertEquals(Set.of(ActionType.CALL, ActionType.FOLD), session.getAvailableActions());
        act(session, ActionType.CALL);
        assertEquals(GamePhase.SHOWDOWN, view(session).getPhase());
        assertFalse(session.getResult().orElseThrow().isUncontested());
    }

    @ParameterizedTest(name = "SS-B11-{0}: decisions after result")
    @EnumSource(value = ActionType.class, names = {"FOLD", "CHECK", "CALL"})
    void completedComparisonRejectsMoreActions(ActionType type) {
        SetupSession session = started(new GameConfig(), 6);
        String initialActor = view(session).getPlayers().get(3).getId();
        for (int i = 0; i < 5; i++) act(session, ActionType.FOLD);
        ShowdownResult result = session.getResult().orElseThrow();
        List<AcceptedAction> history = session.getAcceptedActions();
        assertThrows(IllegalStateException.class, () -> session.submitAction(new ActionRequest(initialActor, type)));
        assertSame(result, session.getResult().orElseThrow());
        assertEquals(history, session.getAcceptedActions());
        assertThrows(UnsupportedOperationException.class, history::clear);
    }

    @ParameterizedTest(name = "SS-B12-{0}: bot/human boundary")
    @ValueSource(ints = {1, 2, 6})
    void botsStopAtTheNextHumanAndReturnOnlyCommittedActions(int humans) {
        SetupSession session = started(new GameConfig(), humans);
        List<AcceptedAction> played = session.playBotTurns();
        int actor = view(session).getCurrentActorSeat();
        assertEquals(humans == 6 ? 3 : 0, actor);
        assertEquals(actor, view(session).getViewerSeat());
        assertEquals(PlayerType.HUMAN, view(session).getPlayers().get(actor).getType());
        assertEquals(humans == 6 ? 0 : 3, played.size());
        assertEquals(played, session.getAcceptedActions());
        assertTrue(played.stream().allMatch(action -> action.getType() == ResolvedActionType.CALL));
        assertTrue(session.playBotTurns().isEmpty());
        assertEquals(played, session.getAcceptedActions());
        assertThrows(UnsupportedOperationException.class, played::clear);
    }

    @Test
    void botsFinishWhenTheOnlyHumanFolds() {
        SetupSession session = started(new GameConfig(), 1);
        session.playBotTurns();
        act(session, ActionType.FOLD);
        List<AcceptedAction> botActions = session.playBotTurns();
        assertEquals(GamePhase.SHOWDOWN, view(session).getPhase());
        assertTrue(session.getResult().isPresent());
        assertTrue(botActions.stream().anyMatch(action -> action.getType() == ResolvedActionType.CHECK));
        assertEquals(100, view(session).getPot());
        assertEquals(6000, total(view(session)));
    }

    @Test
    void failedRestartPreservesResultAndHistoryButSuccessfulRestartClearsThem() {
        SetupSession session = started(new GameConfig(), 6);
        for (int i = 0; i < 5; i++) act(session, ActionType.FOLD);
        ShowdownResult result = session.getResult().orElseThrow();
        List<AcceptedAction> history = session.getAcceptedActions();
        session.revealCards();
        assertThrows(IllegalArgumentException.class, () -> session.start(0));
        assertSame(result, session.getResult().orElseThrow());
        assertEquals(history, session.getAcceptedActions());
        assertTrue(view(session).getHoleCards().isEmpty());
        session.start(2);
        assertTrue(session.getResult().isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertNotEquals(result.getHandId(), view(session).getHandId());
        assertEquals(5, history.size());
    }

    @Test
    void failedComparisonPreservesTheFinalDecisionAndRetryCommitsOnlyOnce() {
        boolean[] failNextComparison = {true};
        PokerHandEvaluator evaluator = new PokerHandEvaluator();
        ShowdownResolver resolver = new ShowdownResolver((first, second) -> {
            if (failNextComparison[0]) {
                failNextComparison[0] = false;
                throw new IllegalStateException("Injected comparison failure");
            }
            return evaluator.compare(first, second);
        });
        SetupSession session = new SetupSession(initializer(new GameConfig()), new BettingEngine(), resolver);
        session.start(6);
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL);
        act(session, ActionType.CHECK);
        for (int i = 0; i < 17; i++) act(session, ActionType.CHECK);
        SetupView before = view(session);
        List<AcceptedAction> history = session.getAcceptedActions();
        session.revealCards();
        assertThrows(IllegalStateException.class, () -> act(session, ActionType.CHECK));
        assertEquals(snapshot(before), snapshot(view(session)));
        assertEquals(history, session.getAcceptedActions());
        assertTrue(session.getResult().isEmpty());
        assertTrue(view(session).getHoleCards().isEmpty());
        AcceptedAction retried = act(session, ActionType.CHECK);
        assertEquals(GamePhase.SHOWDOWN, view(session).getPhase());
        assertTrue(session.getResult().isPresent());
        assertEquals(24, session.getAcceptedActions().size());
        assertSame(retried, session.getAcceptedActions().get(23));
        assertEquals(23, history.size());
    }

    @Test
    void zeroCostCallCannotCloseTheBigBlindsOption() {
        SetupSession session = started(new GameConfig(), 6);
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL);
        SetupView before = view(session);
        List<AcceptedAction> history = session.getAcceptedActions();
        assertThrows(IllegalArgumentException.class, () -> act(session, ActionType.CALL));
        assertEquals(snapshot(before), snapshot(view(session)));
        assertEquals(history, session.getAcceptedActions());
        assertTrue(session.getResult().isEmpty());
        act(session, ActionType.CHECK);
        assertEquals(GamePhase.FLOP, view(session).getPhase());
    }

    @Test
    void botPumpHidesARevealedHandWhenSelectingAHumanWithoutPlayingAnAction() {
        SetupSession session = started(new GameConfig(), 6);
        session.revealCards();
        SetupView old = view(session);
        assertEquals(2, old.getHoleCards().size());
        assertTrue(session.playBotTurns().isEmpty());
        assertEquals(3, view(session).getViewerSeat());
        assertTrue(view(session).getHoleCards().isEmpty());
        assertEquals(2, old.getHoleCards().size());
        assertTrue(session.getAcceptedActions().isEmpty());
    }

    private static AcceptedAction act(SetupSession session, ActionType type) {
        SetupView view = view(session);
        return session.submitAction(new ActionRequest(view.getPlayers().get(view.getCurrentActorSeat()).getId(), type));
    }

    private static SetupView view(SetupSession session) { return session.getView().orElseThrow(); }

    private static long total(SetupView view) {
        return view.getPot() + view.getPlayers().stream().mapToLong(player -> player.getChipStack()).sum();
    }

    private static List<Object> snapshot(SetupView view) {
        List<Object> data = new ArrayList<>(List.of(view.getPlayers(), view.getPot(), view.getCurrentActorSeat(),
                view.getHandId(), view.getPhase(), view.getCurrentBet(), view.getCommunityCards(),
                view.getRemainingCardCount(), view.getStatuses(), view.getViewerSeat()));
        for (int seat = 0; seat < 6; seat++) data.add(List.of(view.getRoundContribution(seat), view.getHandContribution(seat)));
        return data;
    }

    private static SetupSession started(GameConfig config, int humans) {
        SetupSession session = session(config);
        session.start(humans);
        return session;
    }

    private static SetupSession session(GameConfig config) { return new SetupSession(initializer(config)); }

    private static GameInitializer initializer(GameConfig config) {
        return new GameInitializer(config, new FirstSeatRandom(), new LastChoiceRandom());
    }

    private static final class FirstSeatRandom extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return 0; }
    }

    private static final class LastChoiceRandom extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return bound - 1; }
    }
}
