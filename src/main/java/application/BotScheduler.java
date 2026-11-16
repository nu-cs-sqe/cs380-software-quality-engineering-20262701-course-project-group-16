package application;

import domain.model.ActionRequest;
import java.util.function.Consumer;

/** Owner-serialized scheduling port with explicit cancellation and resource ownership. */
public interface BotScheduler extends AutoCloseable {
    void start(BotTurn turn, Consumer<ActionRequest> accepted, Consumer<BotFailure> failed);
    void cancel();
    @Override void close();
}
