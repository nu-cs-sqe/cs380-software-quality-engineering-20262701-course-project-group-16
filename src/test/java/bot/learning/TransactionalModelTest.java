package bot.learning;

import domain.model.BettingStreet;
import domain.model.ResolvedActionType;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static bot.learning.LearningFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TransactionalModelTest {
    @Test
    void tm01ReturnsAnIndependentUpdatedModelAndPreservesSequence() {
        OpponentModel original = new OpponentModel("match", "hero", Set.of("p1"));
        original.observe(event(1));
        OpponentModel next = original.after(event(2));
        assertEquals(1, original.snapshot("p1").getObservedDecisions());
        assertEquals(2, next.snapshot("p1").getObservedDecisions());
        assertEquals(2, next.after(event(2)).snapshot("p1").getObservedDecisions());
        assertTrue(original.observe(event(2)));
        assertTrue(next.observe(event(3)));
        assertEquals(2, original.snapshot("p1").getObservedDecisions());
        assertEquals(3, next.snapshot("p1").getObservedDecisions());
    }

    @Test
    void tm02RejectedUpdateLeavesOriginalReadyForRetry() {
        OpponentModel original = new OpponentModel("match", "hero", Set.of("p1"));
        PublicDecision valid = event(1);
        PublicDecision foreign = new PublicDecision("foreign", 1, valid.getAction(),
                valid.getContext().orElseThrow(), DecisionOrigin.PLAYER_CHOICE);
        assertThrows(IllegalArgumentException.class,
                () -> assertNotSame(original, original.after(foreign)));
        assertEquals(0, original.snapshot("p1").getObservedDecisions());
        assertEquals(1, original.after(valid).snapshot("p1").getObservedDecisions());
    }

    private static PublicDecision event(long sequence) {
        return decision(sequence, "p1", BettingStreet.PREFLOP, ResolvedActionType.CALL, NONE,
                ResolvedActionType.CALL, ResolvedActionType.FOLD);
    }
}
