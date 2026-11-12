package domain.model;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ShortAllInBettingTest {
    private final BettingEngine engine = new BettingEngine();

    @ParameterizedTest
    @ValueSource(longs = {1, 19, 20, 21})
    void openingAllInsPreserveTheFullMinimum(long stack) {
        FoundationFixtures.Fixture fixture = street();
        stack(fixture, 1, stack);
        GameState before = fixture.build();
        BettingResult bet = money(before, ActionType.BET, stack);
        assertEquals(stack, bet.getAcceptedAction().getChipsAdded());
        assertTrue(bet.getAcceptedAction().becameAllIn());
        assertEquals(ResolvedActionType.BET, bet.getAcceptedAction().getType());
        assertEquals(Math.max(20, stack), bet.getState().getLastFullRaiseSize());
        assertEquals(Set.of("p0", "p2", "p3", "p4", "p5"), bet.getState().getBettingProgress().getPlayersToAct());
        assertEquals(6000, bet.getState().getTotalChips());
        assertEquals(0, before.getCurrentBet());
    }

    @ParameterizedTest
    @CsvSource({"101,100", "199,100", "200,100", "201,101"})
    void fullAndShortRaisesUpdateDifferentRoundFacts(long target, long increment) {
        FoundationFixtures.Fixture fixture = street();
        stack(fixture, 2, target);
        GameState bet = money(fixture.build(), ActionType.BET, 100).getState();
        BettingResult raised = money(bet, ActionType.RAISE, target);
        assertEquals(target, raised.getState().getCurrentBet());
        assertEquals(increment, raised.getState().getLastFullRaiseSize());
        assertEquals(target >= 200, raised.getState().getBettingProgress().getPlayersAllowedToRaise().contains("p1"));
        assertTrue(raised.getState().getBettingProgress().getPlayersToAct().contains("p1"));
        assertFalse(raised.getState().getBettingProgress().getPlayersAllowedToRaise().contains("p2"));
        assertEquals(ResolvedActionType.RAISE, raised.getAcceptedAction().getType());
        assertTrue(raised.getAcceptedAction().becameAllIn());
        assertEquals(6000, raised.getState().getTotalChips());
    }

    @ParameterizedTest
    @CsvSource({"199,false", "200,true", "201,true"})
    void cumulativeReopeningUsesEachPlayersOwnLastDecision(long target, boolean reopened) {
        GameState state = cumulative(target);
        assertEquals(1, state.getCurrentActorSeat().orElseThrow());
        assertEquals(reopened ? Set.of("p1") : Set.of(), state.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(Set.of("p1", "p3"), state.getBettingProgress().getPlayersToAct());
        assertEquals(100, state.getLastFullRaiseSize());
        List<Object> original = PostflopFixtures.snapshot(state);
        if (reopened) {
            GameState raised = money(state, ActionType.RAISE, target + 100).getState();
            assertEquals(Set.of("p3"), raised.getBettingProgress().getPlayersAllowedToRaise());
        } else {
            assertThrows(IllegalArgumentException.class, () -> money(state, ActionType.RAISE, target + 100));
        }
        GameState called = act(state, ActionType.CALL).getState();
        assertEquals(3, called.getCurrentActorSeat().orElseThrow());
        assertTrue(called.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> money(called, ActionType.RAISE, 960));
        assertTrue(act(called, ActionType.CALL).getState().getCurrentActorId().isEmpty());
        assertEquals(original, PostflopFixtures.snapshot(state));
    }

    @Test
    void aPlayerWhoHasNotActedMayRaiseAShortOpeningByTheFullMinimum() {
        FoundationFixtures.Fixture fixture = street();
        stack(fixture, 1, 5);
        GameState bet = money(fixture.build(), ActionType.BET, 5).getState();
        assertThrows(IllegalArgumentException.class, () -> money(bet, ActionType.RAISE, 24));
        GameState raised = money(bet, ActionType.RAISE, 25).getState();
        assertEquals(25, raised.getCurrentBet());
        assertEquals(20, raised.getLastFullRaiseSize());
        assertEquals(6000, raised.getTotalChips());
    }

    @ParameterizedTest
    @CsvSource({"19,false", "20,true", "21,true"})
    void checkedPlayersReopenOnlyAfterFacingAFullCumulativeBet(long target, boolean reopened) {
        FoundationFixtures.Fixture fixture = street();
        stack(fixture, 2, 5);
        stack(fixture, 3, target);
        GameState checked = act(fixture.build(), ActionType.CHECK).getState();
        GameState shortBet = money(checked, ActionType.BET, 5).getState();
        assertFalse(shortBet.getBettingProgress().getPlayersAllowedToRaise().contains("p1"));
        GameState next = money(shortBet, ActionType.RAISE, target).getState();
        assertEquals(reopened, next.getBettingProgress().getPlayersAllowedToRaise().contains("p1"));
        assertEquals(20, next.getLastFullRaiseSize());
    }

    @Test
    void callingAnIntermediateShortRaiseResetsOnlyThatPlayersReopeningLevel() {
        FoundationFixtures.Fixture fixture = street();
        stack(fixture, 3, 125);
        stack(fixture, 5, 200);
        GameState state = money(fixture.build(), ActionType.BET, 100).getState();
        state = act(state, ActionType.CALL).getState(); // seat2 at100
        state = money(state, ActionType.RAISE, 125).getState(); // seat3 all-in
        state = act(state, ActionType.CALL).getState(); // seat4 at125
        state = money(state, ActionType.RAISE, 200).getState(); // seat5 all-in
        state = act(state, ActionType.FOLD).getState(); // seat0
        assertEquals(Set.of("p1", "p2"), state.getBettingProgress().getPlayersAllowedToRaise());
        state = act(state, ActionType.CALL).getState(); // seat1 at200
        state = act(state, ActionType.CALL).getState(); // seat2 at200
        assertEquals(4, state.getCurrentActorSeat().orElseThrow());
        assertTrue(state.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
    }

    @Test
    void anUndersizedWagerWithChipsBehindIsRejectedWithoutChanges() {
        GameState unopened = street().build();
        List<Object> original = PostflopFixtures.snapshot(unopened);
        assertThrows(IllegalArgumentException.class, () -> money(unopened, ActionType.BET, 19));
        GameState bet = money(unopened, ActionType.BET, 100).getState();
        assertThrows(IllegalArgumentException.class, () -> money(bet, ActionType.RAISE, 199));
        assertEquals(original, PostflopFixtures.snapshot(unopened));
    }

    @Test
    void shortPreflopRaisesStillProduceUsableAcceptedStatistics() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.deal();
        fixture.player(1, 990, PlayerStatus.ACTIVE, 10, 10);
        fixture.player(2, 980, PlayerStatus.ACTIVE, 20, 20);
        stack(fixture, 4, 50);
        fixture.betting = new BettingProgress(20, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        GameState opened = money(fixture.build(), ActionType.RAISE, 40).getState();
        BettingResult shortRaise = money(opened, ActionType.RAISE, 50);
        assertEquals(PreflopRaiseType.THREE_BET, shortRaise.getAcceptedAction().getStatsContext().getPreflopRaiseType());
        assertTrue(shortRaise.getAcceptedAction().getStatsContext().hasThreeBetOpportunity());
        assertEquals(20, shortRaise.getState().getLastFullRaiseSize());
        assertFalse(shortRaise.getState().getBettingProgress().getPlayersAllowedToRaise().contains("p3"));
    }

    @Test
    void aShortOpeningUsesTheBigBlindRatherThanAStalePriorIncrement() {
        FoundationFixtures.Fixture fixture = street();
        stack(fixture, 1, 5);
        fixture.betting = new BettingProgress(0, 40, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        GameState bet = money(fixture.build(), ActionType.BET, 5).getState();
        assertEquals(20, bet.getLastFullRaiseSize());
        assertEquals(25, money(bet, ActionType.RAISE, 25).getState().getCurrentBet());
    }

    @Test
    void matchingTheWagerAllInIsACallRatherThanARaise() {
        FoundationFixtures.Fixture fixture = street();
        stack(fixture, 2, 100);
        GameState bet = money(fixture.build(), ActionType.BET, 100).getState();
        assertThrows(IllegalArgumentException.class, () -> money(bet, ActionType.RAISE, 100));
        BettingResult called = act(bet, ActionType.CALL);
        assertTrue(called.getAcceptedAction().becameAllIn());
        assertEquals(ResolvedActionType.CALL, called.getAcceptedAction().getType());
        assertEquals(100, called.getAcceptedAction().getChipsAdded());
    }

    @Test
    void aMinimumFullRaiseReopensLegacySnapshotsWithoutStoredDecisionLevels() {
        FoundationFixtures.Fixture fixture = street();
        PostflopFixtures.contribution(fixture, 0, 20);
        Set<String> unacted = Set.of("p1", "p2", "p3", "p4", "p5");
        fixture.betting = new BettingProgress(20, 20, unacted, unacted);
        GameState raised = money(fixture.build(), ActionType.RAISE, 40).getState();
        assertEquals(Set.of("p0", "p2", "p3", "p4", "p5"), raised.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(Set.of("p0", "p2", "p3", "p4", "p5"), raised.getBettingProgress().getPlayersToAct());
        assertEquals(20, raised.getLastFullRaiseSize());
    }

    @Test
    void anOpeningFullBetUsesTheBigBlindEvenWhenThePlayerHasChipsBehind() {
        FoundationFixtures.Fixture fixture = street();
        fixture.betting = new BettingProgress(0, 40, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        BettingResult result = money(fixture.build(), ActionType.BET, 20);
        assertFalse(result.getAcceptedAction().becameAllIn());
        assertEquals(20, result.getState().getLastFullRaiseSize());
        assertEquals(940, result.getState().getPlayers().get(1).getChipStack());
    }

    private GameState cumulative(long target) {
        FoundationFixtures.Fixture fixture = street();
        stack(fixture, 2, 125);
        stack(fixture, 4, target);
        GameState state = money(fixture.build(), ActionType.BET, 100).getState();
        state = money(state, ActionType.RAISE, 125).getState();
        state = act(state, ActionType.CALL).getState();
        state = money(state, ActionType.RAISE, target).getState();
        state = act(state, ActionType.FOLD).getState();
        return act(state, ActionType.FOLD).getState();
    }

    private BettingResult money(GameState state, ActionType type, long target) {
        return engine.apply(state, new ActionRequest(state.getCurrentActorId().orElseThrow(), type, target));
    }

    private BettingResult act(GameState state, ActionType type) {
        return engine.apply(state, new ActionRequest(state.getCurrentActorId().orElseThrow(), type));
    }

    private static FoundationFixtures.Fixture street() { return PostflopFixtures.street(GamePhase.FLOP); }

    private static void stack(FoundationFixtures.Fixture fixture, int seat, long amount) {
        PlayerState before = fixture.players.get(seat);
        long difference = before.getPlayer().getChipStack() - amount;
        fixture.player(seat, amount, PlayerStatus.ACTIVE, before.getRoundContribution(), before.getHandContribution());
        PlayerState other = fixture.players.get(0);
        fixture.player(0, other.getPlayer().getChipStack() + difference, PlayerStatus.ACTIVE,
                other.getRoundContribution(), other.getHandContribution());
    }
}
