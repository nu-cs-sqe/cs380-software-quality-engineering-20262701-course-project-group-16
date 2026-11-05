package ui;

import application.GameInitializer;
import application.SetupSession;
import application.SetupView;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.ShowdownResult;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PlayedGamePanelTest {
    @Test
    void noActionControlsAreEnabledBeforeStart() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GamePanel panel = new GamePanel(new SetupSession());
            for (String name : new String[]{"fold", "check", "call"}) {
                assertFalse(find(panel, name, JButton.class).isEnabled());
            }
        });
    }

    @ParameterizedTest(name = "UI-PLAY01-{0}: bots and private human handoff")
    @ValueSource(ints = {0, 1})
    void humanActionsDriveBotsAndTheFlopWithoutExposingCards(int language) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session(new GameConfig());
            GamePanel panel = panel(session, language);
            start(panel, 2);
            assertEquals(0, view(session).getCurrentActorSeat());
            assertEquals(90, view(session).getPot());
            assertEquals(language == 0 ? "Call 20" : "跟注 20", find(panel, "call", JButton.class).getText());
            assertTrue(find(panel, "fold", JButton.class).isEnabled());
            assertFalse(find(panel, "check", JButton.class).isEnabled());
            find(panel, "reveal", JButton.class).doClick();
            assertNotEquals("--", find(panel, "hole0", JLabel.class).getText());
            find(panel, "call", JButton.class).doClick();
            assertEquals(1, view(session).getCurrentActorSeat());
            assertEquals(1, view(session).getViewerSeat());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertEquals(language == 0 ? "Call 10" : "跟注 10", find(panel, "call", JButton.class).getText());
            find(panel, "call", JButton.class).doClick();
            assertEquals(GamePhase.FLOP, view(session).getPhase());
            assertEquals(3, view(session).getCommunityCards().size());
            assertEquals(120, view(session).getPot());
            assertEquals(language == 0 ? "Flop" : "翻牌", find(panel, "phase", JLabel.class).getText());
            assertEquals(language == 0 ? "Chips: 980" : "筹码：980", find(panel, "seatStack1", JLabel.class).getText());
            assertTrue(find(panel, "check", JButton.class).isEnabled());
            assertFalse(find(panel, "call", JButton.class).isEnabled());
            assertEquals(6, session.getAcceptedActions().size());
            for (int seat = 0; seat < 6; seat++) assertEquals("▧  ▧", find(panel, "seatCards" + seat, JLabel.class).getText());
        });
    }

    @ParameterizedTest(name = "UI-PLAY02-{0}: full played hand in selected language")
    @ValueSource(ints = {0, 1})
    void controlledHandShowsRealTieAndAwaitingSettlement(int language) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session(new GameConfig());
            GamePanel panel = panel(session, language);
            start(panel, 6);
            int[] seats = {3, 4, 5, 0, 1, 2};
            for (int seat : seats) {
                assertEquals(seat, view(session).getCurrentActorSeat());
                String action = seat == 4 ? "fold" : seat == 2 ? "check" : "call";
                assertTrue(find(panel, action, JButton.class).isEnabled());
                find(panel, action, JButton.class).doClick();
            }
            for (int index = 0; index < 15; index++) {
                assertTrue(find(panel, "check", JButton.class).isEnabled());
                find(panel, "check", JButton.class).doClick();
            }
            ShowdownResult result = session.getResult().orElseThrow();
            assertEquals(5, result.getWinnerIds().size());
            assertEquals(21, session.getAcceptedActions().size());
            assertEquals(100, view(session).getPot());
            assertEquals(5900, view(session).getPlayers().stream().mapToLong(player -> player.getChipStack()).sum());
            assertEquals(GamePhase.SHOWDOWN, view(session).getPhase());
            assertEquals(language == 0 ? "Folded" : "已弃牌", find(panel, "seatStatus4", JLabel.class).getText());
            assertTrue(find(panel, "result", JLabel.class).getText().contains(language == 0 ? "Result awaiting settlement" : "结果等待结算"));
            assertTrue(find(panel, "result", JLabel.class).getText().contains(language == 0 ? "Human 6" : "玩家 6"));
            assertTrue(find(panel, "community", JLabel.class).getText().contains("8 ♦"));
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            for (String name : new String[]{"fold", "check", "call"}) assertFalse(find(panel, name, JButton.class).isEnabled());
            find(panel, "language", JComboBox.class).setSelectedIndex(1 - language);
            assertTrue(find(panel, "result", JLabel.class).getText().contains(language == 1 ? "Result awaiting settlement" : "结果等待结算"));
            assertSame(result, session.getResult().orElseThrow());
            assertEquals(21, session.getAcceptedActions().size());
            start(panel, 6);
            assertTrue(session.getResult().isEmpty());
            assertEquals(" ", find(panel, "result", JLabel.class).getText());
            assertTrue(session.getAcceptedActions().isEmpty());
        });
    }

    @ParameterizedTest(name = "UI-PLAY03-{0}: uncontested result without drawing")
    @ValueSource(ints = {0, 1})
    void foldsShowUncontestedResultWithoutPaying(int language) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session(new GameConfig());
            GamePanel panel = panel(session, language);
            start(panel, 6);
            for (int count = 0; count < 5; count++) find(panel, "fold", JButton.class).doClick();
            assertTrue(session.getResult().orElseThrow().isUncontested());
            assertEquals(40, view(session).getRemainingCardCount());
            assertEquals(30, view(session).getPot());
            assertTrue(find(panel, "result", JLabel.class).getText().contains(language == 0 ? "wins uncontested" : "其他玩家均弃牌"));
            assertTrue(find(panel, "result", JLabel.class).getText().contains(language == 0 ? "Human 3" : "玩家 3"));
            assertFalse(find(panel, "fold", JButton.class).isEnabled());
        });
    }

    @ParameterizedTest(name = "UI-PLAY04-{0}: all-in runout")
    @ValueSource(ints = {0, 1})
    void oneHumanAndBotsCanReachAnAllInComparison(int language) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session(new GameConfig(20, 10, 20));
            GamePanel panel = panel(session, language);
            start(panel, 1);
            find(panel, "call", JButton.class).doClick();
            assertEquals(GamePhase.SHOWDOWN, view(session).getPhase());
            assertEquals(120, view(session).getPot());
            assertEquals(5, session.getAcceptedActions().size());
            assertEquals(language == 0 ? "All-in" : "全下", find(panel, "seatStatus0", JLabel.class).getText());
            assertEquals(5, view(session).getCommunityCards().size());
            assertTrue(find(panel, "result", JLabel.class).getText().contains(language == 0 ? "Result awaiting settlement" : "结果等待结算"));
        });
    }

    @Test
    void selectingAnotherViewerDisablesActionsUntilTheActorIsSelected() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session(new GameConfig());
            GamePanel panel = panel(session, 0);
            start(panel, 6);
            assertEquals(3, view(session).getViewerSeat());
            find(panel, "viewers", JComboBox.class).setSelectedIndex(0);
            for (String name : new String[]{"fold", "check", "call"}) assertFalse(find(panel, name, JButton.class).isEnabled());
            find(panel, "viewers", JComboBox.class).setSelectedIndex(3);
            assertTrue(find(panel, "call", JButton.class).isEnabled());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
        });
    }

    @ParameterizedTest(name = "UI-PLAY06-{0}: all action buttons fit with larger fonts")
    @ValueSource(ints = {0, 1})
    void callButtonStaysVisibleInsideTheActionRow(int language) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GamePanel panel = panel(session(new GameConfig()), language);
            start(panel, 2);
            for (String name : new String[]{"fold", "check", "call"}) {
                find(panel, name, JButton.class).setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
            }
            panel.setSize(1100, 740);
            layoutTree(panel);
            for (String name : new String[]{"fold", "check", "call"}) {
                JButton button = find(panel, name, JButton.class);
                Container row = button.getParent();
                assertTrue(button.getWidth() > 0 && button.getHeight() > 0);
                assertTrue(button.getX() >= 0 && button.getY() >= 0
                        && button.getX() + button.getWidth() <= row.getWidth()
                        && button.getY() + button.getHeight() <= row.getHeight(),
                        name + " must stay visible inside its action row");
                int textWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
                assertTrue(textWidth + button.getInsets().left + button.getInsets().right <= button.getWidth(),
                        name + " label must fit without clipping");
            }
            assertTrue(find(panel, "call", JButton.class).isEnabled());
            assertEquals(language == 0 ? "Call 20" : "跟注 20", find(panel, "call", JButton.class).getText());
        });
    }

    private static void layoutTree(Container parent) {
        parent.invalidate();
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) layoutTree((Container) child);
        }
    }

    private static SetupView view(SetupSession session) { return session.getView().orElseThrow(); }

    private static SetupSession session(GameConfig config) {
        return new SetupSession(new GameInitializer(config, new FirstChoiceRandom(), new LastChoiceRandom()));
    }

    private static GamePanel panel(SetupSession session, int language) {
        GamePanel panel = new GamePanel(session);
        find(panel, "language", JComboBox.class).setSelectedIndex(language);
        return panel;
    }

    private static void start(GamePanel panel, int humans) {
        find(panel, "humanCount", JTextField.class).setText(Integer.toString(humans));
        find(panel, "start", JButton.class).doClick();
    }

    private static <T extends Component> T find(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return type.cast(child);
            if (child instanceof Container) {
                T found = locate((Container) child, name, type);
                if (found != null) return found;
            }
        }
        throw new AssertionError("Missing component: " + name);
    }

    private static <T extends Component> T locate(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return type.cast(child);
            if (child instanceof Container) {
                T found = locate((Container) child, name, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void onEdt(Runnable action) throws InterruptedException, InvocationTargetException {
        try { SwingUtilities.invokeAndWait(action); }
        catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof AssertionError) throw (AssertionError) cause;
            throw failure;
        }
    }

    private static final class FirstChoiceRandom extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return 0; }
    }

    private static final class LastChoiceRandom extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return bound - 1; }
    }
}
