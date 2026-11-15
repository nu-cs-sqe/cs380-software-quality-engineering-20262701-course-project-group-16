package bot.policy;

import bot.learning.TendencyEstimate;
import domain.model.ActionType;
import domain.model.LegalActions;
import domain.model.PlayerView;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalLong;

/** Stateless bounded heuristic over authoritative options; not a GTO or profit guarantee. */
public final class BotPolicy {
    private static final double MIXING_MARGIN = 0.05;
    private static final double POSITION_CALL_BONUS = 0.01;
    private static final double POSITION_VALUE_BONUS = 0.04;
    private static final double OPPONENT_PRESSURE = 0.06;
    private static final double CALL_COMMITMENT = 0.02;
    private static final double VALUE_MARGIN = 0.08;
    private static final double AGGRESSION_WEIGHT = 0.5;
    private static final double WAGER_COMMITMENT = 0.4;

    public ActionDistribution distribution(PolicyRequest request, BotPersonality personality) {
        double pressure = opponentPressure(request);
        Map<ActionType, Double> weights = passiveWeights(request, personality, pressure);
        OptionalLong target = addWager(request, personality, pressure, weights);
        return new ActionDistribution(request.getHero().getId(), weights, target);
    }

    private static Map<ActionType, Double> passiveWeights(PolicyRequest request,
            BotPersonality personality, double pressure) {
        Map<ActionType, Double> weights = new EnumMap<>(ActionType.class);
        long gross = request.getLegalActions().getCallCost();
        if (gross == 0) {
            weights.put(ActionType.CHECK, 1.0);
        } else if (request.getCallValue().riskedChips(gross) == 0) {
            weights.put(ActionType.CALL, 1.0);
        } else {
            payingChoices(request, personality, pressure, weights);
        }
        return weights;
    }

    private static void payingChoices(PolicyRequest request, BotPersonality personality,
            double pressure, Map<ActionType, Double> weights) {
        double equity = request.getCallValue().getEquity();
        if (equity == 0 || equity == 1) {
            weights.put(equity == 0 ? ActionType.FOLD : ActionType.CALL, 1.0);
        } else {
            double edge = callEdge(request, personality, pressure);
            addPositive(weights, ActionType.CALL, edge + MIXING_MARGIN);
            addPositive(weights, ActionType.FOLD, MIXING_MARGIN - edge);
        }
    }

    private static double callEdge(PolicyRequest request, BotPersonality personality,
            double pressure) {
        long cost = request.getLegalActions().getCallCost();
        double commitment = request.getCallValue().riskedChips(cost)
                / (double) request.getHero().getChipStack();
        return request.getCallValue().getEquity() - odds(request) + personality.equityBias()
                + (request.isInPosition() ? POSITION_CALL_BONUS : 0)
                - OPPONENT_PRESSURE * pressure - CALL_COMMITMENT * commitment;
    }

    private static double odds(PolicyRequest request) {
        return request.getCallValue().breakEvenEquity(
                request.getLegalActions().getCallCost()).orElse(0);
    }

    private static double opponentPressure(PolicyRequest request) {
        return request.getActiveOpponents().stream()
                .mapToDouble(opponent -> pressureFor(request, opponent)).average().orElse(0);
    }

    private static double pressureFor(PolicyRequest request, PlayerView opponent) {
        if (!request.getOpponents().containsKey(opponent.getId())) {
            return 0;
        }
        TendencyEstimate estimate = request.getOpponents().get(opponent.getId())
                .getAggressionEstimate(request.getStreet());
        return (estimate.getProbability() - 0.5) * estimate.getEvidenceWeight();
    }

    private static OptionalLong addWager(PolicyRequest request, BotPersonality personality,
            double pressure, Map<ActionType, Double> weights) {
        if (request.getLegalActions().getWager().isEmpty()
                || request.getCallValue().getEquity() < odds(request)) {
            return OptionalLong.empty();
        }
        LegalActions.Wager limits = request.getLegalActions().getWager().orElseThrow();
        long target = wagerTarget(request, personality, limits);
        double weight = wagerWeight(request, personality, pressure, target);
        addPositive(weights, limits.getType(), weight);
        return weight > 0 ? OptionalLong.of(target) : OptionalLong.empty();
    }

    private static double wagerWeight(PolicyRequest request, BotPersonality personality,
            double pressure, long target) {
        double strength = request.getCallValue().getEquity()
                - 1.0 / request.getParticipantCount() - VALUE_MARGIN + personality.equityBias()
                + (request.isInPosition() ? POSITION_VALUE_BONUS : 0)
                - OPPONENT_PRESSURE * pressure;
        double commitment = (target - request.getHero().getRoundContribution())
                / (double) request.getHero().getChipStack();
        return Math.max(0, strength) * AGGRESSION_WEIGHT * personality.aggressionScale()
                * (1 - WAGER_COMMITMENT * commitment);
    }

    private static long wagerTarget(PolicyRequest request, BotPersonality personality,
            LegalActions.Wager limits) {
        if (limits.getShortAllInTarget().isPresent()) {
            return limits.getShortAllInTarget().getAsLong();
        }
        long base = request.getHero().getRoundContribution()
                + request.getLegalActions().getCallCost();
        long room = limits.getMaximumTarget() - base;
        long desired = Math.round(request.getCallValue().getEligiblePotAfterCall()
                * personality.potFraction());
        long target = base + Math.min(room, desired);
        return Math.max(limits.getMinimumFullTarget().orElseThrow(), target);
    }

    private static void addPositive(Map<ActionType, Double> weights, ActionType type,
            double weight) {
        if (weight > 0) {
            weights.put(type, weight);
        }
    }
}
