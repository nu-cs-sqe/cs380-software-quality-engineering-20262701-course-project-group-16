package domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ReopeningProgressTest {
    @Test
    void savedDecisionLevelsAreIndependentAndImmutable() {
        Map<String, Long> input = new HashMap<>();
        input.put("p1", 0L);
        BettingProgress progress = new BettingProgress(0, 20, Set.of("p1"), Set.of(), input);
        input.put("p1", 40L);
        assertEquals(Map.of("p1", 0L), progress.getLastDecisionTargets());
        assertThrows(UnsupportedOperationException.class, () -> progress.getLastDecisionTargets().put("p2", 0L));
        assertTrue(new BettingProgress(0, 20, Set.of(), Set.of()).getLastDecisionTargets().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,true", "19,true", "20,true", "21,false"})
    void decisionLevelsMustFitWithinTheRoundTarget(long level, boolean valid) {
        if (valid) {
            BettingProgress progress = new BettingProgress(20, 20, Set.of("p1"), Set.of(), Map.of("p1", level));
            assertEquals(level, progress.getLastDecisionTargets().get("p1"));
        } else {
            assertThrows(IllegalArgumentException.class,
                    () -> new BettingProgress(20, 20, Set.of("p1"), Set.of(), Map.of("p1", level)));
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void decisionLevelsMustNameAPlayer(String id) {
        Map<String, Long> targets = new HashMap<>();
        targets.put(id, 0L);
        assertThrows(IllegalArgumentException.class,
                () -> new BettingProgress(0, 20, Set.of(), Set.of(), targets));
    }

    @Test
    void nullDecisionMapsAndValuesAreRejectedAtTheSnapshotBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> new BettingProgress(0, 20, Set.of(), Set.of(), null));
        Map<String, Long> targets = new HashMap<>();
        targets.put("p1", null);
        assertThrows(IllegalArgumentException.class,
                () -> new BettingProgress(0, 20, Set.of(), Set.of(), targets));
    }

    @Test
    void gameStateRejectsDecisionHistoryForAnUnknownOrInactivePlayer() {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(GamePhase.FLOP);
        fixture.betting = new BettingProgress(0, 20, FoundationFixtures.allIds(), Set.of(), Map.of("unknown", 0L));
        assertThrows(IllegalArgumentException.class, fixture::build);
        fixture.player(4, 960, PlayerStatus.FOLDED, 0, 40);
        fixture.betting = new BettingProgress(0, 20, Set.of("p1"), Set.of(), Map.of("p4", 0L));
        assertThrows(IllegalArgumentException.class, fixture::build);
    }

    @Test
    void anAcceptedDecisionIsRetainedUntilTheNextStreetAndDoesNotAlterItsInput() {
        GameState state = PostflopFixtures.street(GamePhase.FLOP).build();
        BettingEngine engine = new BettingEngine();
        GameState checked = engine.apply(state, new ActionRequest("p1", ActionType.CHECK)).getState();
        assertEquals(Map.of("p1", 0L), checked.getBettingProgress().getLastDecisionTargets());
        assertTrue(state.getBettingProgress().getLastDecisionTargets().isEmpty());
        for (int count = 0; count < 5 && checked.getCurrentActorId().isPresent(); count++) {
            checked = engine.apply(checked,
                    new ActionRequest(checked.getCurrentActorId().orElseThrow(), ActionType.CHECK)).getState();
        }
        assertTrue(checked.getCurrentActorId().isEmpty());
        assertEquals(6, checked.getBettingProgress().getLastDecisionTargets().size());
        GameState turn = engine.advanceStreet(checked);
        assertTrue(turn.getBettingProgress().getLastDecisionTargets().isEmpty());
        assertEquals(6, turn.getBettingProgress().getPlayersAllowedToRaise().size());
    }
}
