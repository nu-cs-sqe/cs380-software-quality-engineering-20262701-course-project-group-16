package ui;

import application.GameInitializer;
import application.SetupSession;
import domain.model.GameConfig;
import domain.model.GamePhase;
import java.awt.Component;
import java.awt.Container;
import java.util.Random;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class StatisticsGamePanelTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void paidHandsAppearInStatisticsAndContinueWithoutResettingTotals(int language)
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            SetupSession session = new SetupSession(new GameInitializer(new GameConfig(),
                    new FirstSeatRandom(), new Random(15)));
            GamePanel panel = new GamePanel(session);
            find(panel, "language", JComboBox.class).setSelectedIndex(language);
            assertFalse(find(panel, "settle", JButton.class).isEnabled());
            assertFalse(find(panel, "nextHand", JButton.class).isEnabled());
            assertEquals(0, find(panel, "statisticsTable", JTable.class).getRowCount());
            find(panel, "humanCount", JTextField.class).setText("6");
            find(panel, "start", JButton.class).doClick();
            JTable table = find(panel, "statisticsTable", JTable.class);
            assertEquals(6, table.getRowCount());
            assertEquals("0", table.getValueAt(0, 1));
            assertEquals(language == 0 ? "N/A" : "暂无", table.getValueAt(0, 2));
            for (int i = 0; i < 5; i++) find(panel, "fold", JButton.class).doClick();
            assertTrue(find(panel, "settle", JButton.class).isEnabled());
            assertFalse(find(panel, "nextHand", JButton.class).isEnabled());
            find(panel, "settle", JButton.class).doClick();
            assertEquals(GamePhase.COMPLETION, session.getView().orElseThrow().getPhase());
            assertFalse(find(panel, "settle", JButton.class).isEnabled());
            assertTrue(find(panel, "nextHand", JButton.class).isEnabled());
            assertEquals("1", table.getValueAt(0, 1));
            assertEquals("0.0%", table.getValueAt(0, 2));
            assertEquals(language == 0 ? "N/A" : "暂无", table.getValueAt(0, 11));
            assertFalse(table.isCellEditable(0, 1));
            assertTrue(find(panel, "result", JLabel.class).getText()
                    .contains(language == 0 ? "Pot paid" : "底池已结算"));
            find(panel, "reveal", JButton.class).doClick();
            find(panel, "sections", JTabbedPane.class).setSelectedIndex(1);
            assertTrue(session.getView().orElseThrow().getHoleCards().isEmpty());
            assertEquals(language == 0 ? "Hands" : "手数", table.getColumnName(1));
            find(panel, "sections", JTabbedPane.class).setSelectedIndex(0);
            find(panel, "nextHand", JButton.class).doClick();
            assertEquals("1", table.getValueAt(0, 1));
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            for (int i = 0; i < 5; i++) find(panel, "fold", JButton.class).doClick();
            find(panel, "settle", JButton.class).doClick();
            assertEquals("2", table.getValueAt(0, 1));
            find(panel, "language", JComboBox.class).setSelectedIndex(1 - language);
            table = find(panel, "statisticsTable", JTable.class);
            assertEquals("2", table.getValueAt(0, 1));
            find(panel, "start", JButton.class).doClick();
            assertEquals("0", table.getValueAt(0, 1));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void tableHeadersStayVisibleAtTheDefaultWindowSize(int language)
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(new SetupSession());
            find(panel, "language", JComboBox.class).setSelectedIndex(language);
            find(panel, "sections", JTabbedPane.class).setSelectedIndex(1);
            panel.setSize(1100, 740);
            layout(panel);
            JTable table = find(panel, "statisticsTable", JTable.class);
            assertNotNull(table.getTableHeader().getParent());
            assertTrue(table.getTableHeader().getHeight() > 0);
            assertEquals(12, table.getColumnCount());
        });
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) layout((Container) child);
        }
    }

    private static <T extends Component> T find(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) return type.cast(child);
            if (child instanceof Container) {
                T found = search((Container) child, name, type);
                if (found != null) return found;
            }
        }
        throw new AssertionError("Missing " + name);
    }

    private static <T extends Component> T search(Container parent, String name, Class<T> type) {
        try { return find(parent, name, type); } catch (AssertionError absent) { return null; }
    }

    private static final class FirstSeatRandom extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return 0; }
    }
}
