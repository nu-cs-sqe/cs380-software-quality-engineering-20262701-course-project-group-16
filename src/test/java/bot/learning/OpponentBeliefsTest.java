package bot.learning;

import domain.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static bot.learning.LearningFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class OpponentBeliefsTest {
    @Test
    void obC01StartsWithPriorsWithoutFabricatingObservations() {
        OpponentBeliefs beliefs = new OpponentBeliefs();
        assertEquals(0, beliefs.getObservedDecisions());
        for (OpponentTendency tendency : OpponentTendency.values()) {
            TendencyEstimate estimate = beliefs.getTendency(tendency);
            assertEquals(0, estimate.getOpportunities());
            assertEquals(0, estimate.getSuccesses());
            assertEquals(0.5, estimate.getProbability());
            assertEquals(0, estimate.getEvidenceWeight());
        }
        ActionPrediction prediction = beliefs.getPrediction(context(BettingStreet.RIVER, true,
                ResolvedActionType.FOLD, ResolvedActionType.CALL));
        assertEquals(Map.of(ResolvedActionType.FOLD, 0.5, ResolvedActionType.CALL, 0.5), prediction.getProbabilities());
        assertEquals(0, beliefs.getObservedDecisions());
        assertThrows(IllegalArgumentException.class, () -> beliefs.getPrediction(null));
        assertThrows(IllegalArgumentException.class, () -> beliefs.getTendency(null));
    }
    @Test
    void obC02UpdatesChoicesWithoutChangingPreviouslyPublishedBeliefs() {
        OpponentBeliefs before = new OpponentBeliefs();
        OpponentBeliefs after = before.withDecision(decision(1, "p1", BettingStreet.PREFLOP,
                ResolvedActionType.CALL, NONE, ResolvedActionType.FOLD, ResolvedActionType.CALL, ResolvedActionType.RAISE))
                .withDecision(decision(2, "p1", BettingStreet.PREFLOP, ResolvedActionType.RAISE,
                        new StatsContext(PreflopRaiseType.OPENING_RAISE, false, false, false),
                        ResolvedActionType.FOLD, ResolvedActionType.CALL, ResolvedActionType.RAISE));
        ActionPrediction prediction = after.getPrediction(context(BettingStreet.PREFLOP, false,
                ResolvedActionType.RAISE, ResolvedActionType.CALL, ResolvedActionType.FOLD));
        assertEquals(2, after.getObservedDecisions());
        assertEquals(2, prediction.getObservations());
        assertEquals(Map.of(ResolvedActionType.FOLD, 0.2, ResolvedActionType.CALL, 0.4, ResolvedActionType.RAISE, 0.4), prediction.getProbabilities());
        assertEquals(0, before.getObservedDecisions());
        assertEquals(0, before.getPrediction(prediction.getContext()).getObservations());
    }
    @Test
    void obC03KeepsStreetLegalChoicesAndPositionContextsSeparate() {
        OpponentBeliefs beliefs = new OpponentBeliefs();
        List<DecisionContext> contexts = List.of(context(BettingStreet.FLOP, false, ResolvedActionType.CHECK, ResolvedActionType.BET),
                context(BettingStreet.TURN, false, ResolvedActionType.CHECK, ResolvedActionType.BET),
                context(BettingStreet.FLOP, true, ResolvedActionType.CHECK, ResolvedActionType.BET),
                context(BettingStreet.FLOP, false, ResolvedActionType.CHECK));
        int sequence = 0;
        for (DecisionContext context : contexts) {
            beliefs = beliefs.withDecision(new PublicDecision("match", ++sequence,
                    action("p1", context.getStreet(), ResolvedActionType.CHECK, NONE), context, DecisionOrigin.PLAYER_CHOICE));
        }
        assertEquals(4, beliefs.getObservedDecisions());
        for (DecisionContext context : contexts) assertEquals(1, beliefs.getPrediction(context).getObservations());
        assertEquals(0, beliefs.getPrediction(context(BettingStreet.RIVER, false, ResolvedActionType.CHECK)).getObservations());
    }
    @Test
    void obC04CountsThreeBetSuccessesAndDeclinedEligibleDecisions() {
        OpponentBeliefs beliefs = new OpponentBeliefs();
        long sequence = 0;
        for (ResolvedActionType type : List.of(ResolvedActionType.CALL, ResolvedActionType.FOLD, ResolvedActionType.RAISE)) {
            StatsContext stats = new StatsContext(type == ResolvedActionType.RAISE ? PreflopRaiseType.THREE_BET : PreflopRaiseType.NONE,
                    true, false, false);
            beliefs = beliefs.withDecision(decision(++sequence, "p1", BettingStreet.PREFLOP, type, stats,
                    ResolvedActionType.CALL, ResolvedActionType.FOLD, ResolvedActionType.RAISE));
        }
        for (PreflopRaiseType type : List.of(PreflopRaiseType.OPENING_RAISE, PreflopRaiseType.FOUR_BET, PreflopRaiseType.LATER_RAISE)) {
            beliefs = beliefs.withDecision(decision(++sequence, "p1", BettingStreet.PREFLOP, ResolvedActionType.RAISE,
                    new StatsContext(type, false, false, false), ResolvedActionType.RAISE));
        }
        assertTendency(beliefs, OpponentTendency.THREE_BET, 1, 3);
        assertEquals(6, beliefs.getObservedDecisions());
        assertTendency(beliefs, OpponentTendency.FLOP_CBET, 0, 0);
    }
    @Test
    void obC05CountsFlopCBetOpportunitiesOnlyWhenSuppliedByEngine() {
        OpponentBeliefs beliefs = new OpponentBeliefs();
        long sequence = 0;
        for (ResolvedActionType type : List.of(ResolvedActionType.CHECK, ResolvedActionType.BET, ResolvedActionType.FOLD)) {
            beliefs = beliefs.withDecision(decision(++sequence, "p1", BettingStreet.FLOP, type,
                    new StatsContext(PreflopRaiseType.NONE, false, true, false),
                    ResolvedActionType.CHECK, ResolvedActionType.BET, ResolvedActionType.FOLD));
        }
        beliefs = beliefs.withDecision(decision(++sequence, "p1", BettingStreet.FLOP,
                ResolvedActionType.BET, NONE, ResolvedActionType.BET));
        beliefs = beliefs.withDecision(decision(sequence + 1, "p1", BettingStreet.TURN,
                ResolvedActionType.BET, NONE, ResolvedActionType.BET));
        assertTendency(beliefs, OpponentTendency.FLOP_CBET, 1, 3);
        assertTendency(beliefs, OpponentTendency.FOLD_TO_FLOP_CBET, 0, 0);
        assertEquals(5, beliefs.getObservedDecisions());
    }
    @Test
    void obC06CountsActualFacedCBetResponsesIncludingCallsAndRaises() {
        OpponentBeliefs beliefs = new OpponentBeliefs();
        long sequence = 0;
        for (ResolvedActionType type : List.of(ResolvedActionType.CALL, ResolvedActionType.RAISE, ResolvedActionType.FOLD)) {
            beliefs = beliefs.withDecision(decision(++sequence, "p1", BettingStreet.FLOP, type,
                    new StatsContext(PreflopRaiseType.NONE, false, false, true),
                    ResolvedActionType.CALL, ResolvedActionType.RAISE, ResolvedActionType.FOLD));
        }
        beliefs = beliefs.withDecision(decision(sequence + 1, "p1", BettingStreet.FLOP, ResolvedActionType.FOLD,
                NONE, ResolvedActionType.CALL, ResolvedActionType.FOLD));
        assertTendency(beliefs, OpponentTendency.FOLD_TO_FLOP_CBET, 1, 3);
        assertTendency(beliefs, OpponentTendency.FLOP_CBET, 0, 0);
    }
    @Test
    void obC07AllInCallStaysACallWithoutInventingAThreeBetOpportunity() {
        AcceptedAction allIn = new AcceptedAction("hand", "p1", BettingStreet.PREFLOP, ResolvedActionType.CALL,
                20, 20, true, NONE);
        DecisionContext context = context(BettingStreet.PREFLOP, false, ResolvedActionType.CALL, ResolvedActionType.FOLD);
        OpponentBeliefs beliefs = new OpponentBeliefs().withDecision(new PublicDecision("match", 1,
                allIn, context, DecisionOrigin.PLAYER_CHOICE));
        assertTendency(beliefs, OpponentTendency.THREE_BET, 0, 0);
        assertEquals(Map.of(ResolvedActionType.FOLD, 1.0 / 3, ResolvedActionType.CALL, 2.0 / 3),
                beliefs.getPrediction(context).getProbabilities());
    }
    @Test
    void obC08RejectsMissingPredictionContextAfterLearningToo() {
        OpponentBeliefs beliefs = new OpponentBeliefs().withDecision(decision(1, "p1", BettingStreet.TURN,
                ResolvedActionType.CHECK, NONE, ResolvedActionType.CHECK));
        assertThrows(IllegalArgumentException.class, () -> beliefs.getPrediction(null));
        assertEquals(1, beliefs.getObservedDecisions());
    }
    private static void assertTendency(OpponentBeliefs beliefs, OpponentTendency tendency, long successes, long opportunities) {
        TendencyEstimate estimate = beliefs.getTendency(tendency);
        assertEquals(successes, estimate.getSuccesses());
        assertEquals(opportunities, estimate.getOpportunities());
        assertEquals((successes + 1.0) / (opportunities + 2.0), estimate.getProbability());
    }
}
