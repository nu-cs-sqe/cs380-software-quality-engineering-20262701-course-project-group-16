package domain.model;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class FullBettingTest {
    private final BettingEngine engine = new BettingEngine();

    @ParameterizedTest
    @CsvSource({"FLOP,19,false", "FLOP,20,true", "FLOP,21,true", "FLOP,960,true", "FLOP,961,false",
        "TURN,19,false", "TURN,20,true", "TURN,21,true", "TURN,960,true", "TURN,961,false",
        "RIVER,19,false", "RIVER,20,true", "RIVER,21,true", "RIVER,960,true", "RIVER,961,false"})
    void openingBetsRespectMinimumAndStackBoundaries(GamePhase phase, long target, boolean legal) {
        GameState before = PostflopFixtures.build(PostflopFixtures.street(phase), new BettingHistory());
        List<Object> original = PostflopFixtures.snapshot(before);
        if (!legal) {
            assertThrows(IllegalArgumentException.class, () -> money(before, ActionType.BET, target));
        } else {
            BettingResult result = money(before, ActionType.BET, target);
            assertEquals(target, result.getAcceptedAction().getChipsAdded());
            assertEquals(target, result.getAcceptedAction().getRoundContributionAfter());
            assertEquals(ResolvedActionType.BET, result.getAcceptedAction().getType());
            assertEquals(target == 960, result.getAcceptedAction().becameAllIn());
            assertEquals(target, result.getState().getCurrentBet());
            assertEquals(target, result.getState().getLastFullRaiseSize());
            assertEquals(240 + target, result.getState().getPot());
            assertEquals(6000, result.getState().getTotalChips());
            assertEquals(phase, result.getState().getPhase());
        }
        assertEquals(original, PostflopFixtures.snapshot(before));
    }

    @ParameterizedTest
    @CsvSource({"PREFLOP,60,false", "PREFLOP,61,false", "PREFLOP,99,false", "PREFLOP,100,true", "PREFLOP,101,true",
        "FLOP,60,false", "FLOP,61,false", "FLOP,99,false", "FLOP,100,true", "FLOP,101,true",
        "TURN,60,false", "TURN,61,false", "TURN,99,false", "TURN,100,true", "TURN,101,true",
        "RIVER,60,false", "RIVER,61,false", "RIVER,99,false", "RIVER,100,true", "RIVER,101,true"})
    void raisesUseRoundTargetsAndTheLastFullIncrement(GamePhase phase, long target, boolean legal) {
        GameState before = facingSixty(phase);
        List<Object> original = PostflopFixtures.snapshot(before);
        if (!legal) {
            assertThrows(IllegalArgumentException.class, () -> money(before, ActionType.RAISE, target));
        } else {
            BettingResult result = money(before, ActionType.RAISE, target);
            assertEquals(target - 10, result.getAcceptedAction().getChipsAdded());
            assertEquals(target, result.getAcceptedAction().getRoundContributionAfter());
            assertEquals(target, result.getState().getCurrentBet());
            assertEquals(target - 60, result.getState().getLastFullRaiseSize());
            assertEquals(ResolvedActionType.RAISE, result.getAcceptedAction().getType());
            assertEquals(6000, result.getState().getTotalChips());
        }
        assertEquals(original, PostflopFixtures.snapshot(before));
    }

    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = {"PREFLOP", "FLOP", "TURN", "RIVER"})
    void callsAndFoldsWorkFacingRaisedRounds(GamePhase phase) {
        GameState before = facingSixty(phase);
        BettingResult call = engine.apply(before, request(before, ActionType.CALL));
        assertEquals(50, call.getAcceptedAction().getChipsAdded());
        assertEquals(60, call.getAcceptedAction().getRoundContributionAfter());
        assertEquals(60, call.getState().getCurrentBet());
        assertEquals(40, call.getState().getLastFullRaiseSize());
        assertEquals(ResolvedActionType.CALL, call.getAcceptedAction().getType());
        assertEquals(PlayerStatus.FOLDED, engine.apply(before, request(before, ActionType.FOLD))
                .getState().getPlayerStates().get(before.getCurrentActorSeat().orElseThrow()).getStatus());
        assertThrows(IllegalArgumentException.class, () -> engine.apply(before, request(before, ActionType.CHECK)));
    }

    @Test
    void aFullBetReopensDecisionsForPlayersWhoAlreadyChecked() {
        GameState before = PostflopFixtures.street(GamePhase.FLOP).build();
        GameState checked = engine.apply(before, request(before, ActionType.CHECK)).getState();
        GameState bet = money(checked, ActionType.BET, 20).getState();
        Set<String> expected = Set.of("p0", "p1", "p3", "p4", "p5");
        assertEquals(expected, bet.getBettingProgress().getPlayersToAct());
        assertEquals(expected, bet.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(3, bet.getCurrentActorSeat().orElseThrow());
        GameState raised = money(bet, ActionType.RAISE, 40).getState();
        assertEquals(Set.of("p0", "p1", "p2", "p4", "p5"), raised.getBettingProgress().getPlayersToAct());
        assertEquals(20, raised.getLastFullRaiseSize());
        assertEquals(6000, raised.getTotalChips());
    }

    @Test
    void raiseClassificationAndLatestAggressorFollowAcceptedPreflopRaises() {
        FoundationFixtures.Fixture fixture = preflop();
        GameState state = fixture.build();
        PreflopRaiseType[] kinds = {PreflopRaiseType.OPENING_RAISE, PreflopRaiseType.THREE_BET,
                PreflopRaiseType.FOUR_BET, PreflopRaiseType.LATER_RAISE};
        for (int index = 0; index < kinds.length; index++) {
            String actor = state.getCurrentActorId().orElseThrow();
            BettingResult result = money(state, ActionType.RAISE, 40 + index * 20);
            assertEquals(kinds[index], result.getAcceptedAction().getStatsContext().getPreflopRaiseType());
            assertEquals(actor, result.getState().getBettingHistory().getFinalPreflopAggressorId().orElseThrow());
            state = result.getState();
        }
    }

    @Test
    void wrongWagerKindsRightsAndAbsentOpponentsRejectWithoutChangingState() {
        GameState unopened = PostflopFixtures.street(GamePhase.FLOP).build();
        assertThrows(IllegalArgumentException.class, () -> money(unopened, ActionType.RAISE, 40));
        GameState facing = facingSixty(GamePhase.FLOP);
        assertThrows(IllegalArgumentException.class, () -> money(facing, ActionType.BET, 100));
        assertThrows(IllegalArgumentException.class, () -> money(preflop().build(), ActionType.BET, 40));
        FoundationFixtures.Fixture denied = PostflopFixtures.street(GamePhase.FLOP);
        denied.betting = new BettingProgress(0, 20, FoundationFixtures.allIds(), Set.of());
        assertThrows(IllegalArgumentException.class, () -> money(denied.build(), ActionType.BET, 20));
        FoundationFixtures.Fixture lone = PostflopFixtures.street(GamePhase.FLOP);
        for (int seat : new int[]{0, 2, 3, 4, 5}) lone.player(seat, 960, PlayerStatus.FOLDED, 0, 40);
        lone.betting = new BettingProgress(0, 20, Set.of("p1"), Set.of("p1"));
        assertThrows(IllegalArgumentException.class, () -> money(lone.build(), ActionType.BET, 20));
        assertEquals(240, unopened.getPot());
    }

    @Test
    void raiseToMaximumUsesOnlyTheRemainingStackAndRejectsOneMore() {
        GameState before = facingSixty(GamePhase.FLOP);
        BettingResult result = money(before, ActionType.RAISE, 960);
        assertEquals(950, result.getAcceptedAction().getChipsAdded());
        assertTrue(result.getAcceptedAction().becameAllIn());
        assertEquals(PlayerStatus.ALL_IN, result.getState().getPlayerStates().get(1).getStatus());
        assertThrows(IllegalArgumentException.class, () -> money(before, ActionType.RAISE, 961));
        assertEquals(6000, result.getState().getTotalChips());
    }

    @Test
    void aShortCallIsStillACallAndARaiseMustExceedTheCurrentTarget() {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(GamePhase.FLOP);
        PostflopFixtures.contribution(fixture, 0, 60);
        fixture.player(1, 20, PlayerStatus.ACTIVE, 10, 50);
        fixture.player(4, 1890, PlayerStatus.ACTIVE, 0, 40);
        fixture.betting = new BettingProgress(60, 40, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        GameState before = fixture.build();
        BettingResult called = engine.apply(before, request(before, ActionType.CALL));
        assertEquals(20, called.getAcceptedAction().getChipsAdded());
        assertTrue(called.getAcceptedAction().becameAllIn());
        assertEquals(ResolvedActionType.CALL, called.getAcceptedAction().getType());
        assertThrows(IllegalArgumentException.class, () -> money(before, ActionType.RAISE, 30));
    }

    @Test
    void anOpeningBetChangesTheFollowingMinimumRaise() {
        GameState bet = money(PostflopFixtures.street(GamePhase.TURN).build(), ActionType.BET, 60).getState();
        assertThrows(IllegalArgumentException.class, () -> money(bet, ActionType.RAISE, 119));
        GameState raised = money(bet, ActionType.RAISE, 120).getState();
        assertEquals(60, raised.getLastFullRaiseSize());
        assertThrows(IllegalArgumentException.class, () -> money(raised, ActionType.RAISE, 179));
        assertEquals(180, money(raised, ActionType.RAISE, 180).getState().getCurrentBet());
    }

    @ParameterizedTest
    @CsvSource({"50,false", "89,true", "90,true", "91,true"})
    void threeBetOpportunityIncludesAnAffordableShortAllIn(long stack, boolean opportunity) {
        FoundationFixtures.Fixture fixture = preflop();
        fixture.player(0, 940, PlayerStatus.ACTIVE, 60, 60);
        fixture.player(3, stack, PlayerStatus.ACTIVE, 10, 10);
        fixture.player(4, 1990 - stack, PlayerStatus.ACTIVE, 0, 0);
        fixture.betting = new BettingProgress(60, 40, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        GameState before = PostflopFixtures.build(fixture, new BettingHistory("p0", 0));
        BettingResult result = engine.apply(before, request(before, ActionType.CALL));
        assertEquals(opportunity, result.getAcceptedAction().getStatsContext().hasThreeBetOpportunity());
        assertEquals(50, result.getAcceptedAction().getChipsAdded());
        assertEquals(6000, result.getState().getTotalChips());
    }

    @ParameterizedTest
    @CsvSource({"19,false", "20,true", "21,true"})
    void openingMinimumUsesTheBigBlindAndAvailableStack(long stack, boolean fullBet) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(GamePhase.FLOP);
        fixture.player(1, stack, PlayerStatus.ACTIVE, 0, 40);
        fixture.player(4, 1920 - stack, PlayerStatus.ACTIVE, 0, 40);
        fixture.betting = new BettingProgress(0, 40, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        GameState before = PostflopFixtures.build(fixture, new BettingHistory("p1", 0));
        assertTrue(engine.apply(before, request(before, ActionType.CHECK))
                .getAcceptedAction().getStatsContext().hasFlopCBetOpportunity());
        if (fullBet) {
            BettingResult result = money(before, ActionType.BET, stack);
            assertEquals(stack, result.getState().getLastFullRaiseSize());
            assertEquals(stack, result.getState().getBettingHistory().getFlopCBetAmount().orElseThrow());
        } else {
            assertTrue(money(before, ActionType.BET, stack).getAcceptedAction().becameAllIn());
        }
    }

    @Test
    void setupAndUnopenedPreflopRejectWagersWithoutChangingSnapshots() {
        GameState setup = FoundationFixtures.fixture().build();
        assertThrows(UnsupportedOperationException.class, () -> money(setup, ActionType.BET, 20));
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.deal();
        GameState unopened = fixture.build();
        assertThrows(IllegalArgumentException.class, () -> money(unopened, ActionType.BET, 20));
        assertEquals(GamePhase.SETUP, setup.getPhase());
        assertEquals(0, unopened.getPot());
        assertEquals(6000, unopened.getTotalChips());
    }

    @Test
    void aMatchedPlayerAndTheAggressorAreNotFacingTheirOwnHistoricalCBet() {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(GamePhase.FLOP);
        PostflopFixtures.contribution(fixture, 0, 20);
        PostflopFixtures.contribution(fixture, 1, 20);
        fixture.betting = new BettingProgress(20, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        GameState matched = PostflopFixtures.build(fixture, new BettingHistory("p0", 20));
        assertFalse(engine.apply(matched, request(matched, ActionType.CHECK))
                .getAcceptedAction().getStatsContext().hasFacedFlopCBetOpportunity());
        fixture.actor = "p0";
        GameState aggressor = PostflopFixtures.build(fixture, new BettingHistory("p0", 20));
        assertFalse(engine.apply(aggressor, request(aggressor, ActionType.CHECK))
                .getAcceptedAction().getStatsContext().hasFacedFlopCBetOpportunity());
    }

    @Test
    void aShortBlindKeepsTheNominalTargetWhileTwoActivePlayersRemain() {
        FoundationFixtures.Fixture fixture = preflop();
        fixture.player(0, 3985, PlayerStatus.ACTIVE, 0, 0);
        fixture.player(2, 0, PlayerStatus.ALL_IN, 15, 15);
        fixture.player(4, 0, PlayerStatus.ELIMINATED, 0, 0);
        fixture.player(5, 0, PlayerStatus.ELIMINATED, 0, 0);
        fixture.betting = new BettingProgress(20, 20, Set.of("p0", "p1", "p3"), Set.of("p0", "p1", "p3"));
        GameState before = fixture.build();
        GameState after = engine.apply(before, request(before, ActionType.FOLD)).getState();
        assertEquals(20, after.getCurrentBet());
        assertEquals(0, after.getCurrentActorSeat().orElseThrow());
        assertEquals(20, engine.apply(after, request(after, ActionType.CALL)).getAcceptedAction().getChipsAdded());
        GameState lone = engine.apply(after, request(after, ActionType.FOLD)).getState();
        assertEquals(15, lone.getCurrentBet());
        assertEquals(Set.of("p1"), lone.getBettingProgress().getPlayersToAct());
        assertTrue(lone.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(5, engine.apply(lone, request(lone, ActionType.CALL)).getAcceptedAction().getChipsAdded());
        assertEquals(6000, lone.getTotalChips());
    }

    @Test
    void closingTheFinalDecisionClearsIndependentRaiseRights() {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(GamePhase.TURN);
        fixture.betting = new BettingProgress(0, 20, Set.of("p1"), Set.of("p0", "p1"));
        GameState after = engine.apply(fixture.build(), new ActionRequest("p1", ActionType.CHECK)).getState();
        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(6000, after.getTotalChips());
    }

    private BettingResult money(GameState state, ActionType type, long target) {
        return engine.apply(state, new ActionRequest(state.getCurrentActorId().orElseThrow(), type, target));
    }

    private static ActionRequest request(GameState state, ActionType type) {
        return new ActionRequest(state.getCurrentActorId().orElseThrow(), type);
    }

    private static FoundationFixtures.Fixture preflop() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.deal();
        fixture.player(1, 990, PlayerStatus.ACTIVE, 10, 10);
        fixture.player(2, 980, PlayerStatus.ACTIVE, 20, 20);
        fixture.betting = new BettingProgress(20, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        return fixture;
    }

    private static GameState facingSixty(GamePhase phase) {
        FoundationFixtures.Fixture fixture = phase == GamePhase.PREFLOP ? preflop() : PostflopFixtures.street(phase);
        int actor = phase == GamePhase.PREFLOP ? 3 : 1;
        if (phase == GamePhase.PREFLOP) {
            fixture.player(0, 940, PlayerStatus.ACTIVE, 60, 60);
            fixture.player(actor, 990, PlayerStatus.ACTIVE, 10, 10);
        } else {
            PostflopFixtures.contribution(fixture, 0, 60);
            PostflopFixtures.contribution(fixture, actor, 10);
        }
        fixture.betting = new BettingProgress(60, 40, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        return PostflopFixtures.build(fixture, new BettingHistory("p0", 0));
    }
}
