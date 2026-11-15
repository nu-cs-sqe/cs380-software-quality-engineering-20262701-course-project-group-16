package bot.learning;

import domain.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static bot.learning.LearningFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class OpponentLearningEvaluationTest {
    private static final ResolvedActionType[] CHOICES = {ResolvedActionType.FOLD, ResolvedActionType.CALL, ResolvedActionType.RAISE};
    private static final DecisionContext CONTEXT = context(BettingStreet.TURN, false, CHOICES);

    @ParameterizedTest
    @ValueSource(longs = {7, 23, 91, 131})
    void leC01ImprovesHeldOutBrierScoreOverLearningDisabled(long seed) {
        OpponentModel model = new OpponentModel("match", "hero", Set.of("p1"));
        List<ResolvedActionType> training = choices(80, 10, 10);
        Collections.shuffle(training, new Random(seed));
        long sequence = train(model, training, 0);
        ActionPrediction learned = model.snapshot("p1").getPrediction(CONTEXT);
        ActionPrediction disabled = new OpponentBeliefs().getPrediction(CONTEXT);
        List<ResolvedActionType> heldOut = choices(70, 20, 10);
        Collections.shuffle(heldOut, new Random(seed + 1));
        double learnedScore = brier(learned.getProbabilities(), heldOut);
        double disabledScore = brier(disabled.getProbabilities(), heldOut);
        System.out.printf(Locale.ROOT, "opponent seed=%d training=%d heldOut=%d learnedBrier=%.9f disabledBrier=%.9f foldProbability=%.9f evidence=%.9f%n",
                seed, sequence, heldOut.size(), learnedScore, disabledScore,
                learned.getProbabilities().get(ResolvedActionType.FOLD), learned.getEvidenceWeight());
        assertTrue(learnedScore < disabledScore - 0.1);
        assertEquals(2.0 / 3, disabledScore, 1e-12);
        assertEquals(81.0 / 103, learned.getProbabilities().get(ResolvedActionType.FOLD));
        assertTrue(learned.getStandardDeviations().get(ResolvedActionType.FOLD)
                < disabled.getStandardDeviations().get(ResolvedActionType.FOLD));
        assertEquals(100, model.snapshot("p1").getObservedDecisions());
    }
    @Test
    void leC02NewEvidenceChangesBeliefsWithoutLeakingIntoUnseenContexts() {
        OpponentModel model = new OpponentModel("match", "hero", Set.of("p1"));
        long sequence = train(model, choices(80, 10, 10), 0);
        ActionPrediction old = model.snapshot("p1").getPrediction(CONTEXT);
        train(model, choices(10, 10, 80), sequence);
        ActionPrediction after = model.snapshot("p1").getPrediction(CONTEXT);
        assertEquals(11.0 / 103, old.getProbabilities().get(ResolvedActionType.RAISE));
        assertEquals(91.0 / 203, after.getProbabilities().get(ResolvedActionType.RAISE));
        assertTrue(after.getProbabilities().get(ResolvedActionType.RAISE) > old.getProbabilities().get(ResolvedActionType.RAISE));
        assertEquals(200, model.snapshot("p1").getObservedDecisions());
        ActionPrediction unseen = model.snapshot("p1").getPrediction(context(BettingStreet.RIVER, false, CHOICES));
        assertEquals(0, unseen.getObservations());
        assertEquals(0, unseen.getEvidenceWeight());
        assertEquals(1.0 / 3, unseen.getProbabilities().get(ResolvedActionType.RAISE));
    }
    private static long train(OpponentModel model, List<ResolvedActionType> actions, long sequence) {
        for (ResolvedActionType type : actions) {
            assertTrue(model.observe(decision(++sequence, "p1", BettingStreet.TURN, type, NONE, CHOICES)));
        }
        return sequence;
    }
    private static List<ResolvedActionType> choices(int folds, int calls, int raises) {
        List<ResolvedActionType> result = new ArrayList<>();
        result.addAll(Collections.nCopies(folds, ResolvedActionType.FOLD));
        result.addAll(Collections.nCopies(calls, ResolvedActionType.CALL));
        result.addAll(Collections.nCopies(raises, ResolvedActionType.RAISE));
        return result;
    }
    private static double brier(Map<ResolvedActionType, Double> probabilities, List<ResolvedActionType> outcomes) {
        double total = 0;
        for (ResolvedActionType outcome : outcomes) {
            for (ResolvedActionType action : CHOICES) {
                double error = probabilities.get(action) - (action == outcome ? 1 : 0);
                total += error * error;
            }
        }
        return total / outcomes.size();
    }
}
