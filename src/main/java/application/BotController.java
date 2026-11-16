package application;

import domain.model.ActionRequest;
import java.util.Optional;

/** Owner-thread live-turn coordinator; the worker never accesses the session. */
public final class BotController implements AutoCloseable {
    private static final int SIMULATIONS = 512;
    private final BotSession session;
    private final BotScheduler scheduler;
    private final Runnable changed;
    private Optional<BotFailure> failure = Optional.empty();
    private Object generation = new Object();
    private boolean thinking;
    private boolean closed;

    public BotController(BotSession session, BotScheduler scheduler, Runnable changed) {
        this.session = session;
        this.scheduler = scheduler;
        this.changed = changed;
    }

    public void advance() {
        if (closed) {
            return;
        }
        if (thinking) {
            session.invalidateBotTurns();
        }
        scheduler.cancel();
        clearStatus();
        captureNext();
    }

    private void captureNext() {
        Optional<BotTurn> turn;
        try {
            session.playDepartureTurns();
            turn = session.getBotTurn(SIMULATIONS);
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            showFailure(BotFailure.FAILED);
            return;
        }
        turn.ifPresentOrElse(this::start, changed);
    }

    private void start(BotTurn turn) {
        thinking = true;
        Object expected = generation;
        changed.run();
        scheduler.start(turn, action -> completed(expected, turn, action),
                reason -> failed(expected, turn, reason));
    }

    private void completed(Object expected, BotTurn turn, ActionRequest action) {
        if (!current(expected)) {
            return;
        }
        try {
            session.applyBotDecision(turn, action);
        } catch (IllegalArgumentException | IllegalStateException
                | UnsupportedOperationException invalid) {
            showFailure(BotFailure.FAILED);
            return;
        }
        // Both acceptance and stale rejection recapture the owner's current turn.
        clearStatus();
        captureNext();
    }

    private void failed(Object expected, BotTurn turn, BotFailure reason) {
        if (!current(expected)) {
            return;
        }
        if (session.isCurrentBotTurn(turn)) {
            showFailure(reason);
        } else {
            clearStatus();
            captureNext();
        }
    }

    private boolean current(Object expected) {
        return !closed && generation == expected;
    }

    private void showFailure(BotFailure reason) {
        clearStatus();
        failure = Optional.of(reason);
        changed.run();
    }

    private void clearStatus() {
        generation = new Object();
        thinking = false;
        failure = Optional.empty();
    }

    public boolean isThinking() { return thinking; }
    public Optional<BotFailure> getFailure() { return failure; }

    public void cancel() {
        if (!closed) {
            scheduler.cancel();
            session.invalidateBotTurns();
            clearStatus();
            changed.run();
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            scheduler.close();
            session.invalidateBotTurns();
            clearStatus();
            changed.run();
        }
    }
}
