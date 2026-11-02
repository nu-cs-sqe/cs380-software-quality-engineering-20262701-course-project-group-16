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
    void initialScreenWaitsForAStartAndCannotRevealCards() throws Exception {
        onEdt(() -> {
            GamePanel panel = panel(new SetupSession(), Locale.ENGLISH);
            assertEquals("Start Game", find(panel, "start", JButton.class).getText());
            assertFalse(find(panel, "reveal", JButton.class).isEnabled());
            assertFalse(find(panel, "viewers", JComboBox.class).isEnabled());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
        });
    }

    @Test
    void startButtonDisplaysInitializedSeatsAndFirstActor() throws Exception {
        onEdt(() -> {
            SetupSession session = session();
            GamePanel panel = panel(session, Locale.ENGLISH);
            start(panel, "2");
            assertEquals(6, session.getView().orElseThrow().getPlayers().size());
            assertEquals("Human 1", find(panel, "seatName0", JLabel.class).getText());
            assertEquals("Bot 4", find(panel, "seatName3", JLabel.class).getText());
            assertEquals("Chips: 990", find(panel, "seatStack1", JLabel.class).getText());
            assertEquals("Chips: 980", find(panel, "seatStack2", JLabel.class).getText());
            assertEquals("Pot: 30", find(panel, "pot", JLabel.class).getText());
            assertEquals("Deck remaining: 40", find(panel, "deck", JLabel.class).getText());
            assertTrue(find(panel, "status", JLabel.class).getText().contains("Bot 4"));
            assertEquals(2, find(panel, "viewers", JComboBox.class).getItemCount());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
        });
    }

    @Test
    void humanHandoffHidesPreviousCardsAndNeverRevealsOtherSeats() throws Exception {
        onEdt(() -> {
            GamePanel panel = panel(session(), Locale.ENGLISH);
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
    void hideAndRestartBothConcealPrivateCards() throws Exception {
        onEdt(() -> {
            GamePanel panel = panel(session(), Locale.ENGLISH);
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

    @Test
    void languageChangeKeepsMatchButHidesCards() throws Exception {
        onEdt(() -> {
            SetupSession session = session();
            GamePanel panel = panel(session, Locale.ENGLISH);
            start(panel, "2");
            String id = session.getView().orElseThrow().getPlayers().get(0).getId();
            find(panel, "reveal", JButton.class).doClick();
            find(panel, "language", JComboBox.class).setSelectedIndex(1);
            assertEquals("开始游戏", find(panel, "start", JButton.class).getText());
            assertEquals("人类玩家 1", find(panel, "seatName0", JLabel.class).getText());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertEquals(id, session.getView().orElseThrow().getPlayers().get(0).getId());
            find(panel, "language", JComboBox.class).setSelectedIndex(0);
            assertEquals("Start Game", find(panel, "start", JButton.class).getText());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "7", "abc", "", "2147483648"})
    void invalidInputShowsAnErrorAndHidesThePreviousHand(String input) throws Exception {
        onEdt(() -> {
            SetupSession session = session();
            GamePanel panel = panel(session, Locale.ENGLISH);
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
    void privacyResetUsedOnWindowDeactivationHidesCards() throws Exception {
        onEdt(() -> {
            GamePanel panel = panel(session(), Locale.ENGLISH);
            assertDoesNotThrow(panel::hidePrivateCards);
            start(panel, "1");
            find(panel, "reveal", JButton.class).doClick();
            panel.hidePrivateCards();
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
        });
    }

    @Test
    void chineseLocaleIsAvailableAtStartup() throws Exception {
        onEdt(() -> {
            GamePanel panel = panel(session(), Locale.SIMPLIFIED_CHINESE);
            assertEquals("开始游戏", find(panel, "start", JButton.class).getText());
            start(panel, "0");
            assertEquals("请输入 1 到 6 之间的整数。", find(panel, "error", JLabel.class).getText());
        });
    }

    private static GamePanel panel(SetupSession session, Locale locale) {
        return new GamePanel(session, locale);
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

    private static void onEdt(Runnable action) throws Exception {
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof AssertionError) throw (AssertionError)failure.getCause();
            throw failure;
        }
    }
}
