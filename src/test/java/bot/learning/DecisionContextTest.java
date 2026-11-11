package bot.learning;

import domain.model.BettingStreet;
import domain.model.ResolvedActionType;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecisionContextTest {
    @Test
    void dcC01PreservesPublicContextWithoutAliasing() {
        Set<ResolvedActionType> source = new HashSet<>(Set.of(ResolvedActionType.FOLD, ResolvedActionType.CALL));
        DecisionContext context = new DecisionContext(BettingStreet.TURN, source, true);
        source.clear();
        assertEquals(BettingStreet.TURN, context.getStreet());
        assertTrue(context.isInPosition());
        assertEquals(Set.of(ResolvedActionType.FOLD, ResolvedActionType.CALL), context.getLegalActions());
        assertThrows(UnsupportedOperationException.class, () -> context.getLegalActions().clear());
    }
    @Test
    void dcC02RejectsMissingOrNonDecisionChoices() {
        assertThrows(IllegalArgumentException.class, () -> new DecisionContext(null, Set.of(ResolvedActionType.CHECK), false));
        assertThrows(IllegalArgumentException.class, () -> new DecisionContext(BettingStreet.FLOP, null, false));
        for (Set<ResolvedActionType> choices : List.of(Set.<ResolvedActionType>of(),
                Set.of(ResolvedActionType.FORCED_POST), new HashSet<>(Arrays.asList((ResolvedActionType) null)))) {
            assertThrows(IllegalArgumentException.class, () -> new DecisionContext(BettingStreet.FLOP, choices, false));
        }
    }
    @Test
    void dcC03MatchesStreetChoicesAndPositionByValue() {
        DecisionContext base = new DecisionContext(BettingStreet.FLOP, Set.of(ResolvedActionType.CHECK), false);
        assertFalse(base.isInPosition());
        assertTrue(base.matches(new DecisionContext(BettingStreet.FLOP, Set.of(ResolvedActionType.CHECK), false)));
        assertFalse(base.matches(new DecisionContext(BettingStreet.TURN, Set.of(ResolvedActionType.CHECK), false)));
        assertFalse(base.matches(new DecisionContext(BettingStreet.FLOP, Set.of(ResolvedActionType.BET), false)));
        assertFalse(base.matches(new DecisionContext(BettingStreet.FLOP, Set.of(ResolvedActionType.CHECK), true)));
    }
}
