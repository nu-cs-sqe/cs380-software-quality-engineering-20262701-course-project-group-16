package application;

import domain.model.ActionRequest;
import domain.model.ActionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BotWorkerTest {
    @Test
    void bw01ComputesAndDispatchesInSeparateControlledSteps() {
        Harness harness = new Harness(10);
        harness.start();
        assertEquals(1, harness.jobs.size());
        assertTrue(harness.actions.isEmpty());
        harness.runJob();
        assertEquals(1, harness.owner.size());
        assertTrue(harness.actions.isEmpty());
        harness.runOwner();
        assertEquals(1, harness.actions.size());
        assertEquals(1, harness.computations);
        assertTrue(harness.errors.isEmpty());
    }

    @Test
    void bw02ReplacementAndCancellationSuppressOldCompletions() {
        Harness harness = new Harness(10);
        harness.start();
        harness.runJob();
        harness.start();
        harness.runOwner();
        assertTrue(harness.actions.isEmpty());
        harness.runJob();
        harness.runOwner();
        assertEquals(1, harness.actions.size());
        harness.start();
        harness.worker.cancel();
        harness.runJob();
        harness.runOwner();
        assertEquals(2, harness.computations);
        assertEquals(1, harness.actions.size());
        assertTrue(harness.errors.isEmpty());
    }

    @Test
    void bw03CancellationDuringComputationIsAlsoIgnored() {
        Harness harness = new Harness(10);
        harness.cancelInside = true;
        harness.start();
        harness.runJob();
        harness.runOwner();
        assertTrue(harness.actions.isEmpty());
        assertTrue(harness.errors.isEmpty());
    }

    @Test
    void bw04DeadlineBoundariesAndNanoTimeWrapAreDeterministic() {
        for (long elapsed : new long[] {9, 10, 11}) {
            Harness harness = new Harness(10);
            harness.start();
            harness.clock.set(elapsed);
            harness.runJob();
            harness.runOwner();
            assertEquals(elapsed < 10 ? 1 : 0, harness.actions.size());
            assertEquals(elapsed < 10 ? List.of() : List.of(BotFailure.TIMED_OUT),
                    harness.errors);
        }
        Harness wrapped = new Harness(10);
        wrapped.clock.set(Long.MAX_VALUE - 5);
        wrapped.start();
        wrapped.clock.set(Long.MIN_VALUE + 4);
        wrapped.runJob();
        wrapped.runOwner();
        assertEquals(List.of(BotFailure.TIMED_OUT), wrapped.errors);
        Harness late = new Harness(10);
        late.expireInside = true;
        late.start();
        late.runJob();
        late.runOwner();
        assertEquals(List.of(BotFailure.TIMED_OUT), late.errors);
        assertTrue(late.actions.isEmpty());
    }

    @Test
    void bw08ThreadInterruptionCancelsBeforeComputation() {
        Harness harness = new Harness(10);
        harness.start();
        Thread.currentThread().interrupt();
        try {
            harness.runJob();
        } finally {
            Thread.interrupted();
        }
        harness.runOwner();
        assertEquals(0, harness.computations);
        assertEquals(List.of(BotFailure.FAILED), harness.errors);
    }

    @Test
    void bw05CalculationAndSchedulingFailuresAreDeliveredToTheOwner() {
        Harness harness = new Harness(10);
        for (Runnable failure : List.<Runnable>of(
                () -> { throw new IllegalStateException("failed"); },
                () -> { throw new CancellationException("unexpected cancellation"); })) {
            harness.problem = Optional.of(failure);
            harness.start();
            harness.runJob();
            harness.runOwner();
        }
        assertEquals(List.of(BotFailure.FAILED, BotFailure.FAILED), harness.errors);
        List<Runnable> owner = new ArrayList<>();
        List<BotFailure> errors = new ArrayList<>();
        BotWorker rejected = new BotWorker((turn, stop) -> action(turn), command -> {
            throw new RejectedExecutionException("closed scheduler");
        }, () -> { }, owner::add, () -> 0, 10);
        rejected.start(turn(), ignored -> fail("No accepted computation"), errors::add);
        assertTrue(errors.isEmpty());
        owner.remove(0).run();
        assertEquals(List.of(BotFailure.FAILED), errors);
    }

    @Test
    void bw06CloseIsIdempotentAndRejectsNewWork() {
        Harness harness = new Harness(10);
        harness.start();
        harness.worker.close();
        harness.worker.close();
        harness.runJob();
        harness.runOwner();
        assertTrue(harness.actions.isEmpty());
        assertTrue(harness.errors.isEmpty());
        assertEquals(1, harness.stops.get());
        assertEquals(0, harness.computations);
        assertThrows(IllegalStateException.class, harness::start);
        harness.worker.cancel();
        assertEquals(1, harness.stops.get());
        for (long budget : new long[] {-1, 0}) {
            assertThrows(IllegalArgumentException.class, () -> new Harness(budget));
        }
        Harness one = new Harness(1);
        one.start();
        one.runJob();
        one.runOwner();
        assertEquals(1, one.actions.size());
    }

    @Test
    void bw07ThreadFactoryCreatesAnIdleNamedDaemon() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean();
        List<Thread> children = new ArrayList<>();
        Thread parent = new Thread(() ->
                children.add(BotWorker.daemonThread(() -> ran.set(true))));
        parent.setDaemon(false);
        parent.start();
        parent.join(2000);
        assertFalse(parent.isAlive());
        assertEquals(1, children.size());
        Thread thread = children.get(0);
        assertTrue(thread.isDaemon());
        assertEquals("poker-bot", thread.getName());
        assertEquals(Thread.State.NEW, thread.getState());
        assertFalse(ran.get());
    }

    private static BotTurn turn() {
        return BotFixtures.started(1).getBotTurn(1).orElseThrow();
    }

    private static ActionRequest action(BotTurn turn) {
        return new ActionRequest(turn.getView().getViewerId(), ActionType.CALL);
    }

    private static final class Harness {
        final List<Runnable> jobs = new ArrayList<>();
        final List<Runnable> owner = new ArrayList<>();
        final List<ActionRequest> actions = new ArrayList<>();
        final List<BotFailure> errors = new ArrayList<>();
        final AtomicLong clock = new AtomicLong();
        final AtomicInteger stops = new AtomicInteger();
        final BotWorker worker;
        Optional<Runnable> problem = Optional.empty();
        boolean cancelInside;
        boolean expireInside;
        int computations;

        Harness(long budget) {
            worker = new BotWorker(this::compute, jobs::add, stops::incrementAndGet,
                    owner::add, clock::get, budget);
        }

        private ActionRequest compute(BotTurn turn, java.util.function.BooleanSupplier stop) {
            assertFalse(stop.getAsBoolean());
            computations++;
            if (cancelInside) worker.cancel();
            if (expireInside) clock.set(10);
            problem.ifPresent(Runnable::run);
            return action(turn);
        }

        void start() { worker.start(turn(), actions::add, errors::add); }
        void runJob() { jobs.remove(0).run(); }
        void runOwner() { owner.remove(0).run(); }
    }
}
