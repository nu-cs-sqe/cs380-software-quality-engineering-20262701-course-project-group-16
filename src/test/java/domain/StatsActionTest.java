package domain;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatsActionTest {

    @ParameterizedTest(name = "{0}: rejects inconsistent resolved actions")
    @MethodSource("invalidActions")
    void rejectsInvalidActions(String id, BettingStreet street, ResolvedActionType type,
            long chips, StatsContext context) {
        assertThrows(IllegalArgumentException.class,
                () -> new StatsAction(true, street, type, chips, context), id);
    }

    static Stream<Arguments> invalidActions() {
        StatsContext none = context(PreflopRaiseType.NONE, false, false, false);
        StatsContext open = context(PreflopRaiseType.OPENING_RAISE, false, false, false);
        StatsContext threeOpportunity = context(PreflopRaiseType.NONE, true, false, false);
        StatsContext cBet = context(PreflopRaiseType.NONE, false, true, false);
        StatsContext faced = context(PreflopRaiseType.NONE, false, false, true);
        return Stream.of(
                Arguments.of("SA-I01", null, ResolvedActionType.CHECK, 0L, none),
                Arguments.of("SA-I02", BettingStreet.PREFLOP, null, 0L, none),
                Arguments.of("SA-I03", BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0L, null),
                Arguments.of("SA-I04", BettingStreet.PREFLOP, ResolvedActionType.CALL, Long.MIN_VALUE, none),
                Arguments.of("SA-I05", BettingStreet.PREFLOP, ResolvedActionType.CALL, -1L, none),
                Arguments.of("SA-I06", BettingStreet.PREFLOP, ResolvedActionType.CHECK, 1L, none),
                Arguments.of("SA-I07", BettingStreet.PREFLOP, ResolvedActionType.FOLD, 1L, none),
                Arguments.of("SA-I08", BettingStreet.PREFLOP, ResolvedActionType.CALL, 0L, none),
                Arguments.of("SA-I09", BettingStreet.FLOP, ResolvedActionType.BET, 0L, none),
                Arguments.of("SA-I10", BettingStreet.PREFLOP, ResolvedActionType.RAISE, 0L, open),
                Arguments.of("SA-I11", BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, 0L, none),
                Arguments.of("SA-I12", BettingStreet.PREFLOP, ResolvedActionType.BET, 1L, none),
                Arguments.of("SA-I13", BettingStreet.FLOP, ResolvedActionType.FORCED_POST, 1L, none),
                Arguments.of("SA-I14", BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, none),
                Arguments.of("SA-I15", BettingStreet.PREFLOP, ResolvedActionType.CALL, 1L, open),
                Arguments.of("SA-I16", BettingStreet.FLOP, ResolvedActionType.RAISE, 1L, open),
                Arguments.of("SA-I17", BettingStreet.FLOP, ResolvedActionType.CALL, 1L, threeOpportunity),
                Arguments.of("SA-I18", BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0L, threeOpportunity),
                Arguments.of("SA-I19", BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, 1L, threeOpportunity),
                Arguments.of("SA-I20", BettingStreet.TURN, ResolvedActionType.BET, 1L, cBet),
                Arguments.of("SA-I21", BettingStreet.PREFLOP, ResolvedActionType.CALL, 1L, faced),
                Arguments.of("SA-I22", BettingStreet.RIVER, ResolvedActionType.FOLD, 0L, faced),
                Arguments.of("SA-I23", BettingStreet.FLOP, ResolvedActionType.CALL, 1L, cBet),
                Arguments.of("SA-I24", BettingStreet.FLOP, ResolvedActionType.RAISE, 1L, cBet),
                Arguments.of("SA-I25", BettingStreet.FLOP, ResolvedActionType.CHECK, 0L, faced),
                Arguments.of("SA-I26", BettingStreet.FLOP, ResolvedActionType.BET, 1L, faced),
                Arguments.of("SA-I27", BettingStreet.PREFLOP, ResolvedActionType.FOLD, 0L, cBet),
                Arguments.of("SA-I28", BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0L, faced));
    }

    @ParameterizedTest(name = "{0}: retains an engine-resolved action")
    @MethodSource("validActions")
    void retainsValidActions(String id, boolean accepted, BettingStreet street,
            ResolvedActionType type, long chips, StatsContext context) {
        StatsAction action = new StatsAction(accepted, street, type, chips, context);

        assertEquals(accepted, action.isAccepted(), id);
        assertEquals(street, action.getStreet(), id);
        assertEquals(type, action.getType(), id);
        assertEquals(chips, action.getChipsAdded(), id);
        assertEquals(context.getPreflopRaiseType(), action.getContext().getPreflopRaiseType(), id);
        assertEquals(context.hasThreeBetOpportunity(), action.getContext().hasThreeBetOpportunity(), id);
        assertEquals(context.hasFlopCBetOpportunity(), action.getContext().hasFlopCBetOpportunity(), id);
        assertEquals(context.hasFacedFlopCBetOpportunity(), action.getContext().hasFacedFlopCBetOpportunity(), id);
    }

    static Stream<Arguments> validActions() {
        StatsContext none = context(PreflopRaiseType.NONE, false, false, false);
        StatsContext open = context(PreflopRaiseType.OPENING_RAISE, false, false, false);
        StatsContext three = context(PreflopRaiseType.THREE_BET, true, false, false);
        StatsContext four = context(PreflopRaiseType.FOUR_BET, false, false, false);
        StatsContext opportunity = context(PreflopRaiseType.NONE, true, false, false);
        StatsContext cBet = context(PreflopRaiseType.NONE, false, true, false);
        StatsContext faced = context(PreflopRaiseType.NONE, false, false, true);
        return Stream.of(
                Arguments.of("SA-V01", true, BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, 1L, none),
                Arguments.of("SA-V02", true, BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0L, none),
                Arguments.of("SA-V03", true, BettingStreet.PREFLOP, ResolvedActionType.CALL, 1L, none),
                Arguments.of("SA-V04", true, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, open),
                Arguments.of("SA-V05", true, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, three),
                Arguments.of("SA-V06", true, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, four),
                Arguments.of("SA-V07", true, BettingStreet.FLOP, ResolvedActionType.BET, 1L, none),
                Arguments.of("SA-V08", true, BettingStreet.TURN, ResolvedActionType.RAISE, 1L, none),
                Arguments.of("SA-V09", true, BettingStreet.RIVER, ResolvedActionType.CALL, Long.MAX_VALUE, none),
                Arguments.of("SA-V10", true, BettingStreet.RIVER, ResolvedActionType.FOLD, 0L, none),
                Arguments.of("SA-V11", false, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, open),
                Arguments.of("SA-V12", true, BettingStreet.FLOP, ResolvedActionType.CHECK, 0L, cBet),
                Arguments.of("SA-V13", true, BettingStreet.FLOP, ResolvedActionType.CALL, 1L, faced),
                Arguments.of("SA-V14", true, BettingStreet.FLOP, ResolvedActionType.FOLD, 0L, faced),
                Arguments.of("SA-V15", true, BettingStreet.PREFLOP, ResolvedActionType.CALL, 1L, opportunity),
                Arguments.of("SA-V16", true, BettingStreet.PREFLOP, ResolvedActionType.FOLD, 0L, opportunity),
                Arguments.of("SA-V17", true, BettingStreet.FLOP, ResolvedActionType.FOLD, 0L, cBet));
    }

    private static StatsContext context(PreflopRaiseType type, boolean three,
            boolean cBet, boolean faced) {
        return new StatsContext(type, three, cBet, faced);
    }
}
