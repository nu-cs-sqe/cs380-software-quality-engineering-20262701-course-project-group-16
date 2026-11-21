package application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class BuyInLimitsTest {
    @ParameterizedTest
    @CsvSource({"400,400", "401,401", "999,999", "1000,1000", "1001,1000"})
    void defaultChoiceUsesAvailableFundsUpToOneThousand(long balance, long expected) {
        assertEquals(expected, BuyInLimits.defaultAmount(balance));
    }

    @Test
    void underfundedDefaultsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> assertEquals(399, BuyInLimits.defaultAmount(399)));
    }

    @ParameterizedTest
    @CsvSource({"-1,2000", "0,2000", "399,2000", "400,399", "2001,2000"})
    void invalidInitialChoicesAreRejected(long amount, long balance) {
        assertThrows(IllegalArgumentException.class,
                () -> BuyInLimits.requireAmount(amount, balance));
    }

    @Test
    void choicesAboveTwoThousandHaveNoFixedCap() {
        assertDoesNotThrow(() -> BuyInLimits.requireAmount(400, 400));
        assertDoesNotThrow(() -> BuyInLimits.requireAmount(401, 401));
        assertDoesNotThrow(() -> BuyInLimits.requireAmount(2001, 2001));
        assertDoesNotThrow(() -> BuyInLimits.requireAmount(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(1000, BuyInLimits.defaultAmount(Long.MAX_VALUE));
    }
}
