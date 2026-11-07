package application;

import domain.model.AcceptedAction;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.PlayStats;
import domain.model.PokerHandEvaluator;
import domain.model.ResolvedActionType;
import domain.model.ShowdownResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Real initialization, engine, adapter and collectors; captures only observation delivery. */
class SessionStatisticsIntegrationTest {
    @Test
    void rejectsAMissingStatisticsFactory() {
        assertThrows(IllegalArgumentException.class, () -> new SetupSession(
                initializer(new GameConfig()), new BettingEngine(), new ShowdownResolver(), null));
    }

    @Test
    void blindsQueriesAndViewerChangesDoNotRecordActions() {
        RecordingStatistics statistics = new RecordingStatistics();
        SetupSession session = session(new GameConfig(), new ShowdownResolver(), statistics);
        session.start(6);
        session.getAvailableActions();
        session.getAvailableActions();
        session.selectViewer(view(session).getPlayers().get(1).getId());
        session.revealCards();
        session.hideCards();
        assertTrue(session.playBotTurns().isEmpty());
        assertEquals(6, statistics.current().collectors.size());
        assertTrue(statistics.current().events.isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertPending(statistics.current());
    }

    @Test
    void acceptedHumanActionIsDeliveredOnceWithoutAggregatingAPendingHand() {
        RecordingStatistics statistics = new RecordingStatistics();
        SetupSession session = session(new GameConfig(), new ShowdownResolver(), statistics);
        session.start(6);
        AcceptedAction action = act(session, ActionType.CALL);
        assertEquals(1, statistics.current().events.size());
        assertSame(action, statistics.current().events.get(0));
        assertEquals(20, action.getChipsAdded());
        assertEquals(view(session).getPlayers().get(3).getId(), action.getPlayerId());
        assertEquals(List.of(action), session.getAcceptedActions());
        assertEquals(50, view(session).getPot());
        assertPending(statistics.current());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "unknown", "outOfTurn", "checkOwing", "bet", "raise"})
    void rejectedRequestsDoNotReachTheCollectors(String invalid) {
        RecordingStatistics statistics = new RecordingStatistics();
        SetupSession session = session(new GameConfig(), new ShowdownResolver(), statistics);
        session.start(6);
        act(session, ActionType.CALL);
        SetupView before = view(session);
        session.revealCards();
        String actor = before.getPlayers().get(before.getCurrentActorSeat()).getId();
        ActionRequest request = invalidRequest(invalid, actor, before);
        Class<? extends RuntimeException> expected = invalid.equals("bet") || invalid.equals("raise")
                ? UnsupportedOperationException.class : IllegalArgumentException.class;
        assertThrows(expected, () -> session.submitAction(request));
        assertEquals(snapshot(before), snapshot(view(session)));
        assertTrue(view(session).getHoleCards().isEmpty());
        assertEquals(1, statistics.current().events.size());
        assertEquals(statistics.current().events, session.getAcceptedActions());
        assertPending(statistics.current());
    }

    @Test
    void allInRunoutRecordsOnlyTheFiveRealCallsAndRejectsFurtherActions() {
        RecordingStatistics statistics = new RecordingStatistics();
        SetupSession session = session(new GameConfig(20, 10, 20), new ShowdownResolver(), statistics);
        session.start(6);
        for (int i = 0; i < 5; i++) {
            assertTrue(act(session, ActionType.CALL).becameAllIn());
        }
        assertEquals(GamePhase.SHOWDOWN, view(session).getPhase());
        assertEquals(5, statistics.current().events.size());
        assertTrue(statistics.current().events.stream()
                .allMatch(action -> action.getType() == ResolvedActionType.CALL));
        assertEquals(statistics.current().events, session.getAcceptedActions());
        assertTrue(session.getResult().isPresent());
        String player = view(session).getPlayers().get(0).getId();
        assertThrows(IllegalStateException.class,
                () -> session.submitAction(new ActionRequest(player, ActionType.CALL)));
        assertTrue(session.playBotTurns().isEmpty());
        assertEquals(5, statistics.current().events.size());
        assertPending(statistics.current());
    }

    @Test
    void uncontestedResultRecordsEachFoldOnceAndDoesNotCompleteStatistics() {
        RecordingStatistics statistics = new RecordingStatistics();
        SetupSession session = session(new GameConfig(), new ShowdownResolver(), statistics);
        session.start(6);
        for (int i = 0; i < 5; i++) {
            act(session, ActionType.FOLD);
        }
        assertTrue(session.getResult().orElseThrow().isUncontested());
        assertEquals(5, statistics.current().events.size());
        assertEquals(statistics.current().events, session.getAcceptedActions());
        assertPending(statistics.current());
    }

    @Test
    void botsDeliverRealActionsOnceAndStopRecordingAtTheHuman() {
        RecordingStatistics statistics = new RecordingStatistics();
        SetupSession session = session(new GameConfig(), new ShowdownResolver(), statistics);
        session.start(1);
        List<AcceptedAction> played = session.playBotTurns();
        assertEquals(3, played.size());
        assertEquals(played, statistics.current().events);
        assertEquals(played, session.getAcceptedActions());
        assertTrue(session.playBotTurns().isEmpty());
        assertEquals(3, statistics.current().events.size());
        assertEquals(0, view(session).getCurrentActorSeat());
        assertPending(statistics.current());
    }

    @Test
    void comparisonFailureAndRetryDeliverTheFinalActionOnlyOnce() {
        boolean[] fail = {true};
        PokerHandEvaluator realEvaluator = new PokerHandEvaluator();
        ShowdownResolver resolver = new ShowdownResolver((first, second) -> {
            if (fail[0]) {
                fail[0] = false;
                throw new IllegalStateException("Comparison failure");
            }
            return realEvaluator.compare(first, second);
        });
        RecordingStatistics statistics = new RecordingStatistics();
        SetupSession session = session(new GameConfig(), resolver, statistics);
        session.start(6);
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL);
        act(session, ActionType.CHECK);
        for (int i = 0; i < 17; i++) act(session, ActionType.CHECK);
        SetupView before = view(session);
        assertEquals(23, statistics.current().events.size());
        assertThrows(IllegalStateException.class, () -> act(session, ActionType.CHECK));
        assertEquals(snapshot(before), snapshot(view(session)));
        assertEquals(23, statistics.current().events.size());
        assertTrue(session.getResult().isEmpty());
        AcceptedAction retried = act(session, ActionType.CHECK);
        assertEquals(24, statistics.current().events.size());
        assertSame(retried, statistics.current().events.get(23));
        assertEquals(statistics.current().events, session.getAcceptedActions());
        assertTrue(session.getResult().isPresent());
        assertPending(statistics.current());
    }

    @Test
    void collectorFailureLeavesTheDecisionAvailableForASingleRetry() {
        RecordingStatistics statistics = new RecordingStatistics();
        SetupSession session = session(new GameConfig(), new ShowdownResolver(), statistics);
        session.start(6);
        SetupView before = view(session);
        session.revealCards();
        statistics.failRecord = true;
        assertThrows(IllegalStateException.class, () -> act(session, ActionType.CALL));
        assertEquals(snapshot(before), snapshot(view(session)));
        assertTrue(view(session).getHoleCards().isEmpty());
        assertTrue(statistics.current().events.isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
        act(session, ActionType.CALL);
        assertEquals(1, statistics.current().events.size());
        assertEquals(50, view(session).getPot());
        assertPending(statistics.current());
    }

    @Test
    void failedRestartKeepsTheCollectorAndSuccessfulRestartCreatesAFreshOne() {
        RecordingStatistics statistics = new RecordingStatistics();
        SetupSession session = session(new GameConfig(), new ShowdownResolver(), statistics);
        session.start(6);
        act(session, ActionType.CALL);
        RecordedHand first = statistics.current();
        SetupView before = view(session);
        assertThrows(IllegalArgumentException.class, () -> session.start(7));
        assertSame(first, statistics.current());
        statistics.failStart = true;
        assertThrows(IllegalStateException.class, () -> session.start(6));
        assertEquals(snapshot(before), snapshot(view(session)));
        assertSame(first, statistics.current());
        assertEquals(1, first.events.size());
        assertEquals(1, session.getAcceptedActions().size());
        session.start(6);
        assertNotEquals(before.getHandId(), view(session).getHandId());
        assertNotSame(first, statistics.current());
        assertTrue(statistics.current().events.isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertEquals(1, first.events.size());
        assertPending(first);
        assertPending(statistics.current());
    }

    @Test
    void failedStatisticsStartDoesNotPublishAnInitialMatch() {
        RecordingStatistics statistics = new RecordingStatistics();
        statistics.failStart = true;
        SetupSession session = session(new GameConfig(), new ShowdownResolver(), statistics);
        assertThrows(IllegalStateException.class, () -> session.start(2));
        assertTrue(session.getView().isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertTrue(statistics.hands.isEmpty());
        session.start(2);
        assertTrue(session.getView().isPresent());
        assertPending(statistics.current());
    }

    private static ActionRequest invalidRequest(String kind, String actor, SetupView before) {
        switch (kind) {
            case "null": return null;
            case "unknown": return new ActionRequest("unknown", ActionType.CALL);
            case "outOfTurn": return new ActionRequest(before.getPlayers().get(0).getId(), ActionType.CALL);
            case "checkOwing": return new ActionRequest(actor, ActionType.CHECK);
            case "bet": return new ActionRequest(actor, ActionType.BET, 40);
            default: return new ActionRequest(actor, ActionType.RAISE, 40);
        }
    }

    private static void assertPending(RecordedHand hand) {
        for (PlayStats stats : hand.collectors.values()) {
            assertEquals(0, stats.getHandsPlayed());
            assertTrue(stats.getVpipPercent().isEmpty());
            assertTrue(stats.getWonAtShowdownPercent().isEmpty());
        }
    }

    private static List<Object> snapshot(SetupView view) {
        return List.of(view.getHandId(), view.getPlayers(), view.getStatuses(),
                view.getPot(), view.getCurrentActorSeat(), view.getPhase(),
                view.getCommunityCards(), view.getRemainingCardCount(), view.getCurrentBet());
    }

    private static AcceptedAction act(SetupSession session, ActionType type) {
        SetupView view = view(session);
        return session.submitAction(new ActionRequest(
                view.getPlayers().get(view.getCurrentActorSeat()).getId(), type));
    }

    private static SetupView view(SetupSession session) { return session.getView().orElseThrow(); }

    private static SetupSession session(GameConfig config, ShowdownResolver resolver,
            RecordingStatistics statistics) {
        return new SetupSession(initializer(config), new BettingEngine(), resolver, statistics::start);
    }

    private static GameInitializer initializer(GameConfig config) {
        return new GameInitializer(config, new FirstSeatRandom(), new LastChoiceRandom());
    }

    private static final class RecordedHand {
        private final Map<String, PlayStats> collectors = new LinkedHashMap<>();
        private final List<AcceptedAction> events = new ArrayList<>();
    }

    private static final class RecordingStatistics {
        private final List<RecordedHand> hands = new ArrayList<>();
        private boolean failRecord;
        private boolean failStart;

        private Consumer<AcceptedAction> start(GameState state) {
            if (failStart) {
                failStart = false;
                throw new IllegalStateException("Collector initialization failure");
            }
            RecordedHand hand = new RecordedHand();
            for (var player : state.getPlayers()) {
                PlayStats stats = new PlayStats();
                stats.beginHand(state.getHandId(), true);
                hand.collectors.put(player.getId(), stats);
            }
            HandStatistics adapter = new HandStatistics(state.getHandId(), hand.collectors);
            hands.add(hand);
            return action -> {
                if (failRecord) {
                    failRecord = false;
                    throw new IllegalStateException("Collector failure before recording");
                }
                adapter.record(action);
                hand.events.add(action);
            };
        }

        private RecordedHand current() { return hands.get(hands.size() - 1); }
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
