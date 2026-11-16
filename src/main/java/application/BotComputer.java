package application;

import bot.EquityEstimate;
import bot.EquityRequest;
import bot.MonteCarloEquityEstimator;
import bot.policy.BotPolicy;
import bot.policy.CallValue;
import bot.policy.PolicyRequest;
import domain.model.ActionRequest;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/** Worker-confined calculation; it has no session or Swing reference. */
public final class BotComputer {
    private final BiFunction<EquityRequest, BooleanSupplier, EquityEstimate> estimate;
    private final DoubleSupplier random;

    public BotComputer() {
        this(new MonteCarloEquityEstimator()::estimate, new Random()::nextDouble);
    }

    BotComputer(BiFunction<EquityRequest, BooleanSupplier, EquityEstimate> estimate,
            DoubleSupplier random) {
        this.estimate = estimate;
        this.random = random;
    }

    public ActionRequest decide(BotTurn turn, BooleanSupplier cancelled) {
        requireRunning(cancelled);
        EquityEstimate equity = estimate.apply(turn.getScenario(), cancelled);
        requireRunning(cancelled);
        CallValue call = new CallValue(equity.getEquity(),
                turn.getScenario().getEligiblePotAmount(), turn.getCallRefund());
        PolicyRequest request = new PolicyRequest(turn.getView(), turn.getLegalActions(),
                call, turn.getBeliefs());
        return new BotPolicy().distribution(request, turn.getPersonality())
                .sample(random.getAsDouble());
    }

    private static void requireRunning(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("The bot calculation was cancelled.");
        }
    }
}
