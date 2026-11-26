package application;

import bot.learning.OpponentBeliefs;
import bot.policy.CallValue;
import bot.policy.KnownOutcomePolicy;
import bot.policy.KnownOutcomePolicy.Plan;
import bot.policy.PolicyRequest;
import domain.model.ActionType;
import domain.model.BettingStreet;
import domain.model.ResolvedActionType;
import domain.model.RiverOutcomeEvaluator.Outcome;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnownOutcomePolicyTest {
    private static final String OPPONENT = "p1";
    private static final int STACK_BEHIND = 600;
    private static final double CHIP_EPSILON = 0.000_001;
    private final KnownOutcomePolicy policy = new KnownOutcomePolicy();

    @ParameterizedTest
    @CsvSource({"FOLD,20,40.472727234", "CALL,40,58.071367894", "RAISE,30,62.917026635"})
    void valuesBothTheSelectedWagerAndItsProjectedReturn(ResolvedActionType observed,
            long target, double netChips) {
        var beliefs = BotRiverDecisionsTest.learnedResponse(BettingStreet.FLOP, 20, observed);
        var plan = plan(turn(2, 0, STACK_BEHIND), Outcome.GUARANTEED_WIN,
                Map.of(OPPONENT, beliefs));
        assertEquals(ActionType.BET, plan.getAction().getType());
        assertEquals(target, plan.getAction().getTargetAmount().orElseThrow());
        assertEquals(netChips, plan.getProjectedNetChips(), CHIP_EPSILON);
    }

    @Test
    void aWinnerCanCheckToInduceAPlayerWhoOpensButFoldsToBets() {
        List<ResolvedActionType> publicReplies = new ArrayList<>(
                Collections.nCopies(20, ResolvedActionType.FOLD));
        publicReplies.addAll(Collections.nCopies(60, ResolvedActionType.BET));
        var beliefs = BotRiverDecisionsTest.learnedResponses(BettingStreet.FLOP, publicReplies);
        assertEquals(20, beliefs.getContinuationEstimate(BettingStreet.FLOP).getOpportunities());
        assertEquals(0, beliefs.getContinuationEstimate(BettingStreet.FLOP).getSuccesses());
        var plan = plan(turn(2, 0, STACK_BEHIND), Outcome.GUARANTEED_WIN,
                Map.of(OPPONENT, beliefs));
        assertEquals(ActionType.CHECK, plan.getAction().getType());
        assertEquals(46.546_341_463, plan.getProjectedNetChips(), CHIP_EPSILON);
    }

    @Test
    void aCappedWinningCallCostsOnlyTheRemainingStack() {
        BotTurn turn = turn(2, 80, 10);
        assertTrue(turn.getLegalActions().getWager().isEmpty());
        assertEquals(10, turn.getLegalActions().getCallCost());
        var win = plan(turn, Outcome.GUARANTEED_WIN, Map.of());
        assertEquals(ActionType.CALL, win.getAction().getType());
        assertEquals(50, win.getProjectedNetChips());
        var loss = plan(turn, Outcome.GUARANTEED_LOSS, Map.of());
        assertEquals(ActionType.FOLD, loss.getAction().getType());
        assertEquals(0, loss.getProjectedNetChips());
    }

    @Test
    void aShortValueBetHasNoFutureOrRaiseContributionWhenTheBotIsAllIn() {
        var plan = plan(turn(2, 0, 19), Outcome.GUARANTEED_WIN, Map.of());
        assertEquals(ActionType.BET, plan.getAction().getType());
        assertEquals(19, plan.getAction().getTargetAmount().orElseThrow());
        assertEquals(46.073_925_513, plan.getProjectedNetChips(), CHIP_EPSILON);
    }

    @ParameterizedTest
    @CsvSource({"19,FOLD", "20,CALL", "21,CALL"})
    void aSuppliedNonLossFloorMustCoverTheCallBeforeThePolicyContinues(long floor,
            ActionType expected) {
        BotTurn turn = turn(2, 20, STACK_BEHIND);
        var plan = planForFloor(turn, Outcome.GUARANTEED_NON_LOSS, floor, Map.of());
        assertEquals(expected, plan.getAction().getType());
    }

    @Test
    void facingABetTheBluffCreditsTheRefundAdjustedPrizeAndCapsTheRaise() {
        var beliefs = BotRiverDecisionsTest.learnedResponse(BettingStreet.FLOP, 8,
                ResolvedActionType.FOLD);
        var plan = plan(turn(2, 20, STACK_BEHIND), Outcome.GUARANTEED_LOSS,
                Map.of(OPPONENT, beliefs));
        assertEquals(ActionType.RAISE, plan.getAction().getType());
        assertEquals(80, plan.getAction().getTargetAmount().orElseThrow());
        assertEquals(55.456_391_577, plan.getProjectedNetChips(), CHIP_EPSILON);
    }

    @Test
    void aBluffNeedsAHeadsUpOpponentWithMoreFoldsThanContinuations() {
        var folds = BotRiverDecisionsTest.learnedResponse(BettingStreet.FLOP, 8,
                ResolvedActionType.FOLD);
        var multiway = plan(turn(3, 0, STACK_BEHIND), Outcome.GUARANTEED_LOSS,
                Map.of(OPPONENT, folds));
        assertEquals(ActionType.CHECK, multiway.getAction().getType());
        var equallyLikely = BotRiverDecisionsTest.learnedResponses(BettingStreet.FLOP, List.of(
                ResolvedActionType.CALL, ResolvedActionType.FOLD, ResolvedActionType.CALL,
                ResolvedActionType.FOLD, ResolvedActionType.CALL, ResolvedActionType.FOLD,
                ResolvedActionType.CALL, ResolvedActionType.FOLD));
        var even = plan(turn(2, 0, STACK_BEHIND), Outcome.GUARANTEED_LOSS,
                Map.of(OPPONENT, equallyLikely));
        assertEquals(ActionType.CHECK, even.getAction().getType());
    }

    private Plan plan(BotTurn turn, Outcome outcome, Map<String, OpponentBeliefs> beliefs) {
        long floor = KnownOutcomePolicy.minimumPayout(turn.getScenario(), outcome);
        return planForFloor(turn, outcome, floor, beliefs);
    }

    private Plan planForFloor(BotTurn turn, Outcome outcome, long floor,
            Map<String, OpponentBeliefs> beliefs) {
        var scenario = turn.getScenario();
        var call = new CallValue(floor / (double) scenario.getEligiblePotAmount(),
                scenario.getEligiblePotAmount(), turn.getCallRefund());
        var request = new PolicyRequest(turn.getView(), turn.getLegalActions(), call, beliefs);
        return policy.choose(request, outcome, floor);
    }

    private static BotTurn turn(int players, long bet, long stack) {
        return BotRiverDecisionsTest.capture(BotRiverDecisionsTest.playingState(players, bet, stack,
                BotRiverDecisionsTest.ROYAL_FLOP, BotRiverDecisionsTest.ROYAL_HOLES, 42));
    }
}
