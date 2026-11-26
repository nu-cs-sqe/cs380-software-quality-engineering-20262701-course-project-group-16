package application;

import bot.EquityEstimate;
import bot.EquityRequest;
import bot.MonteCarloEquityEstimator;
import bot.policy.BotPolicy;
import bot.policy.CallValue;
import bot.policy.KnownOutcomePolicy;
import bot.policy.PolicyRequest;
import domain.model.ActionRequest;
import domain.model.ExactOutcomeEvaluator;
import domain.model.RiverOutcomeEvaluator.Outcome;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/** Worker-confined calculation; it has no session or Swing reference. */
public final class BotComputer {
    private final ExactOutcomeEvaluator outcomes;
    private final BiFunction<EquityRequest, BooleanSupplier, EquityEstimate> estimate;
    private final DoubleSupplier random;

    public BotComputer() {
        this(new MonteCarloEquityEstimator()::estimate, new Random()::nextDouble);
    }

    BotComputer(BiFunction<EquityRequest, BooleanSupplier, EquityEstimate> estimate,
            DoubleSupplier random) {
        this(new ExactOutcomeEvaluator(), estimate, random);
    }

    BotComputer(ExactOutcomeEvaluator outcomes,
            BiFunction<EquityRequest, BooleanSupplier, EquityEstimate> estimate,
            DoubleSupplier random) {
        this.outcomes = outcomes;
        this.estimate = estimate;
        this.random = random;
    }

    public ActionRequest decide(BotTurn turn, BooleanSupplier cancelled) {
        requireRunning(cancelled);
        Outcome outcome = determineOutcome(turn.getScenario(), cancelled);
        requireRunning(cancelled);
        return outcome == Outcome.UNCERTAIN ? sampledAction(turn, cancelled)
                : actionForKnownOutcome(turn, outcome);
    }

    private Outcome determineOutcome(EquityRequest cards, BooleanSupplier cancelled) {
        return cards.getBoard().isEmpty() ? Outcome.UNCERTAIN
                : outcomes.evaluate(cards.getBoard(), cards.getHoleCards(), cancelled);
    }

    static ActionRequest actionForKnownOutcome(BotTurn turn, Outcome outcome) {
        EquityRequest scenario = turn.getScenario();
        long payout = KnownOutcomePolicy.minimumPayout(scenario, outcome);
        PolicyRequest request = requestFor(turn, payout / (double) scenario.getEligiblePotAmount());
        return new KnownOutcomePolicy().choose(request, outcome, payout).getAction();
    }

    private ActionRequest sampledAction(BotTurn turn, BooleanSupplier cancelled) {
        EquityEstimate equity = estimate.apply(turn.getScenario(), cancelled);
        requireRunning(cancelled);
        return new BotPolicy().distribution(requestFor(turn, equity.getEquity()),
                turn.getPersonality())
                .sample(random.getAsDouble());
    }

    private static PolicyRequest requestFor(BotTurn turn, double equity) {
        CallValue call = new CallValue(equity, turn.getScenario().getEligiblePotAmount(),
                turn.getCallRefund());
        return new PolicyRequest(turn.getView(), turn.getLegalActions(), call, turn.getBeliefs());
    }

    private static void requireRunning(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("The bot calculation was cancelled.");
        }
    }
}
