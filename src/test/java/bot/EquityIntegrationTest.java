package bot;

import domain.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EquityIntegrationTest {
    @Test
    void eiC01KnownNutsWinAgainstFiveOpponents() {
        EquityRequest request = request(List.of(c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS)),
                List.of(c(Rank.ACE, Suit.CLUBS), c(Rank.ACE, Suit.DIAMONDS), c(Rank.TWO, Suit.CLUBS),
                        c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.HEARTS)), 5, 3);
        EquityEstimate result = new MonteCarloEquityEstimator().estimate(request);
        assertEquals(1, result.getEquity());
        assertEquals(120, result.getExpectedPayout());
    }
    @Test
    void eiC02SharedRoyalFlushSplitsSixWays() {
        EquityRequest request = request(List.of(c(Rank.TWO, Suit.CLUBS), c(Rank.THREE, Suit.DIAMONDS)),
                List.of(c(Rank.TEN, Suit.HEARTS), c(Rank.JACK, Suit.HEARTS), c(Rank.QUEEN, Suit.HEARTS),
                        c(Rank.KING, Suit.HEARTS), c(Rank.ACE, Suit.HEARTS)), 5, 6);
        EquityEstimate result = new MonteCarloEquityEstimator().estimate(request);
        assertEquals(1.0 / 6, result.getEquity(), 1e-12);
        assertEquals(20, result.getExpectedPayout(), 1e-12);
    }
    @Test
    void eiC03SeededRiverEstimatesMatchExhaustiveReference() {
        EquityRequest request = request(List.of(c(Rank.ACE, Suit.CLUBS), c(Rank.ACE, Suit.DIAMONDS)),
                List.of(c(Rank.TWO, Suit.CLUBS), c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.HEARTS),
                        c(Rank.NINE, Suit.SPADES), c(Rank.KING, Suit.HEARTS)), 1, 2048);
        double exact = exactRiver(request);
        for (long seed : new long[] {7, 23, 91}) {
            long start = System.nanoTime();
            EquityEstimate estimate = new MonteCarloEquityEstimator(new PokerHandEvaluator(),
                    new UniformDealSampler(new Random(seed)::nextInt)).estimate(request);
            double error = Math.abs(exact - estimate.getEquity());
            System.out.printf(Locale.ROOT, "river seed=%d exact=%.9f estimate=%.9f error=%.9f elapsedMs=%.3f%n",
                    seed, exact, estimate.getEquity(), error, (System.nanoTime() - start) / 1e6);
            assertTrue(error <= 0.06, "Fixed-seed regression tolerance exceeded: " + error);
        }
    }
    @Test
    void eiC04RecordsSixPlayerPreflopLatencyWithoutTimingFlakiness() {
        EquityRequest request = request(List.of(c(Rank.ACE, Suit.SPADES), c(Rank.KING, Suit.DIAMONDS)),
                List.of(), 5, 1000);
        long start = System.nanoTime();
        EquityEstimate result = new MonteCarloEquityEstimator(new PokerHandEvaluator(),
                new UniformDealSampler(new Random(49)::nextInt)).estimate(request);
        System.out.printf(Locale.ROOT, "sixPlayerPreflop trials=%d equity=%.9f elapsedMs=%.3f%n",
                result.getSimulations(), result.getEquity(), (System.nanoTime() - start) / 1e6);
        assertEquals(1000, result.getSimulations());
        assertTrue(Double.isFinite(result.getEquity()));
        assertTrue(result.getEquity() >= 0 && result.getEquity() <= 1);
        assertTrue(result.getExpectedPayout() >= 0 && result.getExpectedPayout() <= 120);
    }
    private static double exactRiver(EquityRequest request) {
        List<Card> unseen = new ArrayList<>(new Deck().deal(52));
        unseen.removeAll(request.getHoleCards());
        unseen.removeAll(request.getBoard());
        HandEvaluator evaluator = new PokerHandEvaluator();
        List<Card> own = EquityFixtures.holding(request.getHoleCards(), request.getBoard());
        double shares = 0;
        int combinations = 0;
        for (int i = 0; i < unseen.size(); i++) {
            for (int j = i + 1; j < unseen.size(); j++) {
                int comparison = evaluator.compare(own, EquityFixtures.holding(
                        List.of(unseen.get(i), unseen.get(j)), request.getBoard()));
                shares += comparison > 0 ? 1 : comparison == 0 ? 0.5 : 0;
                combinations++;
            }
        }
        assertEquals(990, combinations);
        return shares / combinations;
    }
    private static EquityRequest request(List<Card> holes, List<Card> board, int opponents, int trials) {
        List<String> ids = EquityFixtures.opponents(opponents);
        Set<String> eligible = new HashSet<>(ids);
        eligible.add("hero");
        return new EquityRequest("hero", holes, board, ids,
                List.of(new EquityPot("main", 120, eligible)), trials);
    }
    private static Card c(Rank rank, Suit suit) { return new Card(rank, suit); }
}
