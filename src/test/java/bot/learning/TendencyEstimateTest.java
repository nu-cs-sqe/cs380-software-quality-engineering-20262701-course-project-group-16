package bot.learning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class TendencyEstimateTest {
    @ParameterizedTest
    @CsvSource({"0,0,0.5,0", "0,1,0.3333333333333333,0.3333333333333333",
            "1,1,0.6666666666666666,0.3333333333333333", "2,4,0.5,0.6666666666666666"})
    void teC01ReturnsSmoothedNumericBeliefs(long successes, long opportunities, double probability, double weight) {
        TendencyEstimate estimate = new TendencyEstimate(successes, opportunities);
        assertEquals(successes, estimate.getSuccesses());
        assertEquals(opportunities, estimate.getOpportunities());
        assertEquals(probability, estimate.getProbability(), 1e-15);
        assertEquals(weight, estimate.getEvidenceWeight(), 1e-15);
        assertEquals(Math.sqrt(probability * (1 - probability) / (opportunities + 3)),
                estimate.getStandardDeviation(), 1e-15);
    }
    @ParameterizedTest
    @CsvSource({"-1,0", "0,-1", "1,0", "3,2"})
    void teC02RejectsInconsistentCounts(long successes, long opportunities) {
        assertThrows(IllegalArgumentException.class, () -> new TendencyEstimate(successes, opportunities));
    }
    @Test
    void teC03UpdatesIndependentlyAndCountsDeclinesAsOpportunities() {
        TendencyEstimate initial = new TendencyEstimate(0, 0);
        TendencyEstimate next = initial.withOutcome(true).withOutcome(false);
        assertEquals(0, initial.getOpportunities());
        assertEquals(0, initial.getSuccesses());
        assertEquals(2, next.getOpportunities());
        assertEquals(1, next.getSuccesses());
        assertEquals(0.5, next.getProbability());
        assertTrue(next.getStandardDeviation() < initial.getStandardDeviation());
    }
    @Test
    void teC04HandlesLongBoundaryWithoutWrappingOrMutating() {
        TendencyEstimate maximum = new TendencyEstimate(Long.MAX_VALUE, Long.MAX_VALUE);
        assertEquals(1, maximum.getProbability());
        assertEquals(1, maximum.getEvidenceWeight());
        assertTrue(Double.isFinite(maximum.getStandardDeviation()));
        assertThrows(ArithmeticException.class, () -> assertNotNull(maximum.withOutcome(true)));
        assertThrows(ArithmeticException.class, () -> assertNotNull(maximum.withOutcome(false)));
        assertEquals(Long.MAX_VALUE, maximum.getSuccesses());
        assertEquals(Long.MAX_VALUE, maximum.getOpportunities());
    }
}
