package ui;

import application.GameInitializer;
import application.SetupSession;
import domain.model.GameConfig;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
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

class GamePanelTest {
    @Test
    void initialScreenWaitsForAStartAndCannotRevealCards() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GamePanel panel = panel(new SetupSession());
            assertEquals("Start Game", find(panel, "start", JButton.class).getText());
            assertFalse(find(panel, "reveal", JButton.class).isEnabled());
            assertFalse(find(panel, "viewers", JComboBox.class).isEnabled());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
        });
    }

    @Test
    void startButtonDisplaysInitializedSeatsAndFirstActor() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session();
            GamePanel panel = panel(session);
            start(panel, "2");
            assertEquals(6, session.getView().orElseThrow().getPlayers().size());
            assertEquals("Human 1", find(panel, "seatName0", JLabel.class).getText());
            assertEquals("Bot 4", find(panel, "seatName3", JLabel.class).getText());
            assertEquals("Chips: 990", find(panel, "seatStack1", JLabel.class).getText());
            assertEquals("Chips: 980", find(panel, "seatStack2", JLabel.class).getText());
            assertEquals("Pot: 90", find(panel, "pot", JLabel.class).getText());
            assertEquals("Deck remaining: 40", find(panel, "deck", JLabel.class).getText());
            assertTrue(find(panel, "status", JLabel.class).getText().contains("Human 1"));
            assertEquals(2, find(panel, "viewers", JComboBox.class).getItemCount());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
        });
    }

    @Test
    void humanHandoffHidesPreviousCardsAndNeverRevealsOtherSeats() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GamePanel panel = panel(session());
            start(panel, "2");
            find(panel, "reveal", JButton.class).doClick();
            assertEquals("A ♥", find(panel, "hole0", JLabel.class).getText());
            assertEquals("Q ♥", find(panel, "hole1", JLabel.class).getText());
            find(panel, "viewers", JComboBox.class).setSelectedIndex(1);
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertTrue(find(panel, "handoff", JLabel.class).getText().contains("Human 2"));
            find(panel, "reveal", JButton.class).doClick();
            assertEquals("A ♠", find(panel, "hole0", JLabel.class).getText());
            assertEquals("3 ♣", find(panel, "hole1", JLabel.class).getText());
            for (int seat = 0; seat < 6; seat++) {
                assertEquals("▧  ▧", find(panel, "seatCards" + seat, JLabel.class).getText());
            }
        });
    }

    @Test
    void hideAndRestartBothConcealPrivateCards() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GamePanel panel = panel(session());
            start(panel, "2");
            find(panel, "reveal", JButton.class).doClick();
            find(panel, "hide", JButton.class).doClick();
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            find(panel, "reveal", JButton.class).doClick();
            start(panel, "1");
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertEquals(1, find(panel, "viewers", JComboBox.class).getItemCount());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "7", "abc", "", "2147483648"})
    void invalidInputShowsAnErrorAndHidesThePreviousHand(String input) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session();
            GamePanel panel = panel(session);
            start(panel, "2");
            String id = session.getView().orElseThrow().getPlayers().get(0).getId();
            find(panel, "reveal", JButton.class).doClick();
            start(panel, input);
            assertEquals("Enter a whole number from 1 to 6.", find(panel, "error", JLabel.class).getText());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertEquals(id, session.getView().orElseThrow().getPlayers().get(0).getId());
        });
    }

    @Test
    void privacyResetUsedOnWindowDeactivationHidesCards() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GamePanel panel = panel(session());
            assertDoesNotThrow(panel::hidePrivateCards);
            start(panel, "1");
            find(panel, "reveal", JButton.class).doClick();
            panel.hidePrivateCards();
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
        });
    }

    @Test
    void defaultLanguageIsEnglishWithAnExplicitChoiceRegardlessOfSystemLocale() throws InterruptedException, InvocationTargetException {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("zh-CN"));
            onEdt(() -> {
                GamePanel panel = panel(session());
                assertEquals("Start Game", find(panel, "start", JButton.class).getText());
                JComboBox<?> language = find(panel, "language", JComboBox.class);
                assertEquals(2, language.getItemCount());
                assertEquals("English", language.getItemAt(0).toString());
                assertEquals("简体中文", language.getItemAt(1).toString());
                language.setSelectedIndex(1);
                assertEquals("开始游戏", find(panel, "start", JButton.class).getText());
                language.setSelectedIndex(0);
                assertEquals("Start Game", find(panel, "start", JButton.class).getText());
            });
        } finally {
            Locale.setDefault(original);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void selectedLanguageAppliesToSetupAndPrivateHandoff(int language) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session();
            GamePanel panel = panel(session);
            find(panel, "language", JComboBox.class).setSelectedIndex(language);
            assertEquals(language == 0 ? "Start Game" : "开始游戏", find(panel, "start", JButton.class).getText());
            assertEquals(language == 0 ? "Texas Hold’em · Group 16" : "德州扑克 · 第16组", panel.getClientProperty("windowTitle"));
            start(panel, "2");
            assertEquals(language == 0 ? "Human 1" : "玩家 1", find(panel, "seatName0", JLabel.class).getText());
            assertEquals(language == 0 ? "Bot 4" : "电脑 4", find(panel, "seatName3", JLabel.class).getText());
            assertEquals(language == 0 ? "Chips: 990" : "筹码：990", find(panel, "seatStack1", JLabel.class).getText());
            assertEquals(language == 0 ? "Pot: 90" : "底池：90", find(panel, "pot", JLabel.class).getText());
            assertEquals(language == 0 ? "Deck remaining: 40" : "剩余牌数：40", find(panel, "deck", JLabel.class).getText());
            assertEquals(language == 0 ? "Turn: Human 1" : "当前行动：玩家 1", find(panel, "status", JLabel.class).getText());
            find(panel, "reveal", JButton.class).doClick();
            assertEquals("A ♥", find(panel, "hole0", JLabel.class).getText());
            find(panel, "viewers", JComboBox.class).setSelectedIndex(1);
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertTrue(find(panel, "handoff", JLabel.class).getText().contains(language == 0 ? "Human 2" : "玩家 2"));
            find(panel, "reveal", JButton.class).doClick();
            assertEquals("A ♠", find(panel, "hole0", JLabel.class).getText());
            for (int seat = 0; seat < 6; seat++) {
                assertEquals("▧  ▧", find(panel, "seatCards" + seat, JLabel.class).getText());
            }
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void selectedLanguageSurvivesRestartAndInvalidInputWithCardsHidden(int language) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session();
            GamePanel panel = panel(session);
            find(panel, "language", JComboBox.class).setSelectedIndex(language);
            start(panel, "2");
            String id = session.getView().orElseThrow().getPlayers().get(0).getId();
            for (String invalid : new String[] {"0", "7", "abc", "", "2147483648"}) {
                find(panel, "reveal", JButton.class).doClick();
                start(panel, invalid);
                assertEquals(language == 0 ? "Enter a whole number from 1 to 6." : "请输入1到6之间的整数。", find(panel, "error", JLabel.class).getText());
                assertEquals("--", find(panel, "hole0", JLabel.class).getText());
                assertEquals(id, session.getView().orElseThrow().getPlayers().get(0).getId());
            }
            find(panel, "reveal", JButton.class).doClick();
            start(panel, "1");
            assertEquals(" ", find(panel, "error", JLabel.class).getText());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertEquals(language == 0 ? "Human 1" : "玩家 1", find(panel, "seatName0", JLabel.class).getText());
            assertEquals(1, find(panel, "viewers", JComboBox.class).getItemCount());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void languageChangeHidesCardsPreservesMatchAndRetranslatesErrors(int initialLanguage) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            SetupSession session = session();
            GamePanel panel = panel(session);
            find(panel, "language", JComboBox.class).setSelectedIndex(initialLanguage);
            start(panel, "2");
            find(panel, "viewers", JComboBox.class).setSelectedIndex(1);
            String id = session.getView().orElseThrow().getPlayers().get(0).getId();
            find(panel, "reveal", JButton.class).doClick();
            find(panel, "language", JComboBox.class).setSelectedIndex(1 - initialLanguage);
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertEquals(id, session.getView().orElseThrow().getPlayers().get(0).getId());
            assertEquals(1, find(panel, "viewers", JComboBox.class).getSelectedIndex());
            start(panel, "0");
            find(panel, "language", JComboBox.class).setSelectedIndex(initialLanguage);
            assertEquals(initialLanguage == 0 ? "Enter a whole number from 1 to 6." : "请输入1到6之间的整数。", find(panel, "error", JLabel.class).getText());
        });
    }

    private static GamePanel panel(SetupSession session) {
        return new GamePanel(session);
    }

    private static SetupSession session() {
        Random dealer = new Random() {
            @Override public int nextInt(int bound) { return 0; }
        };
        return new SetupSession(new GameInitializer(new GameConfig(), dealer, new Random(7)));
    }

    private static void start(GamePanel panel, String count) {
        find(panel, "humanCount", JTextField.class).setText(count);
        find(panel, "start", JButton.class).doClick();
    }

    private static <T extends Component> T find(Container root, String name, Class<T> type) {
        Component found = locate(root, name);
        assertTrue(type.isInstance(found), "Missing " + name + " component");
        return type.cast(found);
    }

    private static Component locate(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof Container) {
                Component nested = locate((Container)c, name);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static void onEdt(Runnable action) throws InterruptedException, InvocationTargetException {
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof AssertionError) throw (AssertionError) cause;
            throw failure;
        }
    }
}
