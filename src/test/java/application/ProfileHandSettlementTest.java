package application;

import domain.model.CompletedStatistics;
import domain.model.PlayStats;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ProfileHandSettlementTest {
    @ParameterizedTest
    @CsvSource({"' ',session,profile,hand", "hand,' ',profile,hand",
        "hand,session,' ',hand", "hand,session,profile,other"})
    void rejectsBlankIdsAndMismatchedHandStatistics(String handId, String sessionId,
            String profileId, String completedId) {
        Map<String, AccountHandResult> accounts = Map.of(profileId, result(completedId));
        assertThrows(IllegalArgumentException.class,
                () -> new ProfileHandSettlement(handId, sessionId, accounts));
    }

    @Test
    void requiresAtLeastOneAccountAndPermitsAtMostSix() {
        LinkedHashMap<String, AccountHandResult> accounts = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class,
                () -> new ProfileHandSettlement("hand", "session", accounts));
        for (int seat = 0; seat < 6; seat++) {
            accounts.put("account-" + seat, result("hand"));
        }
        ProfileHandSettlement hand = new ProfileHandSettlement("hand", "session", accounts);
        accounts.put("seventh", result("hand"));
        assertThrows(IllegalArgumentException.class,
                () -> new ProfileHandSettlement("hand", "session", accounts));
        assertEquals(6, hand.getAccounts().size());
        assertThrows(UnsupportedOperationException.class, () -> hand.getAccounts().clear());
    }

    @Test
    void rejectsStatisticsWithoutACompletedHand() {
        AccountHandResult open = new AccountHandResult(0, new CompletedStatistics());
        assertThrows(IllegalArgumentException.class,
                () -> new ProfileHandSettlement("hand", "session", Map.of("profile", open)));
    }

    private static AccountHandResult result(String handId) {
        PlayStats stats = new PlayStats();
        stats.beginHand(handId, true);
        stats.completeHand(handId, false, false, 0, 0);
        return new AccountHandResult(0, stats.completedSnapshot());
    }
}
