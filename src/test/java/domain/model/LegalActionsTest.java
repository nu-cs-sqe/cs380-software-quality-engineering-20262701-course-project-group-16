package domain.model;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class LegalActionsTest {
    private final BettingEngine engine = new BettingEngine();

    @ParameterizedTest
    @CsvSource({"19, 19, false, false", "20, 20, false, false", "21, 20, true, true",
        "39, 20, true, true", "40, 20, true, false", "41, 20, true, false"})
    void optionsDistinguishCappedCallsShortAllInsAndFullRaises(long stack, long call,
            boolean canRaise, boolean shortRaise) {
        GameState state = preflop(stack, 20, true);
        List<Object> before = PostflopFixtures.snapshot(state);
        LegalActions options = engine.getLegalActions(state);
        assertEquals(call, options.getCallCost());
        assertEquals(canRaise ? Set.of(ActionType.FOLD, ActionType.CALL, ActionType.RAISE)
                : Set.of(ActionType.FOLD, ActionType.CALL), options.getActions());
        assertThrows(UnsupportedOperationException.class, () -> options.getActions().clear());
        assertEquals(call, engine.apply(state, new ActionRequest("p3", ActionType.CALL))
                .getAcceptedAction().getChipsAdded());
        if (canRaise) {
            LegalActions.Wager wager = options.getWager().orElseThrow();
            assertEquals(ActionType.RAISE, wager.getType());
            assertEquals(40, wager.getMinimumFullTarget().orElseThrow());
            assertEquals(stack, wager.getMaximumTarget());
            assertEquals(shortRaise, wager.getShortAllInTarget().isPresent());
            for (long target : new long[]{20, 21, 39, 40, 41, stack, stack + 1}) {
                verifyTarget(state, wager, target, target <= stack
                        && (target >= 40 || target == stack));
            }
        } else {
            assertTrue(options.getWager().isEmpty());
        }
        assertEquals(before, PostflopFixtures.snapshot(state));
    }

    @Test
    void openingOptionsAreBetTargetsAndCheckingCostsNothing() {
        GameState state = PostflopFixtures.street(GamePhase.FLOP).build();
        LegalActions options = engine.getLegalActions(state);
        assertEquals(Set.of(ActionType.FOLD, ActionType.CHECK, ActionType.BET), options.getActions());
        assertEquals(0, options.getCallCost());
        LegalActions.Wager wager = options.getWager().orElseThrow();
        assertEquals(ActionType.BET, wager.getType());
        assertEquals(20, wager.getMinimumFullTarget().orElseThrow());
        assertEquals(960, wager.getMaximumTarget());
        verifyTarget(state, wager, 19, false);
        verifyTarget(state, wager, 20, true);
    }

    @Test
    void closedRaiseRightsAndNonBettingPhasesDoNotAdvertiseWagers() {
        assertEquals(Set.of(ActionType.FOLD, ActionType.CALL),
                engine.getLegalActions(preflop(1000, 20, false)).getActions());
        assertTrue(engine.getLegalActions(FoundationFixtures.fixture().build()).getActions().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> engine.getLegalActions(null));
    }

    @Test
    void unrepresentableFullMinimumStillOffersExactShortAllIn() {
        GameState state = preflop(1000, Long.MAX_VALUE, true);
        LegalActions.Wager wager = engine.getLegalActions(state).getWager().orElseThrow();
        assertTrue(wager.getMinimumFullTarget().isEmpty());
        assertEquals(1000, wager.getShortAllInTarget().orElseThrow());
        verifyTarget(state, wager, 999, false);
        verifyTarget(state, wager, 1000, true);
    }

    @Test
    void maximumRepresentableFullTargetIsStillReported() {
        LegalActions.Wager wager = engine.getLegalActions(preflop(1000, Long.MAX_VALUE - 20, true))
                .getWager().orElseThrow();
        assertEquals(Long.MAX_VALUE, wager.getMinimumFullTarget().orElseThrow());
        assertEquals(1000, wager.getShortAllInTarget().orElseThrow());
    }

    @Test
    void aClosedBettingRoundHasNoOptionsAndZeroPreflopTargetCannotOpenWithBet() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.deal();
        assertEquals(Set.of(ActionType.CHECK, ActionType.FOLD),
                engine.getLegalActions(fixture.build()).getActions());
        fixture.actor = null;
        fixture.betting = new BettingProgress(0, 20, Set.of(), Set.of());
        assertTrue(engine.getLegalActions(fixture.build()).getActions().isEmpty());
    }

    private void verifyTarget(GameState state, LegalActions.Wager wager, long target,
            boolean legal) {
        assertEquals(legal, wager.allows(target));
        ActionRequest request = new ActionRequest(state.getCurrentActorId().orElseThrow(),
                wager.getType(), target);
        if (legal) assertEquals(target, engine.apply(state, request).getState().getCurrentBet());
        else assertThrows(IllegalArgumentException.class, () -> engine.apply(state, request));
    }

    private static GameState preflop(long stack, long increment, boolean canRaise) {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.deal();
        fixture.player(1, 990, PlayerStatus.ACTIVE, 10, 10);
        fixture.player(2, 980, PlayerStatus.ACTIVE, 20, 20);
        fixture.player(3, stack, PlayerStatus.ACTIVE, 0, 0);
        fixture.player(4, 2000 - stack, PlayerStatus.ACTIVE, 0, 0);
        fixture.betting = new BettingProgress(20, increment, FoundationFixtures.allIds(),
                canRaise ? FoundationFixtures.allIds() : Set.of());
        return fixture.build();
    }
}
