package application;

import domain.model.Player;
import domain.model.PlayerType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FundingCapacityTest {
    @Test
    void rawFundingTotalsRejectOverflowWithoutWrapping() {
        List<Player> exact = roster(Long.MAX_VALUE - 5000);
        assertEquals(Long.MAX_VALUE, TableRoster.totalChips(exact).orElseThrow());
        List<Player> overflow = roster(Long.MAX_VALUE - 4999);
        assertTrue(TableRoster.totalChips(overflow).isEmpty());
        assertFalse(FundingCapacity.accepts(overflow, Map.of("p0", "account"),
                Map.of("account", Long.MAX_VALUE)));
        assertEquals(0, FundingCapacity.maximumAddition(overflow, Map.of("p0", "account"),
                Map.of("account", Long.MAX_VALUE), "p0"));
    }

    @Test
    void anUnfundedAccountCannotBuyAnUnpayablePotentialWin() {
        List<Player> roster = roster(0);
        assertTrue(FundingCapacity.accepts(roster, Map.of("p0", "account"),
                Map.of("account", Long.MAX_VALUE)));
        assertEquals(0, FundingCapacity.maximumAddition(roster, Map.of("p0", "account"),
                Map.of("account", Long.MAX_VALUE), "p0"));
    }

    @Test
    void suppliedFundingLimitsCannotExceedAvailableFunds() {
        assertThrows(IllegalArgumentException.class, () -> new TableFunds(10000, 1000, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> new TableFunds(10000, 1000, 0, 9001));
        TableFunds limited = new TableFunds(10000, 1000, 0, 2000);
        assertEquals(2000, limited.getMaximumTopUp());
        assertEquals(1, limited.getMinimumTopUp());
        assertEquals(0, new TableFunds(500, 0, 0, 399).getMaximumTopUp());
    }

    @Test
    void anotherAccountsOutsideFundsCanPreventFurtherDeposits() {
        List<Player> players = new ArrayList<>(roster(1000));
        players.set(1, new Player("p1", 1, PlayerType.HUMAN, 1000));
        players.set(2, new Player("p2", 2, PlayerType.BOT, 5000));
        Map<String, String> accounts = Map.of("p0", "first", "p1", "second");
        Map<String, Long> balances = Map.of("first", Long.MAX_VALUE - 12000, "second", 10000L);
        assertFalse(FundingCapacity.accepts(players, accounts, balances));
        assertEquals(0, FundingCapacity.maximumAddition(players, accounts, balances, "p1"));
    }

    private static List<Player> roster(long firstStack) {
        List<Player> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            players.add(new Player("p" + seat, seat,
                    seat == 0 ? PlayerType.HUMAN : PlayerType.BOT,
                    seat == 0 ? firstStack : 1000));
        }
        return List.copyOf(players);
    }
}
