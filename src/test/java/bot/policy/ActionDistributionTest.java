package bot.policy;

import domain.model.ActionType;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ActionDistributionTest {
    @Test
    void ad01NormalizesWeightsAndUsesHalfOpenIntervals() {
        ActionDistribution distribution = new ActionDistribution("bot",
                Map.of(ActionType.FOLD, 2.0, ActionType.CALL, 3.0, ActionType.RAISE, 5.0),
                OptionalLong.of(60));
        assertEquals(Map.of(ActionType.FOLD, 0.2, ActionType.CALL, 0.3, ActionType.RAISE, 0.5),
                distribution.getProbabilities());
        assertEquals("bot", distribution.sample(0).getPlayerId());
        assertEquals(ActionType.FOLD, distribution.sample(Math.nextDown(0.2)).getType());
        assertEquals(ActionType.CALL, distribution.sample(0.2).getType());
        assertEquals(ActionType.RAISE, distribution.sample(0.5).getType());
        assertEquals(60, distribution.sample(Math.nextDown(1.0)).getTargetAmount().orElseThrow());
        assertTrue(distribution.sample(0).getTargetAmount().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> distribution.getProbabilities().clear());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.000001, 1, Double.NaN,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void ad02RejectsOutOfRangeQuantiles(double draw) {
        assertThrows(IllegalArgumentException.class, () -> check().sample(draw));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, 0, Double.NaN,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void ad03RejectsInvalidWeights(double weight) {
        assertThrows(IllegalArgumentException.class, () -> new ActionDistribution("bot",
                Map.of(ActionType.CHECK, weight), OptionalLong.empty()));
    }

    @Test
    void ad04RejectsInvalidIdentityEmptyOrUnrepresentableWeights() {
        assertThrows(IllegalArgumentException.class, () -> new ActionDistribution(" ",
                Map.of(ActionType.CHECK, 1.0), OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class, () -> new ActionDistribution("bot",
                Map.of(), OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class, () -> new ActionDistribution("bot",
                Map.of(ActionType.CHECK, Double.MAX_VALUE, ActionType.FOLD, Double.MAX_VALUE),
                OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class, () -> new ActionDistribution("bot",
                Map.of(ActionType.CHECK, Double.MIN_VALUE, ActionType.FOLD, Double.MAX_VALUE),
                OptionalLong.empty()));
    }

    @Test
    void ad05RequiresOnePositiveWagerTargetOnlyForAggression() {
        assertThrows(IllegalArgumentException.class, () -> new ActionDistribution("bot",
                Map.of(ActionType.RAISE, 1.0), OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class, () -> new ActionDistribution("bot",
                Map.of(ActionType.CHECK, 1.0), OptionalLong.of(20)));
        for (long target : new long[] {-1, 0}) {
            assertThrows(IllegalArgumentException.class, () -> new ActionDistribution("bot",
                    Map.of(ActionType.BET, 1.0), OptionalLong.of(target)));
        }
        assertThrows(IllegalArgumentException.class, () -> new ActionDistribution("bot",
                Map.of(ActionType.BET, 1.0, ActionType.RAISE, 1.0), OptionalLong.of(20)));
        ActionDistribution bet = new ActionDistribution("bot",
                Map.of(ActionType.BET, 1.0), OptionalLong.of(1));
        assertEquals(1, bet.sample(0).getTargetAmount().orElseThrow());
        assertEquals(ActionType.CHECK, check().sample(Math.nextDown(1.0)).getType());
    }

    @Test
    void ad06DetachesCallerWeightsAtTheOwnershipBoundary() {
        Map<ActionType, Double> weights = new java.util.EnumMap<>(ActionType.class);
        weights.put(ActionType.CHECK, 1.0);
        ActionDistribution distribution = new ActionDistribution("bot", weights,
                OptionalLong.empty());
        weights.clear();
        assertEquals(Map.of(ActionType.CHECK, 1.0), distribution.getProbabilities());
        assertEquals(ActionType.CHECK, distribution.sample(0).getType());
    }

    private static ActionDistribution check() {
        return new ActionDistribution("bot", Map.of(ActionType.CHECK, 1.0), OptionalLong.empty());
    }
}
