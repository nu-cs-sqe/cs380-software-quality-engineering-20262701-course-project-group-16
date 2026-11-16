package application;

import domain.model.*;
import java.util.Random;

final class BotFixtures {
    private BotFixtures() { }

    static GameInitializer initializer() {
        Random dealer = new Random(1) {
            @Override public int nextInt(int bound) { return 0; }
        };
        return new GameInitializer(new GameConfig(), dealer, new Random(380));
    }

    static SetupSession started(int humans) {
        SetupSession session = new SetupSession(initializer());
        session.start(humans);
        return session;
    }

    static String actor(SetupSession session) {
        SetupView view = session.getView().orElseThrow();
        return view.getPlayers().get(view.getCurrentActorSeat()).getId();
    }

    static ActionRequest passive(SetupSession session) {
        return new ActionRequest(actor(session),
                session.getAvailableActions().contains(ActionType.CHECK)
                        ? ActionType.CHECK : ActionType.CALL);
    }

    static void finish(SetupSession session) {
        int decisions = 0;
        while (session.getResult().isEmpty() && decisions++ < 100) {
            session.submitAction(passive(session));
        }
        if (session.getResult().isEmpty()) throw new AssertionError("Hand did not finish.");
    }
}
