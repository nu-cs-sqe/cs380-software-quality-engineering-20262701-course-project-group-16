package bot.learning;

import domain.model.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static bot.learning.LearningFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ActionPredictionTest {
    @Test
    void apC01StartsWithUniformLegalChoicePriorAndUncertainty() {
        DecisionContext context = context(BettingStreet.PREFLOP, false,
                ResolvedActionType.FOLD, ResolvedActionType.CALL, ResolvedActionType.RAISE);
        ActionPrediction prior = new ActionPrediction(context);
        assertSame(context, prior.getContext());
        assertEquals(0, prior.getObservations());
        assertEquals(0, prior.getEvidenceWeight());
        assertEquals(Map.of(ResolvedActionType.FOLD, 0L, ResolvedActionType.CALL, 0L, ResolvedActionType.RAISE, 0L), prior.getCounts());
        for (double probability : prior.getProbabilities().values()) assertEquals(1.0 / 3, probability);
        for (double deviation : prior.getStandardDeviations().values()) assertEquals(Math.sqrt(1.0 / 18), deviation, 1e-15);
        assertEquals(3, prior.getProbabilities().size());
        assertEquals(3, prior.getStandardDeviations().size());
        assertThrows(UnsupportedOperationException.class, () -> prior.getProbabilities().clear());
        assertThrows(UnsupportedOperationException.class, () -> prior.getCounts().clear());
        assertThrows(UnsupportedOperationException.class, () -> prior.getStandardDeviations().clear());
    }
    @Test
    void apC02CountsChoicesAndPreservesOldSnapshots() {
        ActionPrediction prior = new ActionPrediction(context(BettingStreet.PREFLOP, false,
                ResolvedActionType.FOLD, ResolvedActionType.CALL, ResolvedActionType.RAISE));
        ActionPrediction next = prior.withAction(ResolvedActionType.FOLD).withAction(ResolvedActionType.FOLD)
                .withAction(ResolvedActionType.FOLD).withAction(ResolvedActionType.CALL);
        assertEquals(4, next.getObservations());
        assertEquals(4.0 / 7, next.getEvidenceWeight());
        assertEquals(Map.of(ResolvedActionType.FOLD, 3L, ResolvedActionType.CALL, 1L, ResolvedActionType.RAISE, 0L), next.getCounts());
        assertEquals(Map.of(ResolvedActionType.FOLD, 4.0 / 7, ResolvedActionType.CALL, 2.0 / 7,
                ResolvedActionType.RAISE, 1.0 / 7), next.getProbabilities());
        for (ResolvedActionType action : next.getCounts().keySet()) {
            double p = next.getProbabilities().get(action);
            assertEquals(Math.sqrt(p * (1 - p) / 8), next.getStandardDeviations().get(action));
        }
        assertEquals(0, prior.getObservations());
        assertEquals(0L, prior.getCounts().get(ResolvedActionType.FOLD));
    }
    @Test
    void apC03SingleLegalActionIsCertainAndInvalidInputCannotChangeIt() {
        ActionPrediction only = new ActionPrediction(context(BettingStreet.RIVER, true, ResolvedActionType.CHECK));
        assertEquals(Map.of(ResolvedActionType.CHECK, 1.0), only.getProbabilities());
        assertEquals(Map.of(ResolvedActionType.CHECK, 0.0), only.getStandardDeviations());
        assertEquals(0.5, only.withAction(ResolvedActionType.CHECK).getEvidenceWeight());
        assertThrows(IllegalArgumentException.class, () -> new ActionPrediction(null));
        assertThrows(IllegalArgumentException.class, () -> assertNotNull(only.withAction(ResolvedActionType.CALL)));
        assertThrows(IllegalArgumentException.class, () -> assertNotNull(only.withAction(null)));
        assertEquals(0, only.getObservations());
    }
}
