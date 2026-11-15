package bot.learning;

import domain.model.BettingStreet;
import domain.model.ResolvedActionType;
import org.junit.jupiter.api.Test;
import static bot.learning.LearningFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AggressionEstimateTest {
    @Test
    void ae01UnseenStreetHasOnlyThePrior() {
        TendencyEstimate estimate = new OpponentBeliefs()
                .getAggressionEstimate(BettingStreet.FLOP);
        assertEquals(0, estimate.getSuccesses());
        assertEquals(0, estimate.getOpportunities());
        assertEquals(0.5, estimate.getProbability());
        assertEquals(0, estimate.getEvidenceWeight());
    }

    @Test
    void ae02CountsOnlyDecisionsWithAnAggressiveChoiceOnThatStreet() {
        OpponentModel model = new OpponentModel("match", "hero", java.util.Set.of("p1"));
        model.observe(decision(1, "p1", BettingStreet.FLOP, ResolvedActionType.BET, NONE,
                ResolvedActionType.BET, ResolvedActionType.CHECK));
        model.observe(decision(2, "p1", BettingStreet.FLOP, ResolvedActionType.CHECK, NONE,
                ResolvedActionType.BET, ResolvedActionType.CHECK));
        model.observe(decision(3, "p1", BettingStreet.FLOP, ResolvedActionType.RAISE, NONE,
                ResolvedActionType.RAISE, ResolvedActionType.CALL, ResolvedActionType.FOLD));
        model.observe(decision(4, "p1", BettingStreet.FLOP, ResolvedActionType.CALL, NONE,
                ResolvedActionType.CALL, ResolvedActionType.FOLD));
        model.observe(decision(5, "p1", BettingStreet.TURN, ResolvedActionType.BET, NONE,
                ResolvedActionType.BET, ResolvedActionType.CHECK));
        OpponentBeliefs before = model.snapshot("p1");
        TendencyEstimate flop = before.getAggressionEstimate(BettingStreet.FLOP);
        assertEquals(2, flop.getSuccesses());
        assertEquals(3, flop.getOpportunities());
        assertEquals(0.6, flop.getProbability());
        assertEquals(1, before.getAggressionEstimate(BettingStreet.TURN).getSuccesses());
        assertEquals(0, before.getAggressionEstimate(BettingStreet.RIVER).getOpportunities());
        model.observe(decision(6, "p1", BettingStreet.FLOP, ResolvedActionType.FOLD, NONE,
                ResolvedActionType.RAISE, ResolvedActionType.CALL, ResolvedActionType.FOLD));
        assertEquals(3, flop.getOpportunities());
        assertEquals(3, before.getAggressionEstimate(BettingStreet.FLOP).getOpportunities());
        assertEquals(4, model.snapshot("p1").getAggressionEstimate(BettingStreet.FLOP)
                .getOpportunities());
    }
}
