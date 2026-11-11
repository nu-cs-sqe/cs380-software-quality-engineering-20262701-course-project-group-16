package bot.learning;

import domain.model.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static bot.learning.LearningFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class OpponentModelTest {
    @Test
    void omC01CoordinatesOneOpponentUpdateAndCopiesRegistry() {
        OpponentBeliefs before = new OpponentBeliefs();
        OpponentBeliefs after = new OpponentBeliefs();
        Map<String, OpponentBeliefs> registry = new HashMap<>(Map.of("p1", before));
        List<OpponentBeliefs> supplied = new ArrayList<>();
        List<PublicDecision> delivered = new ArrayList<>();
        OpponentModel model = new OpponentModel("match", "hero", registry, (beliefs, event) -> {
            supplied.add(beliefs); delivered.add(event); return after;
        });
        registry.clear();
        PublicDecision first = call(1, "p1");
        assertTrue(model.observe(first));
        assertSame(after, model.snapshot("p1"));
        assertTrue(model.observe(call(2, "p1")));
        assertEquals(List.of(before, after), supplied);
        assertSame(first, delivered.get(0));
    }
    @Test
    void omC02SkipsOwnForcedDepartureAndReplayedDecisions() {
        AtomicInteger calls = new AtomicInteger();
        OpponentBeliefs empty = new OpponentBeliefs();
        OpponentModel model = new OpponentModel("match", "hero", Map.of("p1", empty), (a, b) -> {
            calls.incrementAndGet(); return empty;
        });
        assertFalse(model.observe(call(1, "hero")));
        assertFalse(model.observe(new PublicDecision("match", 2,
                action("p1", BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, NONE), DecisionOrigin.PLAYER_CHOICE)));
        assertFalse(model.observe(new PublicDecision("match", 3,
                action("p1", BettingStreet.PREFLOP, ResolvedActionType.FOLD, NONE), DecisionOrigin.DEPARTURE)));
        assertEquals(0, calls.get());
        assertTrue(model.observe(call(4, "p1")));
        assertFalse(model.observe(call(4, "p1")));
        assertFalse(model.observe(call(2, "p1")));
        assertEquals(1, calls.get());
    }
    @Test
    void omC03RejectsForeignOrMissingInputWithoutConsumingSequence() {
        AtomicInteger calls = new AtomicInteger();
        OpponentBeliefs empty = new OpponentBeliefs();
        OpponentModel model = new OpponentModel("match", "hero", Map.of("p1", empty), (a, b) -> {
            calls.incrementAndGet(); return empty;
        });
        PublicDecision valid = call(100, "p1");
        assertThrows(IllegalArgumentException.class, () -> model.observe(null));
        assertThrows(IllegalArgumentException.class, () -> model.observe(new PublicDecision("other", 100,
                valid.getAction(), valid.getContext().orElseThrow(), DecisionOrigin.PLAYER_CHOICE)));
        assertThrows(IllegalArgumentException.class, () -> model.observe(call(100, "unknown")));
        assertSame(empty, model.snapshot("p1"));
        assertThrows(IllegalArgumentException.class, () -> model.snapshot("unknown"));
        assertThrows(IllegalArgumentException.class, () -> model.snapshot(null));
        assertEquals(0, calls.get());
        assertTrue(model.observe(call(1, "p1")));
    }
    @Test
    void omC04FailedUpdateAndRetryPublishOnlyOnce() {
        OpponentBeliefs before = new OpponentBeliefs();
        OpponentBeliefs after = new OpponentBeliefs();
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("update failed");
        OpponentModel model = new OpponentModel("match", "hero", Map.of("p1", before), (a, b) -> {
            if (calls.incrementAndGet() == 1) throw failure;
            return after;
        });
        PublicDecision decision = call(10, "p1");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> model.observe(decision)));
        assertSame(before, model.snapshot("p1"));
        assertTrue(model.observe(decision));
        assertSame(after, model.snapshot("p1"));
        assertFalse(model.observe(decision));
        assertEquals(2, calls.get());
    }
    @Test
    void omC05ValidatesSixSeatRegistryAndCompositionBoundary() {
        assertDoesNotThrow(() -> new OpponentModel("match", "hero", Set.of()));
        assertDoesNotThrow(() -> new OpponentModel("match", "hero", Set.of("a", "b", "c", "d", "e")));
        for (String id : Arrays.asList(null, "", " ")) {
            assertThrows(IllegalArgumentException.class, () -> new OpponentModel(id, "hero", Set.of("p1")));
            assertThrows(IllegalArgumentException.class, () -> new OpponentModel("match", id, Set.of("p1")));
            assertThrows(IllegalArgumentException.class, () -> new OpponentModel("match", "hero", new HashSet<>(Arrays.asList(id))));
        }
        assertThrows(IllegalArgumentException.class, () -> new OpponentModel("match", "hero", (Set<String>) null));
        assertThrows(IllegalArgumentException.class, () -> new OpponentModel("match", "hero", Set.of("hero")));
        assertThrows(IllegalArgumentException.class, () -> new OpponentModel("match", "hero", Set.of("a", "b", "c", "d", "e", "f")));
        assertThrows(IllegalArgumentException.class, () -> new OpponentModel("match", "hero", null, (a, b) -> a));
        assertThrows(IllegalArgumentException.class, () -> new OpponentModel("match", "hero", Map.of("p1", new OpponentBeliefs()), null));
        Map<String, OpponentBeliefs> missing = new HashMap<>(); missing.put("p1", null);
        assertThrows(IllegalArgumentException.class, () -> new OpponentModel("match", "hero", missing, (a, b) -> a));
    }
    @Test
    void omC06AcceptsLastSequenceWithoutOverflowAndRejectsNullUpdaterResultAtomically() {
        OpponentBeliefs before = new OpponentBeliefs();
        AtomicInteger calls = new AtomicInteger();
        OpponentModel model = new OpponentModel("match", "hero", Map.of("p1", before), (a, b) -> calls.incrementAndGet() == 1 ? null : before);
        PublicDecision decision = call(Long.MAX_VALUE, "p1");
        assertThrows(NullPointerException.class, () -> model.observe(decision));
        assertSame(before, model.snapshot("p1"));
        assertTrue(model.observe(decision));
        assertFalse(model.observe(decision));
        assertEquals(2, calls.get());
    }
    private static PublicDecision call(long sequence, String player) {
        return decision(sequence, player, BettingStreet.PREFLOP, ResolvedActionType.CALL, NONE,
                ResolvedActionType.CALL, ResolvedActionType.FOLD);
    }
}
