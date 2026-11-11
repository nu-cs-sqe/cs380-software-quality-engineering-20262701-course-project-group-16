package bot.learning;

import domain.model.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static bot.learning.LearningFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class PublicDecisionTest {
    @Test
    void pdC01RetainsExactPublicEventAndPositiveSequence() {
        AcceptedAction action = action("opponent", BettingStreet.TURN, ResolvedActionType.CHECK, NONE);
        DecisionContext context = context(BettingStreet.TURN, true, ResolvedActionType.CHECK);
        PublicDecision decision = new PublicDecision(" match ", Long.MAX_VALUE, action, context, DecisionOrigin.PLAYER_CHOICE);
        assertEquals(" match ", decision.getMatchId());
        assertEquals(Long.MAX_VALUE, decision.getSequence());
        assertSame(action, decision.getAction());
        assertSame(context, decision.getContext().orElseThrow());
        assertTrue(decision.isStrategic());
    }
    @Test
    void pdC02RejectsMissingOrInconsistentBoundaries() {
        AcceptedAction action = action("opponent", BettingStreet.FLOP, ResolvedActionType.CHECK, NONE);
        DecisionContext context = context(BettingStreet.FLOP, false, ResolvedActionType.CHECK);
        for (String id : Arrays.asList(null, "", " ")) {
            assertThrows(IllegalArgumentException.class, () -> new PublicDecision(id, 1, action, context, DecisionOrigin.PLAYER_CHOICE));
        }
        for (long sequence : new long[] {-1, 0}) {
            assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", sequence, action, context, DecisionOrigin.PLAYER_CHOICE));
        }
        assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", 1, null, context, DecisionOrigin.PLAYER_CHOICE));
        assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", 1, action, null, DecisionOrigin.PLAYER_CHOICE));
        assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", 1, action, context, null));
        assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", 1, action,
                context(BettingStreet.TURN, false, ResolvedActionType.CHECK), DecisionOrigin.PLAYER_CHOICE));
        assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", 1, action,
                context(BettingStreet.FLOP, false, ResolvedActionType.BET), DecisionOrigin.PLAYER_CHOICE));
        assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", 1, action, context, DecisionOrigin.DEPARTURE));
    }
    @Test
    void pdC03SeparatesForcedAndDepartureEventsFromVoluntaryFolds() {
        DecisionContext context = context(BettingStreet.PREFLOP, false, ResolvedActionType.FOLD);
        AcceptedAction forced = action("opponent", BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, NONE);
        AcceptedAction fold = action("opponent", BettingStreet.PREFLOP, ResolvedActionType.FOLD, NONE);
        assertFalse(new PublicDecision("match", 1, forced, context, DecisionOrigin.PLAYER_CHOICE).isStrategic());
        assertFalse(new PublicDecision("match", 2, fold, context, DecisionOrigin.DEPARTURE).isStrategic());
        assertTrue(new PublicDecision("match", 3, fold, context, DecisionOrigin.PLAYER_CHOICE).isStrategic());
    }
    @ParameterizedTest
    @EnumSource(value = ResolvedActionType.class, names = {"CALL", "BET", "RAISE"})
    void pdC04AllInsRetainTheirResolvedDecisionMeaning(ResolvedActionType type) {
        StatsContext stats = type == ResolvedActionType.RAISE
                ? new StatsContext(PreflopRaiseType.THREE_BET, true, false, false) : NONE;
        BettingStreet street = type == ResolvedActionType.BET ? BettingStreet.FLOP : BettingStreet.PREFLOP;
        AcceptedAction action = new AcceptedAction("hand", "opponent", street, type, 20, 20, true, stats);
        PublicDecision decision = new PublicDecision("match", 1, action, context(street, false, type), DecisionOrigin.PLAYER_CHOICE);
        assertTrue(decision.isStrategic());
        assertEquals(type, decision.getAction().getType());
        assertTrue(decision.getAction().becameAllIn());
    }
    @Test
    void pdC05RejectsThreeBetOpportunityWithoutLegalRaise() {
        AcceptedAction call = action("p1", BettingStreet.PREFLOP, ResolvedActionType.CALL,
                new StatsContext(PreflopRaiseType.NONE, true, false, false));
        assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", 1, call,
                context(BettingStreet.PREFLOP, false, ResolvedActionType.CALL, ResolvedActionType.FOLD), DecisionOrigin.PLAYER_CHOICE));
    }
    @Test
    void pdC06RejectsCBetOpportunityWithoutLegalOpeningBet() {
        AcceptedAction check = action("p1", BettingStreet.FLOP, ResolvedActionType.CHECK,
                new StatsContext(PreflopRaiseType.NONE, false, true, false));
        assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", 1, check,
                context(BettingStreet.FLOP, false, ResolvedActionType.CHECK), DecisionOrigin.PLAYER_CHOICE));
    }

    @Test
    void pdC07ForcedPostNeedsNoInventedLegalChoiceContext() {
        PublicDecision event = new PublicDecision("match", 1,
                action("p1", BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, NONE), DecisionOrigin.PLAYER_CHOICE);
        assertTrue(event.getContext().isEmpty());
        assertFalse(event.isStrategic());
    }
    @Test
    void pdC08NonCurrentDepartureNeedsNoInventedLegalChoiceContext() {
        PublicDecision event = new PublicDecision("match", 1,
                action("p1", BettingStreet.TURN, ResolvedActionType.FOLD, NONE), DecisionOrigin.DEPARTURE);
        assertTrue(event.getContext().isEmpty());
        assertFalse(event.isStrategic());
    }
    @Test
    void pdC09OrdinaryChoiceCannotOmitItsPublicContext() {
        assertThrows(IllegalArgumentException.class, () -> new PublicDecision("match", 1,
                action("p1", BettingStreet.TURN, ResolvedActionType.CALL, NONE), DecisionOrigin.PLAYER_CHOICE));
    }

}
