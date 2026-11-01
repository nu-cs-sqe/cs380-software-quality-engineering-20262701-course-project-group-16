package domain;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatsContextTest {

    @ParameterizedTest(name = "{0}: rejects inconsistent engine context")
    @MethodSource("invalidContexts")
    void rejectsInvalidContexts(String id, PreflopRaiseType raise,
            boolean threeBet, boolean cBet, boolean facedCBet) {
        assertThrows(IllegalArgumentException.class,
                () -> new StatsContext(raise, threeBet, cBet, facedCBet), id);
    }

    static Stream<Arguments> invalidContexts() {
        return Stream.of(
                Arguments.of("SC-I01", null, false, false, false),
                Arguments.of("SC-I02", PreflopRaiseType.NONE, false, true, true),
                Arguments.of("SC-I03", PreflopRaiseType.NONE, true, true, false),
                Arguments.of("SC-I04", PreflopRaiseType.NONE, true, false, true),
                Arguments.of("SC-I05", PreflopRaiseType.OPENING_RAISE, false, true, false),
                Arguments.of("SC-I06", PreflopRaiseType.THREE_BET, false, false, false),
                Arguments.of("SC-I07", PreflopRaiseType.OPENING_RAISE, true, false, false),
                Arguments.of("SC-I08", PreflopRaiseType.FOUR_BET, true, false, false),
                Arguments.of("SC-I09", PreflopRaiseType.LATER_RAISE, true, false, false),
                Arguments.of("SC-I10", PreflopRaiseType.FOUR_BET, false, false, true));
    }

    @ParameterizedTest(name = "{0}: retains explicit engine context")
    @MethodSource("validContexts")
    void retainsValidContexts(String id, PreflopRaiseType raise,
            boolean threeBet, boolean cBet, boolean facedCBet) {
        StatsContext context = new StatsContext(raise, threeBet, cBet, facedCBet);

        assertEquals(raise, context.getPreflopRaiseType(), id);
        assertEquals(threeBet, context.hasThreeBetOpportunity(), id);
        assertEquals(cBet, context.hasFlopCBetOpportunity(), id);
        assertEquals(facedCBet, context.hasFacedFlopCBetOpportunity(), id);
    }

    static Stream<Arguments> validContexts() {
        return Stream.of(
                Arguments.of("SC-V01", PreflopRaiseType.NONE, false, false, false),
                Arguments.of("SC-V02", PreflopRaiseType.NONE, true, false, false),
                Arguments.of("SC-V03", PreflopRaiseType.OPENING_RAISE, false, false, false),
                Arguments.of("SC-V04", PreflopRaiseType.THREE_BET, true, false, false),
                Arguments.of("SC-V05", PreflopRaiseType.FOUR_BET, false, false, false),
                Arguments.of("SC-V06", PreflopRaiseType.LATER_RAISE, false, false, false),
                Arguments.of("SC-V07", PreflopRaiseType.NONE, false, true, false),
                Arguments.of("SC-V08", PreflopRaiseType.NONE, false, false, true));
    }
}
