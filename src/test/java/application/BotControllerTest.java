package application;

import domain.model.AcceptedAction;
import domain.model.ActionRequest;
import domain.model.ActionType;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BotControllerTest {
    @Test
    void ct01WaitsForAJobThenContinuesOnlyForAnotherBot() {
        Harness harness = new Harness();
        harness.session.current = Optional.empty();
        harness.controller.advance();
        assertFalse(harness.controller.isThinking());
        assertTrue(harness.scheduler.jobs.isEmpty());
        harness.session.current = Optional.of(turn());
        harness.session.next = Optional.of(turn());
        harness.controller.advance();
        assertTrue(harness.controller.isThinking());
        assertTrue(harness.states.contains("thinking"));
        harness.scheduler.succeed(0);
        assertTrue(harness.controller.isThinking());
        assertEquals(2, harness.scheduler.jobs.size());
        harness.scheduler.succeed(1);
        assertFalse(harness.controller.isThinking());
        assertEquals(2, harness.session.applied);
        assertTrue(harness.controller.getFailure().isEmpty());
        assertTrue(harness.changed > 0);
    }

    @Test
    void ct02StaleSuccessAndFailureDoNotAffectTheCurrentMatch() {
        Harness success = new Harness();
        success.controller.advance();
        success.session.current = Optional.empty();
        success.scheduler.succeed(0);
        assertEquals(0, success.session.applied);
        assertFalse(success.controller.isThinking());
        assertEquals("idle", success.states.get(success.states.size() - 1));
        Harness failure = new Harness();
        failure.controller.advance();
        failure.session.current = Optional.empty();
        failure.scheduler.fail(0, BotFailure.TIMED_OUT);
        assertTrue(failure.controller.getFailure().isEmpty());
        assertEquals(0, failure.session.applied);
        assertFalse(failure.controller.isThinking());
        assertEquals("idle", failure.states.get(failure.states.size() - 1));
    }

    @Test
    void ct03FailureWaitsForAnExplicitRetryWithoutFallback() {
        Harness harness = new Harness();
        harness.controller.advance();
        harness.scheduler.fail(0, BotFailure.TIMED_OUT);
        assertEquals(Optional.of(BotFailure.TIMED_OUT), harness.controller.getFailure());
        assertFalse(harness.controller.isThinking());
        assertEquals("TIMED_OUT", harness.states.get(harness.states.size() - 1));
        assertEquals(0, harness.session.applied);
        harness.controller.advance();
        assertTrue(harness.controller.getFailure().isEmpty());
        assertTrue(harness.controller.isThinking());
        harness.scheduler.succeed(1);
        assertEquals(1, harness.session.applied);
    }

    @Test
    void ct04ApplicationFailuresAreRetryableAndPublishNoInventedAction() {
        Harness capture = new Harness();
        capture.session.badCapture = true;
        capture.controller.advance();
        assertEquals(Optional.of(BotFailure.FAILED), capture.controller.getFailure());
        assertTrue(capture.scheduler.jobs.isEmpty());
        Harness apply = new Harness();
        apply.controller.advance();
        apply.session.badApply = true;
        apply.scheduler.succeed(0);
        assertEquals(Optional.of(BotFailure.FAILED), apply.controller.getFailure());
        assertEquals(0, apply.session.applied);
        assertFalse(apply.controller.isThinking());
        assertEquals("FAILED", apply.states.get(apply.states.size() - 1));
        assertEquals(1, apply.scheduler.jobs.size());
    }

    @Test
    void ct05ReplacementCancelAndCloseSuppressLateCallbacks() {
        Harness harness = new Harness();
        harness.controller.advance();
        int beforeReplacement = harness.scheduler.cancels;
        harness.controller.advance();
        assertTrue(harness.scheduler.cancels > beforeReplacement);
        assertFalse(harness.session.isCurrentBotTurn(harness.scheduler.turns.get(0)));
        harness.scheduler.succeed(0);
        assertTrue(harness.controller.isThinking());
        assertEquals(0, harness.session.applied);
        int beforeCancel = harness.scheduler.cancels;
        int beforeNotice = harness.changed;
        harness.controller.cancel();
        assertTrue(harness.scheduler.cancels > beforeCancel);
        assertTrue(harness.changed > beforeNotice);
        assertEquals("idle", harness.states.get(harness.states.size() - 1));
        assertFalse(harness.session.isCurrentBotTurn(harness.scheduler.turns.get(1)));
        harness.scheduler.fail(1, BotFailure.FAILED);
        assertTrue(harness.controller.getFailure().isEmpty());
        harness.scheduler.succeed(1);
        assertEquals(0, harness.session.applied);
        harness.controller.advance();
        beforeNotice = harness.changed;
        harness.controller.close();
        assertTrue(harness.changed > beforeNotice);
        assertFalse(harness.session.isCurrentBotTurn(harness.scheduler.turns.get(2)));
        harness.controller.close();
        harness.scheduler.succeed(2);
        harness.scheduler.fail(2, BotFailure.FAILED);
        harness.controller.advance();
        harness.controller.cancel();
        assertEquals(1, harness.scheduler.closes);
        assertFalse(harness.controller.isThinking());
        assertEquals(3, harness.scheduler.jobs.size());
        assertEquals(0, harness.session.applied);
    }

    private static BotTurn turn() {
        return BotFixtures.started(1).getBotTurn(64).orElseThrow();
    }

    private static final class Harness {
        final FakeSession session = new FakeSession();
        final FakeScheduler scheduler = new FakeScheduler();
        int changed;
        final List<String> states = new ArrayList<>();
        final BotController controller = new BotController(session, scheduler, this::observe);

        private void observe() {
            changed++;
            states.add(controller.isThinking() ? "thinking"
                    : controller.getFailure().map(Enum::name).orElse("idle"));
        }
    }

    private static final class FakeSession implements BotSession {
        Optional<BotTurn> current = Optional.of(turn());
        Optional<BotTurn> next = Optional.empty();
        boolean badCapture;
        boolean badApply;
        int applied;

        @Override public Optional<BotTurn> getBotTurn(int trials) {
            assertEquals(512, trials);
            if (badCapture) throw new IllegalStateException("Cannot preview this hand");
            return current;
        }

        @Override public boolean isCurrentBotTurn(BotTurn turn) {
            return current.filter(value -> value == turn).isPresent();
        }

        @Override public boolean applyBotDecision(BotTurn turn, ActionRequest action) {
            if (badApply) throw new IllegalArgumentException("Rejected engine request");
            if (!isCurrentBotTurn(turn)) return false;
            assertEquals(turn.getView().getViewerId(), action.getPlayerId());
            applied++;
            current = next;
            next = Optional.empty();
            return true;
        }

        @Override public void invalidateBotTurns() {
            current = current.map(value -> new BotTurn(value.getMatchId(),
                    value.getRevision() + 1, value.getView(), value.getLegalActions(),
                    value.getScenario(), value.getCallRefund(), value.getBeliefs(),
                    value.getPersonality()));
        }

        @Override public List<AcceptedAction> playDepartureTurns() { return List.of(); }
    }

    private static final class FakeScheduler implements BotScheduler {
        final List<BotTurn> turns = new ArrayList<>();
        final List<Consumer<ActionRequest>> jobs = new ArrayList<>();
        final List<Consumer<BotFailure>> failures = new ArrayList<>();
        int closes;
        int cancels;

        @Override public void start(BotTurn turn, Consumer<ActionRequest> action,
                Consumer<BotFailure> failed) {
            turns.add(turn);
            jobs.add(action);
            failures.add(failed);
        }

        void succeed(int index) {
            jobs.get(index).accept(new ActionRequest(turns.get(index).getView().getViewerId(),
                    ActionType.CALL));
        }

        void fail(int index, BotFailure reason) { failures.get(index).accept(reason); }
        @Override public void cancel() { cancels++; }
        @Override public void close() { closes++; }
    }
}
