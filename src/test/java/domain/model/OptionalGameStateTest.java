package domain.model;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OptionalGameStateTest {
    @Test
    void ncC01CreatesTheActingSnapshotWithExplicitPresence() {
        GameState before = FoundationFixtures.fixture().build();
        GameState after = copy(before, before.getCurrentActorId());
        assertEquals(Optional.of("p3"), after.getCurrentActorId());
        assertEquals(3, after.getCurrentActorSeat().orElseThrow());
        assertEquals(before.getPlayers(), after.getPlayers());
        assertEquals(before.getTotalChips(), after.getTotalChips());
        assertTrue(after.getBettingHistory().getFinalPreflopAggressorId().isEmpty());
    }
    @Test
    void ncC02CreatesAClosedSnapshotWithExplicitActorAbsence() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.betting = new BettingProgress(0, 20, Set.of(), Set.of());
        GameState after = GameState.create(fixture.config, fixture.hand, fixture.phase,
                fixture.players, fixture.positions, Optional.empty(), fixture.community,
                fixture.deck, fixture.betting);
        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getCurrentActorSeat().isEmpty());
        assertEquals(6000, after.getTotalChips());
    }
    @Test
    void ncC03PreservesExplicitEngineHistory() {
        GameState before = FoundationFixtures.fixture().build();
        BettingHistory history = new BettingHistory();
        GameState after = GameState.create(before.getConfig(), before.getHandId(), before.getPhase(),
                before.getPlayerStates(), before.getPositions(), before.getCurrentActorId(),
                before.getCommunityCards(), before.getRemainingDeck(), before.getBettingProgress(), history);
        assertSame(history, after.getBettingHistory());
        assertEquals(before.getCurrentActorId(), after.getCurrentActorId());
    }
    @Test
    void ncC05RejectsAnUnknownPresentActor() {
        GameState before = FoundationFixtures.fixture().build();
        assertThrows(IllegalArgumentException.class, () -> copy(before, Optional.of("unknown")));
    }
    @Test
    void ncC06DoesNotLosePendingDecisionValidation() {
        GameState before = FoundationFixtures.fixture().build();
        assertThrows(IllegalArgumentException.class, () -> copy(before, Optional.empty()));
    }
    private static GameState copy(GameState state, Optional<String> actor) {
        return GameState.create(state.getConfig(), state.getHandId(), state.getPhase(), state.getPlayerStates(),
                state.getPositions(), actor, state.getCommunityCards(), state.getRemainingDeck(), state.getBettingProgress());
    }
}
