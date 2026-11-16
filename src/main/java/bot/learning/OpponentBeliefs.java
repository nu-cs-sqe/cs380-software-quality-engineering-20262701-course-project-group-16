package bot.learning;

import domain.model.ResolvedActionType;
import domain.model.PreflopRaiseType;
import domain.model.StatsContext;
import domain.model.BettingStreet;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable per-opponent predictions and eligible-decision tendencies. */
public final class OpponentBeliefs {
    private final List<ActionPrediction> predictions;
    private final Map<OpponentTendency, TendencyEstimate> tendencies;
    private final long observedDecisions;

    public OpponentBeliefs() {
        this(List.of(), emptyTendencies(), 0);
    }

    private OpponentBeliefs(List<ActionPrediction> predictions,
            Map<OpponentTendency, TendencyEstimate> tendencies, long observedDecisions) {
        this.predictions = List.copyOf(predictions);
        this.tendencies = Map.copyOf(tendencies);
        this.observedDecisions = observedDecisions;
    }

    private static Map<OpponentTendency, TendencyEstimate> emptyTendencies() {
        Map<OpponentTendency, TendencyEstimate> result = new EnumMap<>(OpponentTendency.class);
        for (OpponentTendency tendency : OpponentTendency.values()) {
            result.put(tendency, new TendencyEstimate(0, 0));
        }
        return result;
    }

    // Called only after the model has filtered nonstrategic and replayed events.
    OpponentBeliefs withDecision(PublicDecision decision) {
        DecisionContext context = decision.getContext().orElseThrow();
        ActionPrediction next = getPrediction(context).withAction(decision.getAction().getType());
        List<ActionPrediction> updated = new ArrayList<>(predictions);
        updated.removeIf(prediction -> prediction.getContext().matches(context));
        updated.add(next);
        Map<OpponentTendency, TendencyEstimate> nextTendencies = updatedTendencies(decision);
        return new OpponentBeliefs(updated, nextTendencies, Math.incrementExact(observedDecisions));
    }

    private Map<OpponentTendency, TendencyEstimate> updatedTendencies(PublicDecision decision) {
        Map<OpponentTendency, TendencyEstimate> next = new EnumMap<>(tendencies);
        StatsContext context = decision.getAction().getStatsContext();
        ResolvedActionType type = decision.getAction().getType();
        recordOpportunity(next, OpponentTendency.THREE_BET, context.hasThreeBetOpportunity(),
                context.getPreflopRaiseType() == PreflopRaiseType.THREE_BET);
        recordOpportunity(next, OpponentTendency.FLOP_CBET, context.hasFlopCBetOpportunity(),
                type == ResolvedActionType.BET);
        recordOpportunity(next, OpponentTendency.FOLD_TO_FLOP_CBET,
                context.hasFacedFlopCBetOpportunity(), type == ResolvedActionType.FOLD);
        return next;
    }

    private static void recordOpportunity(Map<OpponentTendency, TendencyEstimate> next,
            OpponentTendency tendency, boolean eligible, boolean success) {
        if (eligible) {
            next.put(tendency, next.get(tendency).withOutcome(success));
        }
    }

    public ActionPrediction getPrediction(DecisionContext context) {
        return predictions.stream().filter(prediction -> prediction.getContext().matches(context))
                .findFirst().orElseGet(() -> new ActionPrediction(context));
    }

    public TendencyEstimate getTendency(OpponentTendency tendency) {
        return tendencies.get(tendency);
    }

    /** Beta-smoothed aggression over decisions where a bet or raise was legal. */
    public TendencyEstimate getAggressionEstimate(BettingStreet street) {
        long successes = 0;
        long opportunities = 0;
        for (ActionPrediction prediction : predictions) {
            if (eligibleAggression(prediction.getContext(), street)) {
                successes = Math.addExact(successes, aggressionCount(prediction));
                opportunities = Math.addExact(opportunities, prediction.getObservations());
            }
        }
        return new TendencyEstimate(successes, opportunities);
    }

    private static boolean eligibleAggression(DecisionContext context, BettingStreet street) {
        return context.getStreet() == street
                && (context.getLegalActions().contains(ResolvedActionType.BET)
                || context.getLegalActions().contains(ResolvedActionType.RAISE));
    }

    private static long aggressionCount(ActionPrediction prediction) {
        return Math.addExact(prediction.getCounts().getOrDefault(ResolvedActionType.BET, 0L),
                prediction.getCounts().getOrDefault(ResolvedActionType.RAISE, 0L));
    }

    public long getObservedDecisions() { return observedDecisions; }
}
