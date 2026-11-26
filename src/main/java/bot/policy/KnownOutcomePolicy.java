package bot.policy;

import bot.EquityPot;
import bot.EquityRequest;
import bot.learning.OpponentBeliefs;
import bot.learning.TendencyEstimate;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingStreet;
import domain.model.LegalActions;
import domain.model.PlayerView;
import domain.model.RiverOutcomeEvaluator.Outcome;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Prices public betting opportunities separately from the proven showdown result. */
public final class KnownOutcomePolicy {
    private static final List<Double> POT_FRACTIONS = List.of(0.5, 0.75, 1.0);
    private static final OpponentBeliefs PRIOR = new OpponentBeliefs();
    private static final int HEADS_UP_PARTICIPANTS = 2;
    private static final long MIN_BLUFF_OBSERVATIONS = 8;
    private static final double EVEN_CONTINUATION_PROBABILITY = 0.5;
    private static final double PRICE_SENSITIVITY = 3.0;
    private static final double RAISE_SHARE = 0.25;
    private static final double FUTURE_STREET_DISCOUNT = 0.16;
    private static final double OUT_OF_POSITION_DISCOUNT = 0.75;
    private static final double CHECK_INDUCE_DISCOUNT = 0.20;

    public static long minimumPayout(EquityRequest request, Outcome outcome) {
        if (outcome == Outcome.UNCERTAIN) {
            throw new IllegalArgumentException("A payout floor needs a proven outcome.");
        }
        return request.getPots().stream()
                .filter(pot -> pot.getEligiblePlayers().contains(request.getHeroId()))
                .mapToLong(pot -> minimumShare(pot, outcome)).sum();
    }

    private static long minimumShare(EquityPot pot, Outcome outcome) {
        int claimants = pot.getEligiblePlayers().size();
        if (outcome == Outcome.GUARANTEED_WIN || claimants == 1) {
            return pot.getAmount();
        }
        return outcome == Outcome.GUARANTEED_LOSS ? 0 : pot.getAmount() / claimants;
    }

    /** Prices legal actions using the payout floor for the same outcome and call preview. */
    public Plan choose(PolicyRequest request, Outcome outcome, long award) {
        long risk = request.getCallValue().riskedChips(request.getLegalActions().getCallCost());
        Plan passive = passivePlan(request, award - risk, outcome);
        if (outcome == Outcome.GUARANTEED_WIN) {
            return valuePlan(request, passive);
        }
        if (outcome == Outcome.GUARANTEED_LOSS) {
            return bluffPlan(request, passive);
        }
        return passive;
    }

    private static Plan passivePlan(PolicyRequest request, long value, Outcome outcome) {
        String hero = request.getHero().getId();
        if (request.getLegalActions().getActions().contains(ActionType.CHECK)) {
            return new Plan(new ActionRequest(hero, ActionType.CHECK), value);
        }
        if (value < 0 && (outcome == Outcome.GUARANTEED_LOSS
                || outcome == Outcome.GUARANTEED_NON_LOSS)) {
            return new Plan(new ActionRequest(hero, ActionType.FOLD), 0);
        }
        return new Plan(new ActionRequest(hero, ActionType.CALL), value);
    }

    private static Plan valuePlan(PolicyRequest request, Plan passive) {
        double base = passive.getProjectedNetChips();
        Plan best = new Plan(passive.getAction(), base + expectedContributions(request,
                request.getView().getCurrentBet()));
        var legal = request.getLegalActions().getWager();
        if (legal.isPresent()) {
            LegalActions.Wager wager = legal.orElseThrow();
            for (long target : targets(request, wager)) {
                double value = base + expectedContributions(request, target);
                if (value > best.getProjectedNetChips()) {
                    best = wagerPlan(request, wager, target, value);
                }
            }
        }
        return best;
    }

    private static Plan bluffPlan(PolicyRequest request, Plan passive) {
        var legal = request.getLegalActions().getWager();
        List<PlayerView> opponents = request.getActiveOpponents();
        if (legal.isEmpty() || request.getParticipantCount() != HEADS_UP_PARTICIPANTS
                || opponents.size() != 1) {
            return passive;
        }
        PlayerView opponent = opponents.get(0);
        TendencyEstimate continuation = beliefs(request, opponent)
                .getContinuationEstimate(request.getStreet());
        if (continuation.getOpportunities() < MIN_BLUFF_OBSERVATIONS
                || continuation.getProbability() >= EVEN_CONTINUATION_PROBABILITY) {
            return passive;
        }
        return priceBluffs(request, opponent, legal.orElseThrow(), passive);
    }

    private static Plan priceBluffs(PolicyRequest request, PlayerView opponent,
            LegalActions.Wager wager, Plan passive) {
        long callRisk = request.getCallValue().riskedChips(request.getLegalActions().getCallCost());
        long prize = request.getCallValue().getEligiblePotAfterCall() - callRisk;
        Plan best = passive;
        for (long target : targets(request, wager)) {
            if (target - request.getView().getCurrentBet() > prize) {
                continue;
            }
            double value = projectedBluffReturn(request, opponent, target, prize);
            if (value > best.getProjectedNetChips()) {
                best = wagerPlan(request, wager, target, value);
            }
        }
        return best;
    }

    private static double projectedBluffReturn(PolicyRequest request, PlayerView opponent,
            long target, long prize) {
        Response reply = response(request, opponent, target);
        long matchedTarget = Math.min(target,
                opponent.getRoundContribution() + opponent.getChipStack());
        long risk = matchedTarget - request.getHero().getRoundContribution();
        return reply.fold * prize - (reply.call + reply.raise) * risk;
    }

    private static Set<Long> targets(PolicyRequest request, LegalActions.Wager wager) {
        if (wager.getShortAllInTarget().isPresent()) {
            return Set.of(wager.getShortAllInTarget().orElseThrow());
        }
        long minimum = wager.getMinimumFullTarget().orElseThrow();
        long maximum = wager.getMaximumTarget();
        long current = request.getView().getCurrentBet();
        long pot = request.getCallValue().getEligiblePotAfterCall();
        Set<Long> targets = new TreeSet<>(Set.of(minimum));
        targets.add(maximum);
        for (double fraction : POT_FRACTIONS) {
            long extra = Math.min(maximum - current, Math.round(pot * fraction));
            targets.add(Math.max(minimum, current + extra));
        }
        return targets;
    }

    private static Plan wagerPlan(PolicyRequest request, LegalActions.Wager wager, long target,
            double value) {
        return new Plan(new ActionRequest(request.getHero().getId(), wager.getType(), target),
                value);
    }

    private static double expectedContributions(PolicyRequest request, long target) {
        return request.getActiveOpponents().stream().mapToDouble(opponent ->
                opponentContribution(request, opponent, target)).sum();
    }

    private static double opponentContribution(PolicyRequest request, PlayerView opponent,
            long target) {
        Response reply = response(request, opponent, target);
        long cost = responseCost(opponent, target);
        long depth = remainingDepth(request, opponent, target, cost);
        double halfPot = request.getCallValue().getEligiblePotAfterCall() / 2.0;
        double opportunity = Math.min(depth, halfPot);
        double aggression = beliefs(request, opponent).getAggressionEstimate(request.getStreet())
                .getProbability();
        double induce = canInduceBetByChecking(request, opponent, target)
                ? CHECK_INDUCE_DISCOUNT * aggression * opportunity : 0;
        double future = futureStreetContribution(request, aggression, opportunity);
        return (reply.call + reply.raise) * (cost + future) + reply.raise * opportunity + induce;
    }

    private static boolean canInduceBetByChecking(PolicyRequest request, PlayerView opponent,
            long target) {
        return target == 0 && !request.isInPosition()
                && request.getView().getPlayersToAct().contains(opponent.getId());
    }

    private static double futureStreetContribution(PolicyRequest request, double aggression,
            double opportunity) {
        int streets = BettingStreet.RIVER.ordinal() - request.getStreet().ordinal();
        double position = request.isInPosition() ? 1 : OUT_OF_POSITION_DISCOUNT;
        return FUTURE_STREET_DISCOUNT * streets * position * aggression * opportunity;
    }

    private static Response response(PolicyRequest request, PlayerView opponent, long target) {
        long cost = responseCost(opponent, target);
        if (cost == 0) {
            return new Response(0, 1, 0);
        }
        OpponentBeliefs observations = beliefs(request, opponent);
        double base = observations.getContinuationEstimate(request.getStreet()).getProbability();
        long pricedPot = Math.max(1, request.getCallValue().getEligiblePotAfterCall());
        double price = cost / (double) pricedPot;
        double depth = cost / (double) opponent.getChipStack();
        double continues = base / (1 + (1 + PRICE_SENSITIVITY * (1 - base)) * price * price
                + depth * depth);
        double raise = remainingDepth(request, opponent, target, cost) == 0 ? 0
                : continues * RAISE_SHARE
                        * observations.getAggressionEstimate(request.getStreet()).getProbability();
        return new Response(1 - continues, continues - raise, raise);
    }

    private static long responseCost(PlayerView opponent, long target) {
        long owed = Math.max(0, target - opponent.getRoundContribution());
        return Math.min(opponent.getChipStack(), owed);
    }

    private static long remainingDepth(PolicyRequest request, PlayerView opponent, long target,
            long cost) {
        PlayerView hero = request.getHero();
        long ownCost = Math.min(hero.getChipStack(), target - hero.getRoundContribution());
        return Math.min(hero.getChipStack() - ownCost, opponent.getChipStack() - cost);
    }

    private static OpponentBeliefs beliefs(PolicyRequest request, PlayerView opponent) {
        return request.getOpponents().getOrDefault(opponent.getId(), PRIOR);
    }

    private static final class Response {
        private final double fold;
        private final double call;
        private final double raise;

        private Response(double fold, double call, double raise) {
            this.fold = fold;
            this.call = call;
            this.raise = raise;
        }
    }

    /** Selected action and modeled net value in chips measured from this decision. */
    public static final class Plan {
        private final ActionRequest action;
        private final double projectedNetChips;

        private Plan(ActionRequest action, double projectedNetChips) {
            this.action = action;
            this.projectedNetChips = projectedNetChips;
        }

        public ActionRequest getAction() { return action; }
        public double getProjectedNetChips() { return projectedNetChips; }
    }
}
