package bot;

import domain.model.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class MonteCarloEquityEstimatorTest {
    private static final HandEvaluator ORDER = (first, second) ->
            Integer.compare(first.get(0).getRank().ordinal(), second.get(0).getRank().ordinal());

    @ParameterizedTest
    @EnumSource(value = Rank.class, names = {"NINE", "TEN", "JACK"})
    void mcC01AccountsForWinsLossesAndTwoWayTies(Rank opponent) {
        EquityEstimator estimator = new MonteCarloEquityEstimator(ORDER, r -> EquityFixtures.deal(opponent));
        EquityEstimate estimate = estimator.estimate(EquityFixtures.request(2, opponent));
        double expected = opponent == Rank.NINE ? 1 : opponent == Rank.TEN ? 0.5 : 0;
        assertEquals(expected, estimate.getEquity());
        assertEquals(expected, estimate.getPotShares().get("main"));
        assertEquals(120 * expected, estimate.getExpectedPayout());
        assertEquals(2, estimate.getSimulations());
        assertThrows(UnsupportedOperationException.class, () -> estimate.getPotShares().clear());
    }
    @Test
    void mcC02DividesMultiwayTiesAndDoesNotCountATieBeforeALaterStrongerHand() {
        MonteCarloEquityEstimator tied = new MonteCarloEquityEstimator(ORDER,
                r -> EquityFixtures.deal(Rank.TEN, Rank.TEN));
        assertEquals(1.0 / 3, tied.estimate(EquityFixtures.request(3, Rank.TEN, Rank.TEN)).getEquity(), 1e-12);
        MonteCarloEquityEstimator later = new MonteCarloEquityEstimator(ORDER,
                r -> EquityFixtures.deal(Rank.TEN, Rank.JACK));
        assertEquals(0, later.estimate(EquityFixtures.request(1, Rank.TEN, Rank.JACK)).getEquity());
    }
    @Test
    void mcC03SeparatesSidePotEligibilityAndCachesComparisonsAcrossPots() {
        AtomicInteger comparisons = new AtomicInteger();
        MonteCarloEquityEstimator estimator = new MonteCarloEquityEstimator((a, b) -> {
            comparisons.incrementAndGet(); return ORDER.compare(a, b);
        }, r -> EquityFixtures.deal(Rank.NINE, Rank.JACK));
        EquityRequest request = new EquityRequest("hero", EquityFixtures.HOLE, EquityFixtures.BOARD,
                List.of("p1", "p2"), List.of(new EquityPot("main", 100, Set.of("hero", "p1")),
                        new EquityPot("side", 200, Set.of("hero", "p2")),
                        new EquityPot("other", 300, Set.of("p1", "p2"))), 2);
        EquityEstimate estimate = estimator.estimate(request);
        assertEquals(Map.of("main", 1.0, "side", 0.0, "other", 0.0), estimate.getPotShares());
        assertEquals(100, estimate.getExpectedPayout());
        assertEquals(1.0 / 3, estimate.getEquity(), 1e-12);
        assertEquals(4, comparisons.get());
    }
    @Test
    void mcC04AveragesTrialSharesRatherThanCountingOnlyOutrightWins() {
        AtomicInteger sample = new AtomicInteger();
        Rank[] opponents = {Rank.NINE, Rank.TEN, Rank.JACK};
        MonteCarloEquityEstimator estimator = new MonteCarloEquityEstimator(ORDER,
                r -> EquityFixtures.deal(opponents[sample.getAndIncrement()]));
        EquityEstimate estimate = estimator.estimate(EquityFixtures.request(3, Rank.TEN));
        assertEquals(0.5, estimate.getEquity());
        assertEquals(60, estimate.getExpectedPayout());
        assertEquals(3, sample.get());
    }
    @Test
    void mcC05SingleEligiblePlayerReceivesThePotWithoutComparison() {
        MonteCarloEquityEstimator estimator = new MonteCarloEquityEstimator((a, b) -> {
            throw new AssertionError("No opponents to compare");
        }, r -> EquityFixtures.deal());
        EquityEstimate estimate = estimator.estimate(EquityFixtures.request(1));
        assertEquals(1, estimate.getEquity());
        assertEquals(120, estimate.getExpectedPayout());
    }
    @Test
    void mcC06RejectsInvalidCalculationInputsAndDependencies() {
        assertThrows(IllegalArgumentException.class, () -> new MonteCarloEquityEstimator(null, r -> EquityFixtures.deal()));
        assertThrows(IllegalArgumentException.class, () -> new MonteCarloEquityEstimator(ORDER, null));
        MonteCarloEquityEstimator estimator = new MonteCarloEquityEstimator(ORDER, r -> EquityFixtures.deal());
        assertThrows(IllegalArgumentException.class, () -> estimator.estimate(null));
        assertThrows(IllegalArgumentException.class, () -> estimator.estimate(EquityFixtures.request(1), null));
    }
    @Test
    void mcC07CancellationDoesNotReturnPartialResultsOrRetainAggregates() {
        AtomicInteger samples = new AtomicInteger();
        MonteCarloEquityEstimator estimator = new MonteCarloEquityEstimator(ORDER, r -> {
            samples.incrementAndGet(); return EquityFixtures.deal(Rank.NINE);
        });
        EquityRequest request = EquityFixtures.request(3, Rank.NINE);
        assertThrows(CancellationException.class, () -> estimator.estimate(request, () -> true));
        assertEquals(0, samples.get());
        assertThrows(CancellationException.class, () -> estimator.estimate(request, () -> samples.get() >= 1));
        assertEquals(1, samples.get());
        assertEquals(1, estimator.estimate(request).getEquity());
        assertEquals(4, samples.get());
    }
    @Test
    void mcC08HonorsThreadInterruptionWithoutClearingIt() {
        MonteCarloEquityEstimator estimator = new MonteCarloEquityEstimator(ORDER, r -> {
            throw new AssertionError("An interrupted calculation cannot sample");
        });
        try {
            Thread.currentThread().interrupt();
            assertThrows(CancellationException.class, () -> estimator.estimate(EquityFixtures.request(1)));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }
    @Test
    void mcC09PropagatesCollaboratorFailuresWithoutPublishingAResult() {
        IllegalArgumentException failure = new IllegalArgumentException("evaluator unavailable");
        MonteCarloEquityEstimator evaluation = new MonteCarloEquityEstimator((a, b) -> { throw failure; },
                r -> EquityFixtures.deal(Rank.NINE));
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> evaluation.estimate(EquityFixtures.request(1, Rank.NINE))));
        MonteCarloEquityEstimator sampling = new MonteCarloEquityEstimator(ORDER, r -> { throw failure; });
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> sampling.estimate(EquityFixtures.request(1))));
    }
    @Test
    void mcC10StrongerHeroStillCannotWinAnIneligibleSidePot() {
        EquityRequest request = new EquityRequest("hero", EquityFixtures.HOLE, EquityFixtures.BOARD,
                List.of("p1"), List.of(new EquityPot("main", 100, Set.of("hero", "p1")),
                        new EquityPot("other", 200, Set.of("p1"))), 1);
        EquityEstimate estimate = new MonteCarloEquityEstimator(ORDER,
                ignored -> EquityFixtures.deal(Rank.NINE)).estimate(request);
        assertEquals(Map.of("main", 1.0, "other", 0.0), estimate.getPotShares());
        assertEquals(1, estimate.getEquity());
        assertEquals(100, estimate.getExpectedPayout());
    }

}
