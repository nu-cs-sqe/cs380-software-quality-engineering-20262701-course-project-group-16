package bot;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class EquityPotTest {
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t"})
    void epC01RejectsBlankIds(String id) {
        assertThrows(IllegalArgumentException.class, () -> new EquityPot(id, 1, Set.of("hero")));
    }
    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void epC02RejectsNonpositiveAmounts(long amount) {
        assertThrows(IllegalArgumentException.class, () -> new EquityPot("main", amount, Set.of("hero")));
    }
    @Test
    void epC03RejectsMissingOrInvalidEligibility() {
        assertThrows(IllegalArgumentException.class, () -> new EquityPot("main", 1, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new EquityPot("main", 1, Set.of(" ")));
    }
    @Test
    void epC04RetainsExactValuesAndImmutableDetachedEligibility() {
        Set<String> eligible = new HashSet<>(Set.of("hero", "other"));
        EquityPot pot = new EquityPot(" main ", Long.MAX_VALUE, eligible);
        eligible.clear();
        assertEquals(" main ", pot.getId());
        assertEquals(Long.MAX_VALUE, pot.getAmount());
        assertEquals(Set.of("hero", "other"), pot.getEligiblePlayers());
        assertThrows(UnsupportedOperationException.class, () -> pot.getEligiblePlayers().clear());
    }
}
