package application;

import bot.EquityEstimate;
import bot.EquityPot;
import bot.EquityRequest;
import bot.MonteCarloEquityEstimator;
import bot.policy.BotPolicy;
import bot.policy.CallValue;
import bot.policy.PolicyRequest;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.GamePhase;
import domain.model.RiverOutcomeEvaluator;
import domain.model.RiverOutcomeEvaluator.Outcome;
import java.util.Optional;
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
        Optional<ActionType> exact = exactRiverAction(turn, cancelled);
        requireRunning(cancelled);
        return exact.map(type -> new ActionRequest(turn.getScenario().getHeroId(), type))
                .orElseGet(() -> sampledAction(turn, cancelled));
    }

    private static Optional<ActionType> exactRiverAction(BotTurn turn, BooleanSupplier cancelled) {
        if (turn.getView().getPhase() != GamePhase.RIVER) {
            return Optional.empty();
        }
        EquityRequest cards = turn.getScenario();
        Outcome outcome = new RiverOutcomeEvaluator().evaluate(cards.getBoard(),
                cards.getHoleCards(), cancelled);
        if (outcome == Outcome.UNCERTAIN) {
            return Optional.empty();
        }
        return Optional.of(actionForKnownOutcome(turn, outcome));
    }

    static ActionType actionForKnownOutcome(BotTurn turn, Outcome outcome) {
        if (turn.getLegalActions().getActions().contains(ActionType.CHECK)) {
            return ActionType.CHECK;
        }
        return outcome == Outcome.GUARANTEED_LOSS && !coversCallRisk(turn)
                ? ActionType.FOLD : ActionType.CALL;
    }

    /** Even a losing holding can recover a refund or a pot with no opposing claimant. */
    private static boolean coversCallRisk(BotTurn turn) {
        EquityRequest scenario = turn.getScenario();
        long solePayout = scenario.getPots().stream()
                .filter(pot -> pot.getEligiblePlayers().size() == 1
                        && pot.getEligiblePlayers().contains(scenario.getHeroId()))
                .mapToLong(EquityPot::getAmount).sum();
        long risk = turn.getLegalActions().getCallCost() - turn.getCallRefund();
        return solePayout >= risk;
    }

    private ActionRequest sampledAction(BotTurn turn, BooleanSupplier cancelled) {
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
