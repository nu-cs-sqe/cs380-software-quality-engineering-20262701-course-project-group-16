package bot.learning;

import domain.model.ResolvedActionType;
import java.util.EnumMap;
import java.util.Map;

/** Immutable add-one categorical prediction over the supplied legal choices. */
public final class ActionPrediction {
    private final DecisionContext context;
    private final Map<ResolvedActionType, Long> counts;
    private final long observations;

    ActionPrediction(DecisionContext context) {
        this(context, emptyCounts(context), 0);
    }

    private ActionPrediction(DecisionContext context, Map<ResolvedActionType, Long> counts,
            long observations) {
        this.context = context;
        this.counts = Map.copyOf(counts);
        this.observations = observations;
    }

    private static Map<ResolvedActionType, Long> emptyCounts(DecisionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("A public decision context is required.");
        }
        Map<ResolvedActionType, Long> empty = new EnumMap<>(ResolvedActionType.class);
        context.getLegalActions().forEach(action -> empty.put(action, 0L));
        return empty;
    }

    ActionPrediction withAction(ResolvedActionType action) {
        if (action == null || !counts.containsKey(action)) {
            throw new IllegalArgumentException("Only a supplied legal choice can be learned.");
        }
        Map<ResolvedActionType, Long> next = new EnumMap<>(counts);
        next.put(action, Math.incrementExact(counts.get(action)));
        return new ActionPrediction(context, next, Math.incrementExact(observations));
    }

    public DecisionContext getContext() { return context; }
    public Map<ResolvedActionType, Long> getCounts() { return counts; }
    public long getObservations() { return observations; }

    public Map<ResolvedActionType, Double> getProbabilities() {
        Map<ResolvedActionType, Double> result = new EnumMap<>(ResolvedActionType.class);
        counts.forEach((action, count) -> result.put(action,
                (count + 1.0) / (observations + (double) counts.size())));
        return Map.copyOf(result);
    }

    /** Marginal posterior uncertainty, not a frequentist confidence interval. */
    public Map<ResolvedActionType, Double> getStandardDeviations() {
        Map<ResolvedActionType, Double> result = new EnumMap<>(ResolvedActionType.class);
        getProbabilities().forEach((action, probability) -> result.put(action,
                Math.sqrt(probability * (1 - probability)
                / (observations + (double) counts.size() + 1.0))));
        return Map.copyOf(result);
    }

    public double getEvidenceWeight() {
        return observations / (observations + (double) counts.size());
    }
}
