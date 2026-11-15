package bot.policy;

import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CallValueTest {
    @ParameterizedTest
    @ValueSource(doubles = {-0.000001, 1.000001, Double.NaN,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void cv01RejectsInvalidEquity(double equity) {
        assertThrows(IllegalArgumentException.class, () -> new CallValue(equity, 30, 0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.000001, 0.5, 0.999999, 1})
    void cv02RetainsUnroundedBoundaryValues(double equity) {
        CallValue value = new CallValue(equity, Long.MAX_VALUE, Long.MAX_VALUE);
        assertEquals(equity, value.getEquity());
        assertEquals(Long.MAX_VALUE, value.getEligiblePotAfterCall());
        assertEquals(Long.MAX_VALUE, value.getCallRefund());
        assertEquals(0, value.riskedChips(Long.MAX_VALUE));
    }

    @Test
    void cv03RejectsNegativeChipsAndImpossibleRefunds() {
        assertThrows(IllegalArgumentException.class, () -> new CallValue(0.5, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new CallValue(0.5, 10, -1));
        CallValue value = new CallValue(0.5, 10, 5);
        assertThrows(IllegalArgumentException.class, () -> value.riskedChips(4));
        assertThrows(IllegalArgumentException.class, () -> value.breakEvenEquity(4));
        assertThrows(IllegalArgumentException.class, () -> value.riskedChips(-1));
        assertThrows(IllegalArgumentException.class, () -> value.breakEvenEquity(16));
    }

    @Test
    void cv04ComputesOddsFromEligiblePotsAndNewChipRefundOnly() {
        CallValue value = new CallValue(0.25, 100, 10);
        assertEquals(20, value.riskedChips(30));
        assertEquals(0.2, value.breakEvenEquity(30).orElseThrow());
        assertEquals(0, value.breakEvenEquity(10).orElseThrow());
        assertEquals(1, value.breakEvenEquity(110).orElseThrow());
        assertEquals(OptionalDouble.empty(), new CallValue(0.5, 0, 0).breakEvenEquity(0));
        assertThrows(IllegalArgumentException.class,
                () -> new CallValue(0.5, 0, 0).breakEvenEquity(1));
    }
}
