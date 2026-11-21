package application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class TableFundsTest {
    @ParameterizedTest
    @CsvSource({
        "0,0,400,0,true,false", "399,0,400,0,true,false",
        "400,0,400,400,false,true", "401,0,400,401,false,true",
        "399,200,200,0,false,false", "400,200,200,200,false,false",
        "400,399,1,1,false,false", "401,399,1,2,false,false",
        "10000,3000,1,7000,false,false", "1,1,399,0,false,false"
    })
    void fundingChoicesRespectTheMinimumAndAvailableBalance(long balance, long stack,
            long minimum, long maximum, boolean recovery, boolean topUp) {
        TableFunds funds = new TableFunds(balance, stack, 0);
        assertEquals(minimum, funds.getMinimumTopUp());
        assertEquals(maximum, funds.getMaximumTopUp());
        assertEquals(recovery, funds.needsRecovery());
        assertEquals(topUp, funds.needsTopUp());
    }

    @Test
    void largeAccountBalancesHaveNoFixedFundingCap() {
        TableFunds funds = new TableFunds(Long.MAX_VALUE, 0, 0);
        assertEquals(Long.MAX_VALUE, funds.getMaximumTopUp());
        assertEquals(400, funds.getMinimumTopUp());
    }
}
