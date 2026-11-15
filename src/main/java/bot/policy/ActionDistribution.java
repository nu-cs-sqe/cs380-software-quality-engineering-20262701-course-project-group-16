package bot.policy;

import domain.model.ActionRequest;
import domain.model.ActionType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/** Immutable normalized policy; sampling is pure and uses explicit enum order. */
public final class ActionDistribution {
    private final String playerId;
    private final Map<ActionType, Double> probabilities;
    private final List<ActionType> order;
    private final OptionalLong target;

    ActionDistribution(String playerId, Map<ActionType, Double> weights, OptionalLong target) {
        requireIdentityAndWeights(playerId, weights);
        requireTarget(weights, target);
        this.playerId = playerId;
        probabilities = Map.copyOf(normalize(weights));
        order = weights.keySet().stream().sorted().collect(Collectors.toUnmodifiableList());
        this.target = target;
    }

    private static void requireIdentityAndWeights(String id, Map<ActionType, Double> weights) {
        if (id.isBlank() || weights.isEmpty()
                || weights.values().stream().anyMatch(value -> !Double.isFinite(value)
                || value <= 0)) {
            throw new IllegalArgumentException(
                    "Use an identity and finite positive action weights.");
        }
    }

    private static void requireTarget(Map<ActionType, Double> weights, OptionalLong target) {
        boolean bet = weights.containsKey(ActionType.BET);
        boolean raise = weights.containsKey(ActionType.RAISE);
        if ((bet && raise) || (bet || raise) != target.isPresent()
                || (target.isPresent() && target.getAsLong() <= 0)) {
            throw new IllegalArgumentException("One positive target is required only for a wager.");
        }
    }

    private static Map<ActionType, Double> normalize(Map<ActionType, Double> weights) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<ActionType, Double> result = new EnumMap<>(ActionType.class);
        weights.forEach((type, weight) -> result.put(type, weight / total));
        if (result.values().stream().anyMatch(value -> value == 0)) {
            throw new IllegalArgumentException(
                    "Weights cannot be normalized to positive probabilities.");
        }
        return result;
    }

    public Map<ActionType, Double> getProbabilities() { return probabilities; }

    /** Uses half-open probability intervals; the last option absorbs floating-point residue. */
    public ActionRequest sample(double quantile) {
        if (!Double.isFinite(quantile) || quantile < 0 || quantile >= 1) {
            throw new IllegalArgumentException("Supply a finite random quantile in [0,1).");
        }
        ActionType selected = order.get(0);
        double remaining = quantile;
        for (ActionType type : order) {
            selected = type;
            remaining -= probabilities.get(type);
            if (remaining < 0) {
                break;
            }
        }
        return request(selected);
    }

    private ActionRequest request(ActionType type) {
        return type == ActionType.BET || type == ActionType.RAISE
                ? new ActionRequest(playerId, type, target.orElseThrow())
                : new ActionRequest(playerId, type);
    }
}
