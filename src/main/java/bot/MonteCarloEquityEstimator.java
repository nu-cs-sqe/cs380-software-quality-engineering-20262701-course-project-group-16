package bot;

import domain.model.Card;
import domain.model.HandEvaluator;
import domain.model.PokerHandEvaluator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

/** Bounded equity simulation; worker-confined sources; no live state or totals are retained. */
public final class MonteCarloEquityEstimator implements EquityEstimator {
    private final HandEvaluator evaluator;
    private final DealSampler sampler;

    public MonteCarloEquityEstimator() {
        this(new PokerHandEvaluator(), new UniformDealSampler());
    }

    public MonteCarloEquityEstimator(HandEvaluator evaluator, DealSampler sampler) {
        if (evaluator == null || sampler == null) {
            throw new IllegalArgumentException(
                    "An evaluator and hypothetical sampler are required.");
        }
        this.evaluator = evaluator;
        this.sampler = sampler;
    }

    @Override
    public EquityEstimate estimate(EquityRequest request) {
        return estimate(request, () -> false);
    }

    /** Cancellation is checked between trials; no partial result is returned. */
    public EquityEstimate estimate(EquityRequest request, BooleanSupplier cancelled) {
        if (request == null || cancelled == null) {
            throw new IllegalArgumentException("A request and cancellation source are required.");
        }
        Map<String, Double> totals = new LinkedHashMap<>();
        request.getPots().forEach(pot -> totals.put(pot.getId(), 0.0));
        IntStream.range(0, request.getSimulations()).forEach(trial ->
                sampleTrial(request, cancelled, totals));
        return summarize(request, totals);
    }

    private void sampleTrial(EquityRequest request, BooleanSupplier cancelled,
            Map<String, Double> totals) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Equity calculation was cancelled.");
        }
        Map<String, List<Card>> deal = sampler.sample(request);
        Map<String, Integer> comparisons = compareOpponents(request, deal);
        for (EquityPot pot : request.getPots()) {
            double share = share(request.getHeroId(), request.getOpponents(), pot, comparisons);
            totals.put(pot.getId(), totals.get(pot.getId()) + share);
        }
    }

    private Map<String, Integer> compareOpponents(EquityRequest request,
            Map<String, List<Card>> deal) {
        Map<String, Integer> comparisons = new LinkedHashMap<>();
        List<Card> own = deal.get(request.getHeroId());
        for (String opponent : request.getOpponents()) {
            comparisons.put(opponent, evaluator.compare(own, deal.get(opponent)));
        }
        return comparisons;
    }

    private static double share(String hero, List<String> opponents, EquityPot pot,
            Map<String, Integer> comparisons) {
        if (!pot.getEligiblePlayers().contains(hero)) {
            return 0;
        }
        int tied = 1;
        for (String opponent : opponents) {
            if (pot.getEligiblePlayers().contains(opponent)) {
                int comparison = comparisons.get(opponent);
                if (comparison < 0) {
                    return 0;
                }
                if (comparison == 0) {
                    tied++;
                }
            }
        }
        return 1.0 / tied;
    }

    private static EquityEstimate summarize(EquityRequest request, Map<String, Double> totals) {
        Map<String, Double> shares = new LinkedHashMap<>();
        totals.forEach((id, total) -> shares.put(id, total / request.getSimulations()));
        double payout = request.getPots().stream().mapToDouble(pot ->
                pot.getAmount() * shares.get(pot.getId())).sum();
        // Bound floating-point accumulation at the mathematical maximum; do not round rates.
        double boundedPayout = Math.min(payout, request.getEligiblePotAmount());
        return new EquityEstimate(request.getSimulations(), shares,
                boundedPayout / request.getEligiblePotAmount(), boundedPayout);
    }
}
