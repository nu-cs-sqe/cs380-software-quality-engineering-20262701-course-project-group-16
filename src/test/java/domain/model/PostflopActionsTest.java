package domain.model;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class PostflopActionsTest {
    private final BettingEngine engine = new BettingEngine();

    @ParameterizedTest(name = "{0}: {2} on {1}")
    @CsvSource({
        "BE-PF01-FLOP, FLOP, CHECK, FLOP, ACTIVE",
        "BE-PF01-TURN, TURN, CHECK, TURN, ACTIVE",
        "BE-PF01-RIVER, RIVER, CHECK, RIVER, ACTIVE",
        "BE-PF02-FLOP, FLOP, FOLD, FLOP, FOLDED",
        "BE-PF02-TURN, TURN, FOLD, TURN, FOLDED",
        "BE-PF02-RIVER, RIVER, FOLD, RIVER, FOLDED"
    })
    void zeroCostActionsPreserveChipsAndReportTheActualStreet(String id, GamePhase phase,
            ActionType type, BettingStreet street, PlayerStatus status) {
        GameState before = PostflopFixtures.build(PostflopFixtures.street(phase), new BettingHistory());
        List<Object> original = PostflopFixtures.snapshot(before);

        BettingResult result = act(before, 1, type);
        GameState after = result.getState();

        assertNotSame(before, after);
        assertEquals(status, after.getPlayerStates().get(1).getStatus(), id);
        assertEquals(2, after.getCurrentActorSeat().orElseThrow());
        Set<String> pending = Set.of("p0", "p2", "p3", "p4", "p5");
        assertEquals(pending, after.getBettingProgress().getPlayersToAct());
        assertEquals(pending, after.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(phase, after.getPhase());
        assertEquals(before.getHandId(), after.getHandId());
        assertEquals(before.getCommunityCards(), after.getCommunityCards());
        assertEquals(before.getRemainingDeck(), after.getRemainingDeck());
        assertEquals(0, after.getCurrentBet());
        assertEquals(20, after.getLastFullRaiseSize());
        assertEquals(240, after.getPot());
        assertEquals(6000, after.getTotalChips());
        for (int seat = 0; seat < 6; seat++) {
            assertEquals(960, after.getPlayers().get(seat).getChipStack());
            assertEquals(0, after.getPlayerStates().get(seat).getRoundContribution());
            assertEquals(40, after.getCommittedChips(seat));
            assertEquals(before.getPlayers().get(seat).getId(), after.getPlayers().get(seat).getId());
            assertEquals(before.getPlayers().get(seat).getType(), after.getPlayers().get(seat).getType());
            assertEquals(before.getPlayerStates().get(seat).getHoleCards(), after.getPlayerStates().get(seat).getHoleCards());
        }
        assertAction(result.getAcceptedAction(), before, street,
                type == ActionType.CHECK ? ResolvedActionType.CHECK : ResolvedActionType.FOLD, 0);
        assertContext(result, false, false);
        assertEquals(original, PostflopFixtures.snapshot(before));
    }

    @ParameterizedTest(name = "BE-PF03-{0}-{1}: reject CHECK with chips owed")
    @CsvSource({"FLOP,1", "FLOP,20", "TURN,1", "TURN,20", "RIVER,1", "RIVER,20"})
    void checkIsRejectedWhenChipsAreOwed(GamePhase phase, long owed) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(phase);
        PostflopFixtures.contribution(fixture, 0, 20);
        PostflopFixtures.contribution(fixture, 1, 20 - owed);
        fixture.betting = new BettingProgress(20, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        GameState before = PostflopFixtures.build(fixture, new BettingHistory());
        List<Object> original = PostflopFixtures.snapshot(before);

        assertThrows(IllegalArgumentException.class, () -> act(before, 1, ActionType.CHECK));

        assertEquals(original, PostflopFixtures.snapshot(before));
    }

    @ParameterizedTest(name = "BE-PF04-{0}: folding retains an existing current-round commitment")
    @EnumSource(value = GamePhase.class, names = {"FLOP", "TURN", "RIVER"})
    void foldRetainsPreviouslyCommittedChips(GamePhase phase) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(phase);
        PostflopFixtures.contribution(fixture, 0, 20);
        PostflopFixtures.contribution(fixture, 1, 10);
        fixture.betting = new BettingProgress(20, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        GameState before = PostflopFixtures.build(fixture, new BettingHistory());
        List<Object> original = PostflopFixtures.snapshot(before);

        BettingResult result = act(before, 1, ActionType.FOLD);
        GameState after = result.getState();

        assertEquals(PlayerStatus.FOLDED, after.getPlayerStates().get(1).getStatus());
        assertEquals(950, after.getPlayers().get(1).getChipStack());
        assertEquals(10, after.getPlayerStates().get(1).getRoundContribution());
        assertEquals(50, after.getCommittedChips(1));
        assertEquals(270, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertEquals(2, after.getCurrentActorSeat().orElseThrow());
        assertEquals(20, after.getCurrentBet());
        assertEquals(10, result.getAcceptedAction().getRoundContributionAfter());
        assertEquals(0, result.getAcceptedAction().getChipsAdded());
        assertEquals(original, PostflopFixtures.snapshot(before));
    }

    @ParameterizedTest(name = "BE-PF05-{0}-{1}: reject an out-of-turn action")
    @CsvSource({"FLOP,CHECK", "FLOP,FOLD", "TURN,CHECK", "TURN,FOLD", "RIVER,CHECK", "RIVER,FOLD"})
    void postflopActionsStillRequireTheCorrectTurn(GamePhase phase, ActionType type) {
        GameState before = PostflopFixtures.build(PostflopFixtures.street(phase), new BettingHistory());
        List<Object> original = PostflopFixtures.snapshot(before);

        assertThrows(IllegalArgumentException.class, () -> act(before, 4, type));

        assertEquals(original, PostflopFixtures.snapshot(before));
    }

    @ParameterizedTest(name = "BE-PF06-{0}: final CHECK closes decisions without dealing")
    @EnumSource(value = GamePhase.class, names = {"FLOP", "TURN", "RIVER"})
    void theLastCheckClosesTheRound(GamePhase phase) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(phase);
        fixture.betting = new BettingProgress(0, 20, Set.of("p1"), Set.of("p0", "p1"));
        GameState before = PostflopFixtures.build(fixture, new BettingHistory());

        GameState after = act(before, 1, ActionType.CHECK).getState();

        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(phase, after.getPhase());
        assertEquals(before.getCommunityCards(), after.getCommunityCards());
        assertEquals(before.getRemainingDeck(), after.getRemainingDeck());
        assertEquals(240, after.getPot());
        assertEquals(6000, after.getTotalChips());
    }

    @ParameterizedTest(name = "BE-PF07-{0}: final FOLD stops for settlement")
    @EnumSource(value = GamePhase.class, names = {"FLOP", "TURN", "RIVER"})
    void oneRemainingParticipantGetsNoFurtherDecisionOrPrematurePayout(GamePhase phase) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(phase);
        for (int seat = 2; seat < 6; seat++) {
            fixture.player(seat, 960, PlayerStatus.FOLDED, 0, 40);
        }
        fixture.betting = new BettingProgress(0, 20, Set.of("p0", "p1"), Set.of("p0", "p1"));
        GameState before = PostflopFixtures.build(fixture, new BettingHistory());

        GameState after = act(before, 1, ActionType.FOLD).getState();

        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(1, after.getPlayerStates().stream().filter(PlayerState::isParticipating).count());
        assertEquals(240, after.getPot());
        assertEquals(960, after.getPlayers().get(0).getChipStack());
        assertEquals(6000, after.getTotalChips());
        assertEquals(before.getCommunityCards(), after.getCommunityCards());
        assertThrows(IllegalArgumentException.class, () -> engine.advanceStreet(after));
    }

    @Test
    @DisplayName("BE-PF08: skip folded/all-in/eliminated and already-acted seats")
    void actorSelectionUsesOnlyPendingActivePlayers() {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(GamePhase.FLOP);
        fixture.actor = "p0";
        fixture.player(1, 960, PlayerStatus.FOLDED, 0, 40);
        fixture.player(2, 0, PlayerStatus.ALL_IN, 0, 40);
        fixture.player(3, 0, PlayerStatus.ELIMINATED, 0, 0);
        fixture.player(0, 2920, PlayerStatus.ACTIVE, 0, 40);
        fixture.betting = new BettingProgress(0, 20, Set.of("p0", "p4"), Set.of("p0", "p4"));
        GameState before = PostflopFixtures.build(fixture, new BettingHistory());

        GameState afterFirst = act(before, 0, ActionType.CHECK).getState();
        assertEquals(4, afterFirst.getCurrentActorSeat().orElseThrow());
        assertEquals(Set.of("p4"), afterFirst.getBettingProgress().getPlayersToAct());
        GameState afterLast = act(afterFirst, 4, ActionType.CHECK).getState();

        assertTrue(afterLast.getCurrentActorId().isEmpty());
        assertTrue(afterLast.getBettingProgress().getPlayersToAct().isEmpty());
        assertEquals(PlayerStatus.ACTIVE, afterLast.getPlayerStates().get(5).getStatus());
        assertEquals(PlayerStatus.ALL_IN, afterLast.getPlayerStates().get(2).getStatus());
        assertEquals(200, afterLast.getPot());
        assertEquals(6000, afterLast.getTotalChips());
    }

    @ParameterizedTest(name = "BE-PF09-{0}: reject actions after the round closes")
    @EnumSource(value = GamePhase.class, names = {"FLOP", "TURN", "RIVER"})
    void aClosedPostflopRoundAcceptsNoMoreActions(GamePhase phase) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(phase);
        PostflopFixtures.close(fixture);
        GameState before = PostflopFixtures.build(fixture, new BettingHistory());
        List<Object> original = PostflopFixtures.snapshot(before);

        assertThrows(IllegalArgumentException.class, () -> act(before, 1, ActionType.CHECK));

        assertEquals(original, PostflopFixtures.snapshot(before));
    }

    @Test
    @DisplayName("BE-PF10: an unknown postflop player cannot act")
    void anUnknownPlayerCannotAct() {
        GameState before = PostflopFixtures.build(PostflopFixtures.street(GamePhase.FLOP), new BettingHistory());
        List<Object> original = PostflopFixtures.snapshot(before);
        assertThrows(IllegalArgumentException.class,
                () -> engine.apply(before, new ActionRequest("unknown", ActionType.FOLD)));
        assertEquals(original, PostflopFixtures.snapshot(before));
    }

    @ParameterizedTest(name = "BE-PC01-{0}: final preflop aggressor declines an eligible c-bet")
    @EnumSource(value = ActionType.class, names = {"CHECK", "FOLD"})
    void anEligibleAggressorGetsExplicitOpeningOpportunityContext(ActionType type) {
        GameState before = PostflopFixtures.build(PostflopFixtures.street(GamePhase.FLOP),
                new BettingHistory("p1", 0));
        BettingResult result = act(before, 1, type);
        assertContext(result, true, false);
        assertEquals("p1", result.getState().getBettingHistory().getFinalPreflopAggressorId().orElseThrow());
        assertTrue(result.getState().getBettingHistory().getFlopCBetAmount().isEmpty());
    }

    @Test
    @DisplayName("BE-PC02: another player's preflop aggression creates no opening opportunity")
    void anotherPlayerIsNotTheContinuationBettor() {
        GameState before = PostflopFixtures.build(PostflopFixtures.street(GamePhase.FLOP),
                new BettingHistory("p0", 0));
        assertContext(act(before, 1, ActionType.CHECK), false, false);
    }

    @Test
    @DisplayName("BE-PC03: missing betting rights prevents c-bet opportunity")
    void anAggressorWithoutBettingRightsCannotOpen() {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(GamePhase.FLOP);
        fixture.betting = new BettingProgress(0, 20, FoundationFixtures.allIds(), Set.of("p0", "p2", "p3", "p4", "p5"));
        GameState before = PostflopFixtures.build(fixture, new BettingHistory("p1", 0));
        assertContext(act(before, 1, ActionType.CHECK), false, false);
    }

    @Test
    @DisplayName("BE-PC04: fold to a recorded actual flop continuation bet")
    void foldFacingAnActualCBetCarriesTheOpportunityAndRetainsHistory() {
        FoundationFixtures.Fixture fixture = facingBet(GamePhase.FLOP, 20);
        GameState before = PostflopFixtures.build(fixture, new BettingHistory("p0", 20));
        BettingResult result = act(before, 1, ActionType.FOLD);
        assertContext(result, false, true);
        assertEquals(ResolvedActionType.FOLD, result.getAcceptedAction().getType());
        assertEquals("p0", result.getState().getBettingHistory().getFinalPreflopAggressorId().orElseThrow());
        assertEquals(20, result.getState().getBettingHistory().getFlopCBetAmount().orElseThrow());
    }

    @Test
    @DisplayName("BE-PC05: an ordinary opening bet is not inferred to be a c-bet")
    void anUnrecordedOpeningBetCreatesNoFacedCBetContext() {
        GameState before = PostflopFixtures.build(facingBet(GamePhase.FLOP, 20),
                new BettingHistory("p5", 0));
        assertContext(act(before, 1, ActionType.FOLD), false, false);
    }

    @Test
    @DisplayName("BE-PC06: folding to a raise after a c-bet is not folding to the original c-bet")
    void anInterveningRaiseIsNotTheOriginalContinuationBet() {
        FoundationFixtures.Fixture fixture = facingBet(GamePhase.FLOP, 20);
        PostflopFixtures.contribution(fixture, 2, 60);
        fixture.betting = new BettingProgress(60, 40, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        GameState before = PostflopFixtures.build(fixture, new BettingHistory("p0", 20));
        assertContext(act(before, 1, ActionType.FOLD), false, false);
    }

    @Test
    @DisplayName("BE-PC07: facing another opening bet prevents the aggressor's opening opportunity")
    void anAggressorFacingAnotherOpeningBetCannotCBet() {
        GameState before = PostflopFixtures.build(facingBet(GamePhase.FLOP, 20),
                new BettingHistory("p1", 0));
        assertContext(act(before, 1, ActionType.FOLD), false, false);
    }

    @ParameterizedTest(name = "BE-PC08-{0}: a historical flop c-bet creates no later-street opportunity")
    @EnumSource(value = GamePhase.class, names = {"TURN", "RIVER"})
    void aPastCBetIsNotACurrentTurnOrRiverCBet(GamePhase phase) {
        GameState before = PostflopFixtures.build(facingBet(phase, 20), new BettingHistory("p0", 20));
        assertContext(act(before, 1, ActionType.FOLD), false, false);
    }

    @ParameterizedTest(name = "BE-PF11-{0}-{1}: other postflop actions remain outside this increment")
    @CsvSource({"FLOP,CALL", "FLOP,BET", "FLOP,RAISE", "TURN,CALL", "TURN,BET", "TURN,RAISE",
        "RIVER,CALL", "RIVER,BET", "RIVER,RAISE"})
    void unsupportedPostflopActionsRemainExplicit(GamePhase phase, ActionType type) {
        GameState before = PostflopFixtures.build(PostflopFixtures.street(phase), new BettingHistory());
        List<Object> original = PostflopFixtures.snapshot(before);
        ActionRequest request = type == ActionType.CALL ? new ActionRequest("p1", type)
                : new ActionRequest("p1", type, 60);
        assertThrows(UnsupportedOperationException.class, () -> engine.apply(before, request));
        assertEquals(original, PostflopFixtures.snapshot(before));
    }

    private static FoundationFixtures.Fixture facingBet(GamePhase phase, long amount) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(phase);
        PostflopFixtures.contribution(fixture, 0, amount);
        fixture.betting = new BettingProgress(amount, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        return fixture;
    }

    private BettingResult act(GameState state, int seat, ActionType type) {
        return engine.apply(state, new ActionRequest(state.getPlayers().get(seat).getId(), type));
    }

    private static void assertAction(AcceptedAction action, GameState before, BettingStreet street,
            ResolvedActionType type, long contribution) {
        assertEquals(before.getHandId(), action.getHandId());
        assertEquals(before.getCurrentActorId().orElseThrow(), action.getPlayerId());
        assertEquals(street, action.getStreet());
        assertEquals(type, action.getType());
        assertEquals(0, action.getChipsAdded());
        assertEquals(contribution, action.getRoundContributionAfter());
        assertFalse(action.becameAllIn());
        assertTrue(action.toStatsAction().isAccepted());
        assertEquals(street, action.toStatsAction().getStreet());
        assertEquals(type, action.toStatsAction().getType());
    }

    private static void assertContext(BettingResult result, boolean opening, boolean facing) {
        StatsContext context = result.getAcceptedAction().getStatsContext();
        assertEquals(PreflopRaiseType.NONE, context.getPreflopRaiseType());
        assertFalse(context.hasThreeBetOpportunity());
        assertEquals(opening, context.hasFlopCBetOpportunity());
        assertEquals(facing, context.hasFacedFlopCBetOpportunity());
    }
}
