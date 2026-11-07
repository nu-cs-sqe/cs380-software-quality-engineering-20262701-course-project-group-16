package application;

import domain.model.AcceptedAction;
import domain.model.BettingStreet;
import domain.model.GameSetup;
import domain.model.GameState;
import domain.model.PlayStats;
import domain.model.PreflopRaiseType;
import domain.model.ResolvedActionType;
import domain.model.StatsContext;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class HandStatisticsTest {
    private static final String HAND = "hand-1";

    @Test
    void routesAcceptedCallsToTheOwningPlayerWithoutCompletingTheHand() {
        PlayStats first = pending();
        PlayStats second = pending();
        HandStatistics statistics = new HandStatistics(HAND, Map.of("p1", first, "p2", second));
        statistics.record(action(HAND, "p1", BettingStreet.PREFLOP, ResolvedActionType.CALL, 20));
        assertEquals(0, first.getHandsPlayed());
        assertTrue(first.getVpipPercent().isEmpty());

        // Model-only completion facts expose the recorded observation; the session never does this.
        first.completeHand(HAND, false, false, 0, 0);
        second.completeHand(HAND, false, false, 0, 0);
        assertEquals(100, first.getVpipPercent().orElseThrow());
        assertEquals(0, first.getPfrPercent().orElseThrow());
        assertEquals(0, second.getVpipPercent().orElseThrow());
    }

    @Test
    void preservesTheEnginesThreeBetContext() {
        PlayStats stats = pending();
        HandStatistics statistics = new HandStatistics(HAND, Map.of("p1", stats));
        statistics.record(new AcceptedAction(HAND, "p1", BettingStreet.PREFLOP,
                ResolvedActionType.RAISE, 60, 60, false,
                new StatsContext(PreflopRaiseType.THREE_BET, true, false, false)));
        stats.completeHand(HAND, false, false, 0, 0);
        assertEquals(100, stats.getVpipPercent().orElseThrow());
        assertEquals(100, stats.getPfrPercent().orElseThrow());
        assertEquals(100, stats.getThreeBetPercent().orElseThrow());
    }

    @Test
    void preservesPostflopActionKindsForAggregation() {
        PlayStats stats = pending();
        HandStatistics statistics = new HandStatistics(HAND, Map.of("p1", stats));
        statistics.record(action(HAND, "p1", BettingStreet.FLOP, ResolvedActionType.BET, 10));
        statistics.record(action(HAND, "p1", BettingStreet.FLOP, ResolvedActionType.CALL, 10));
        statistics.record(action(HAND, "p1", BettingStreet.TURN, ResolvedActionType.RAISE, 20));
        statistics.record(action(HAND, "p1", BettingStreet.RIVER, ResolvedActionType.FOLD, 0));
        stats.completeHand(HAND, true, false, 0, 0);
        assertEquals(2, stats.getAggressionFactor().orElseThrow());
        assertEquals(50, stats.getAggressionFrequencyPercent().orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(value = ResolvedActionType.class, names = {"CHECK", "FOLD", "FORCED_POST"})
    void nonVoluntaryActionsDoNotCreateVpipOrPfr(ResolvedActionType type) {
        PlayStats stats = pending();
        HandStatistics statistics = new HandStatistics(HAND, Map.of("p1", stats));
        statistics.record(action(HAND, "p1", BettingStreet.PREFLOP, type,
                type == ResolvedActionType.FORCED_POST ? 10 : 0));
        stats.completeHand(HAND, false, false, 0, 0);
        assertEquals(1, stats.getHandsPlayed());
        assertEquals(0, stats.getVpipPercent().orElseThrow());
        assertEquals(0, stats.getPfrPercent().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"hand", "player"})
    void rejectsForeignObservationsWithoutChangingStatistics(String foreign) {
        PlayStats stats = pending();
        HandStatistics statistics = new HandStatistics(HAND, Map.of("p1", stats));
        AcceptedAction observation = action(foreign.equals("hand") ? "other" : HAND,
                foreign.equals("player") ? "other" : "p1",
                BettingStreet.PREFLOP, ResolvedActionType.CALL, 20);
        assertThrows(IllegalArgumentException.class, () -> statistics.record(observation));
        stats.completeHand(HAND, false, false, 0, 0);
        assertEquals(0, stats.getVpipPercent().orElseThrow());
    }

    @Test
    void keepsCollectorValidationForActionsAfterFolding() {
        HandStatistics statistics = new HandStatistics(HAND, Map.of("p1", pending()));
        statistics.record(action(HAND, "p1", BettingStreet.PREFLOP, ResolvedActionType.FOLD, 0));
        assertThrows(IllegalArgumentException.class, () -> statistics.record(
                action(HAND, "p1", BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0)));
    }

    @Test
    void copiesThePlayerRegistryWhileRetainingItsCollectors() {
        PlayStats stats = pending();
        Map<String, PlayStats> registry = new HashMap<>(Map.of("p1", stats));
        HandStatistics statistics = new HandStatistics(HAND, registry);
        registry.clear();
        statistics.record(action(HAND, "p1", BettingStreet.PREFLOP, ResolvedActionType.CALL, 20));
        stats.completeHand(HAND, false, false, 0, 0);
        assertEquals(100, stats.getVpipPercent().orElseThrow());
    }

    @Test
    void startsCollectorsForEveryDealtPlayer() {
        GameState state = new GameInitializer().start(2);
        Consumer<AcceptedAction> record = HandStatistics.start(state);
        for (var player : state.getPlayers()) {
            String id = player.getId();
            record.accept(action(state.getHandId(), id, BettingStreet.PREFLOP,
                    ResolvedActionType.FOLD, 0));
            assertThrows(IllegalArgumentException.class, () -> record.accept(
                    action(state.getHandId(), id, BettingStreet.PREFLOP,
                            ResolvedActionType.CHECK, 0)));
        }
    }

    @Test
    void undealtSeatsCannotRecordVoluntaryActions() {
        GameState state = new GameSetup().create(1);
        Consumer<AcceptedAction> record = HandStatistics.start(state);
        assertThrows(IllegalArgumentException.class, () -> record.accept(action(
                state.getHandId(), state.getPlayers().get(0).getId(),
                BettingStreet.PREFLOP, ResolvedActionType.CALL, 20)));
    }

    private static PlayStats pending() {
        PlayStats stats = new PlayStats();
        stats.beginHand(HAND, true);
        return stats;
    }

    private static AcceptedAction action(String hand, String player, BettingStreet street,
            ResolvedActionType type, long added) {
        return new AcceptedAction(hand, player, street, type, added, added, false,
                new StatsContext(PreflopRaiseType.NONE, false, false, false));
    }
}
