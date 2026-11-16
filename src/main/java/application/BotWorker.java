package application;

import domain.model.ActionRequest;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** One bounded background worker; all lifecycle calls and callbacks belong to the owner. */
public final class BotWorker implements BotScheduler {
    private static final int WORKERS = 1;
    private static final int QUEUE_CAPACITY = 1;
    private static final long DEFAULT_BUDGET = TimeUnit.SECONDS.toNanos(1);
    private final BiFunction<BotTurn, BooleanSupplier, ActionRequest> computer;
    private final Consumer<Runnable> execute;
    private final Runnable shutdown;
    private final Consumer<Runnable> owner;
    private final LongSupplier nanoTime;
    private final long budget;
    private Optional<AtomicBoolean> cancellation = Optional.empty();
    private Object generation = new Object();
    private volatile boolean closed;

    BotWorker(BiFunction<BotTurn, BooleanSupplier, ActionRequest> computer,
            Consumer<Runnable> execute, Runnable shutdown, Consumer<Runnable> owner,
            LongSupplier nanoTime, long budget) {
        requirePositiveDecisionBudget(budget);
        this.computer = computer;
        this.execute = execute;
        this.shutdown = shutdown;
        this.owner = owner;
        this.nanoTime = nanoTime;
        this.budget = budget;
    }

    private static void requirePositiveDecisionBudget(long budget) {
        if (budget <= 0) {
            throw new IllegalArgumentException("A positive decision budget is required.");
        }
    }

    public static BotWorker background(
            BiFunction<BotTurn, BooleanSupplier, ActionRequest> computer,
            Consumer<Runnable> owner) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(WORKERS, WORKERS, 0,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                BotWorker::daemonThread, new ThreadPoolExecutor.DiscardOldestPolicy());
        return new BotWorker(computer, pool::execute, pool::shutdownNow, owner,
                System::nanoTime, DEFAULT_BUDGET);
    }

    static Thread daemonThread(Runnable command) {
        Thread thread = new Thread(command, "poker-bot");
        thread.setDaemon(true);
        return thread;
    }

    @Override
    public void start(BotTurn turn, Consumer<ActionRequest> accepted, Consumer<BotFailure> failed) {
        if (closed) {
            throw new IllegalStateException("The bot worker is closed.");
        }
        cancel();
        AtomicBoolean cancelled = new AtomicBoolean();
        cancellation = Optional.of(cancelled);
        Object expected = generation;
        long started = nanoTime.getAsLong();
        BooleanSupplier stop = () -> cancelled.get() || Thread.currentThread().isInterrupted()
                || expired(started);
        Runnable job = () -> calculate(turn, stop, started, expected, accepted, failed);
        schedule(job, expected, failed);
    }

    private void schedule(Runnable job, Object expected, Consumer<BotFailure> failed) {
        try {
            execute.accept(job);
        } catch (RejectedExecutionException rejected) {
            deliver(expected, () -> failed.accept(BotFailure.FAILED));
        }
    }

    private void calculate(BotTurn turn, BooleanSupplier stop, long started, Object expected,
            Consumer<ActionRequest> accepted, Consumer<BotFailure> failed) {
        ActionRequest action;
        try {
            action = runningCalculation(turn, stop);
        } catch (CancellationException cancelled) {
            BotFailure reason = expired(started) ? BotFailure.TIMED_OUT : BotFailure.FAILED;
            deliver(expected, () -> failed.accept(reason));
            return;
        } catch (RuntimeException failure) {
            deliver(expected, () -> failed.accept(BotFailure.FAILED));
            return;
        }
        deliver(expected, () -> accepted.accept(action));
    }

    private ActionRequest runningCalculation(BotTurn turn, BooleanSupplier stop) {
        requireRunning(stop);
        ActionRequest action = computer.apply(turn, stop);
        requireRunning(stop);
        return action;
    }

    private static void requireRunning(BooleanSupplier stop) {
        if (stop.getAsBoolean()) {
            throw new CancellationException("This bot job is no longer current.");
        }
    }

    private boolean expired(long started) {
        return nanoTime.getAsLong() - started >= budget;
    }

    private void deliver(Object expected, Runnable completion) {
        owner.accept(() -> {
            if (!closed && generation == expected) {
                cancellation = Optional.empty();
                completion.run();
            }
        });
    }

    @Override
    public void cancel() {
        cancellation.ifPresent(flag -> flag.set(true));
        cancellation = Optional.empty();
        generation = new Object();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            cancel();
            shutdown.run();
        }
    }
}
