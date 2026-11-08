package ui;

import application.StatisticsView;
import domain.model.Player;
import domain.model.PlayerType;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import localization.Localizations;

/** Read-only completed match statistics; horizontal scrolling keeps all metrics available. */
final class StatisticsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTable table = new JTable();
    private final JLabel note = new JLabel();

    StatisticsPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        table.setName("statisticsTable");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        add(note, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        add(scroll, BorderLayout.CENTER);
    }

    void render(List<Player> players, Map<String, StatisticsView> stats,
            Localizations messages, String language) {
        List<String> columns = new ArrayList<>();
        columns.add(messages.text(language, "stats.player"));
        columns.add(messages.text(language, "stats.hands"));
        StatisticsView.METRICS.forEach(key -> columns.add(messages.text(language, "stats." + key)));
        Object[][] rows = new Object[players.size()][];
        for (int index = 0; index < players.size(); index++) {
            Player player = players.get(index);
            rows[index] = row(player, stats.get(player.getId()), messages, language);
        }
        table.setModel(new ReadOnlyModel(rows, columns.toArray()));
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        for (int column = 1; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(100);
        }
        note.setText(messages.text(language, "stats.note"));
        table.getAccessibleContext().setAccessibleName(messages.text(language, "stats.title"));
    }

    private static Object[] row(Player player, StatisticsView stats,
            Localizations messages, String language) {
        Object[] row = new Object[StatisticsView.METRICS.size() + 2];
        row[0] = messages.text(language, player.getType() == PlayerType.HUMAN ? "human" : "bot",
                player.getSeat() + 1);
        row[1] = messages.text(language, "stats.count", stats.getHandsPlayed());
        for (int index = 0; index < stats.getRates().size(); index++) {
            row[index + 2] = format(stats.getRates().get(index), index, messages, language);
        }
        return row;
    }

    private static String format(OptionalDouble rate, int index,
            Localizations messages, String language) {
        if (rate.isEmpty()) {
            return messages.text(language, "stats.unavailable");
        }
        return messages.text(language, index == 2 || index == 4 ? "stats.number" : "stats.percent",
                rate.orElseThrow());
    }
    private static final class ReadOnlyModel extends DefaultTableModel {
        private static final long serialVersionUID = 1L;

        ReadOnlyModel(Object[][] rows, Object[] columns) { super(rows, columns); }

        @Override public boolean isCellEditable(int row, int column) { return false; }
    }

}
