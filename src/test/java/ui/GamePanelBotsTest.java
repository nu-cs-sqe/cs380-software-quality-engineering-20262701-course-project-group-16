package ui;

import static ui.UiComponents.find;

import application.BotController;
import application.BotFailure;
import application.BotScheduler;
import application.BotTurn;
import application.GameInitializer;
import application.SetupSession;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.GameConfig;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GamePanelBotsTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void ui01ThinkingDisablesHumanActionsAndReturnsThePrivateHandoff(int language)
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            Harness h = new Harness(language);
            h.start("1");
            assertEquals(1, h.jobs.turns.size());
            assertEquals(language == 0 ? "Bot is thinking…" : "电脑正在思考…", h.label("botStatus"));
            for (String name : List.of("fold", "check", "call", "wager")) {
                assertFalse(h.button(name).isEnabled());
            }
            assertEquals("--", h.label("hole0"));
            h.complete(0);
            h.complete(1);
            h.complete(2);
            assertEquals(3, h.session.getAcceptedActions().size());
            assertEquals(0, h.session.getView().orElseThrow().getCurrentActorSeat());
            assertTrue(h.button("call").isEnabled());
            assertTrue(h.label("botStatus").isBlank());
            assertEquals("--", h.label("hole0"));
            h.button("reveal").doClick();
            assertNotEquals("--", h.label("hole0"));
            h.panel.stopBots();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void ui02TimeoutRequiresExplicitRetryAndDoesNotAcceptOldCallbacks(int language)
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            Harness h = new Harness(language);
            h.start("1");
            h.jobs.failures.get(0).accept(BotFailure.TIMED_OUT);
            assertEquals(1, h.jobs.turns.size());
            assertTrue(h.button("retryBot").isVisible());
            assertEquals(language == 0 ? "Bot took too long. Please retry."
                    : "电脑思考超时，请重试。", h.label("error"));
            h.button("retryBot").doClick();
            assertEquals(2, h.jobs.turns.size());
            assertFalse(h.button("retryBot").isVisible());
            assertTrue(h.label("error").isBlank());
            h.complete(0);
            h.jobs.failures.get(0).accept(BotFailure.FAILED);
            assertTrue(h.session.getAcceptedActions().isEmpty());
            h.complete(1);
            assertEquals(1, h.session.getAcceptedActions().size());
            h.panel.stopBots();
        });
    }

    @Test
    void ui03RestartAndCloseIgnoreOldResultsWithoutConcealingNewCards()
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            Harness h = new Harness(0);
            h.start("1");
            h.language(1);
            assertEquals(1, h.jobs.turns.size());
            assertEquals("电脑正在思考…", h.label("botStatus"));
            h.start("6");
            assertEquals(1, h.jobs.turns.size());
            h.button("reveal").doClick();
            String cards = h.label("hole0");
            assertNotEquals("--", cards);
            h.complete(0);
            h.jobs.failures.get(0).accept(BotFailure.FAILED);
            assertEquals(cards, h.label("hole0"));
            assertTrue(h.session.getAcceptedActions().isEmpty());
            h.start("1");
            h.panel.stopBots();
            h.panel.stopBots();
            h.complete(1);
            assertEquals(1, h.jobs.closed);
            assertTrue(h.session.getAcceptedActions().isEmpty());
        });
    }

    @Test
    void ui04InvalidRestartRecapturesOldMatchAndKeepsTheValidationMessage()
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            Harness h = new Harness(0);
            h.start("1");
            String hand = h.jobs.turns.get(0).getView().getHandId();
            h.start("0");
            assertEquals(2, h.jobs.turns.size());
            assertEquals(hand, h.jobs.turns.get(1).getView().getHandId());
            assertEquals("Enter a whole number from 1 to 6.", h.label("error"));
            h.complete(0);
            assertTrue(h.session.getAcceptedActions().isEmpty());
            h.complete(1);
            assertEquals(1, h.session.getAcceptedActions().size());
            h.panel.stopBots();
        });
    }

    private static final class FirstDealer extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return 0; }
    }

    private static final class Harness {
        final SetupSession session = new SetupSession(new GameInitializer(new GameConfig(),
                new FirstDealer(), new Random(380)));
        final ManualScheduler jobs = new ManualScheduler();
        final GamePanel panel = new GamePanel(session,
                changed -> new BotController(session, jobs, changed));

        Harness(int language) { language(language); }
        void language(int index) {
            find(panel, "language", JComboBox.class).setSelectedIndex(index);
        }
        void start(String humans) {
            find(panel, "humanCount", JTextField.class).setText(humans);
            button("start").doClick();
        }
        JButton button(String name) { return find(panel, name, JButton.class); }
        String label(String name) { return find(panel, name, JLabel.class).getText(); }
        void complete(int index) {
            BotTurn turn = jobs.turns.get(index);
            ActionType type = turn.getLegalActions().getActions().contains(ActionType.CHECK)
                    ? ActionType.CHECK : ActionType.CALL;
            jobs.successes.get(index).accept(new ActionRequest(turn.getView().getViewerId(), type));
        }
    }

    private static final class ManualScheduler implements BotScheduler {
        final List<BotTurn> turns = new ArrayList<>();
        final List<Consumer<ActionRequest>> successes = new ArrayList<>();
        final List<Consumer<BotFailure>> failures = new ArrayList<>();
        int closed;
        @Override public void start(BotTurn turn, Consumer<ActionRequest> success,
                Consumer<BotFailure> failure) {
            turns.add(turn);
            successes.add(success);
            failures.add(failure);
        }
        @Override public void cancel() { }
        @Override public void close() { closed++; }
    }

}
