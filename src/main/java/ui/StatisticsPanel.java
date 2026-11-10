package ui;

import application.StatisticsView;
import domain.model.Player;
import domain.model.PlayerType;
import domain.model.PlayerProfile;
import java.util.stream.Collectors;
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

    StatisticsPanel() { this("statisticsTable"); }

    StatisticsPanel(String tableName) {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        table.setName(tableName);
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
        render(players, stats, Map.of(), messages, language);
    }

    void render(List<Player> players, Map<String, StatisticsView> stats, Map<String, String> names,
            Localizations messages, String language) {
        List<String> labels = players.stream().map(player -> names.getOrDefault(player.getId(),
                messages.text(language, player.getType() == PlayerType.HUMAN ? "human" : "bot",
                        player.getSeat() + 1))).collect(Collectors.toList());
        List<StatisticsView> views = players.stream().map(player -> stats.get(player.getId()))
                .collect(Collectors.toList());
        renderRows(labels, views, messages, language, "stats.note", "stats.title");
    }

    void renderProfiles(List<PlayerProfile> profiles, Map<String, StatisticsView> stats,
            Localizations messages, String language) {
        List<PlayerProfile> loaded = profiles.stream().filter(profile ->
                stats.containsKey(profile.getProfileId())).collect(Collectors.toList());
        renderRows(loaded.stream().map(PlayerProfile::getDisplayName).collect(Collectors.toList()),
                loaded.stream().map(profile -> stats.get(profile.getProfileId()))
                        .collect(Collectors.toList()),
                messages, language, "profile.statsNote", "profile.lifetime");
    }

    private void renderRows(List<String> names, List<StatisticsView> stats, Localizations messages,
            String language, String noteKey, String titleKey) {
        List<String> columns = new ArrayList<>();
        columns.add(messages.text(language, "stats.player"));
        columns.add(messages.text(language, "stats.hands"));
        StatisticsView.METRICS.forEach(key -> columns.add(messages.text(language, "stats." + key)));
        Object[][] rows = new Object[names.size()][];
        for (int index = 0; index < names.size(); index++) {
            rows[index] = row(names.get(index), stats.get(index), messages, language);
        }
        table.setModel(new ReadOnlyModel(rows, columns.toArray()));
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        for (int column = 1; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(100);
        }
        note.setText("<html>" + messages.text(language, noteKey) + "</html>");
        table.getAccessibleContext().setAccessibleName(messages.text(language, titleKey));
    }

    private static Object[] row(String name, StatisticsView stats,
            Localizations messages, String language) {
        Object[] row = new Object[StatisticsView.METRICS.size() + 2];
        row[0] = ProfilePanel.labelText(name);
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
