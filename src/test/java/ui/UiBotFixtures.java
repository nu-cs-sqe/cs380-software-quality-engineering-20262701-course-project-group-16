package ui;

import application.BotController;
import application.BotFailure;
import application.BotScheduler;
import application.BotTurn;
import application.ProfileService;
import application.SetupSession;
import application.TableAccountStore;
import domain.model.ActionRequest;
import domain.model.ActionType;
import java.util.function.Consumer;

/** Deterministic presentation fixture; real background integration has separate tests. */
final class UiBotFixtures {
    private UiBotFixtures() { }

    static GamePanel passive(SetupSession session) {
        return new GamePanel(session, changed -> new BotController(session,
                new PassiveScheduler(), changed));
    }

    static GamePanel passive(SetupSession session, ProfileService profiles) {
        return new GamePanel(session, profiles, changed -> new BotController(session,
                new PassiveScheduler(), changed));
    }

    static GamePanel passive(SetupSession session, ProfileService profiles,
            TableAccountStore store) {
        return new GamePanel(session, profiles, store, changed -> new BotController(session,
                new PassiveScheduler(), changed));
    }

    private static final class PassiveScheduler implements BotScheduler {
        @Override public void start(BotTurn turn, Consumer<ActionRequest> success,
                Consumer<BotFailure> failure) {
            ActionType type = turn.getLegalActions().getActions().contains(ActionType.CHECK)
                    ? ActionType.CHECK : ActionType.CALL;
            success.accept(new ActionRequest(turn.getView().getViewerId(), type));
        }
        @Override public void cancel() { }
        @Override public void close() { }
    }
}
