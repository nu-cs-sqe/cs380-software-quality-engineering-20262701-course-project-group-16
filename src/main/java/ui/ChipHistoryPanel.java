package ui;

import application.ChipHistoryEntry;
import application.ProfileOverview;
import java.awt.BorderLayout;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import localization.Localizations;

/** Displays saved virtual chip changes for the selected signed-in account. */
final class ChipHistoryPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final List<String> COLUMNS = List.of("time", "event", "change", "balance",
            "sessionNet");
    private final JLabel balance = new JLabel();
    private final JTable history = new JTable();

    ChipHistoryPanel() {
        super(new BorderLayout(0, 6));
        balance.setName("accountChipBalance");
        history.setName("chipHistory");
        add(balance, BorderLayout.NORTH);
        add(new JScrollPane(history), BorderLayout.CENTER);
    }

    void render(Optional<ProfileOverview> overview, Localizations messages, String language) {
        String balanceText = overview.map(data -> messages.text(language, "profile.chips.total",
                data.getBalance())).orElseGet(() -> messages.text(language, "profile.chips.none"));
        balance.setText(balanceText);
        List<ChipHistoryEntry> entries = overview.map(ProfileOverview::getHistory)
                .orElseGet(List::of);
        history.setModel(new HistoryModel(formatRows(entries, messages, language),
                COLUMNS.stream().map(key -> messages.text(language, "profile.chips." + key))
                        .toArray()));
    }

    private static Object[][] formatRows(List<ChipHistoryEntry> entries, Localizations messages,
            String language) {
        DateFormat dates = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                messages.locale(language));
        return entries.stream().map(entry -> new Object[]{
            dates.format(Date.from(entry.getTimestamp())),
            messages.text(language, "profile.chips.type." + entry.getType().name()),
            signed(entry.getChange(), messages, language),
            messages.text(language, "stats.count", entry.getBalance()),
            sessionNet(entry, messages, language)
        }).toArray(Object[][]::new);
    }

    private static String sessionNet(ChipHistoryEntry entry, Localizations messages,
            String language) {
        return entry.getSessionNet().isPresent()
                ? signed(entry.getSessionNet().orElseThrow(), messages, language)
                : messages.text(language, "stats.unavailable");
    }

    private static String signed(long chips, Localizations messages, String language) {
        String formatted = messages.text(language, "stats.count", chips);
        return chips > 0 ? "+" + formatted : formatted;
    }

    private static final class HistoryModel extends DefaultTableModel {
        private static final long serialVersionUID = 1L;

        HistoryModel(Object[][] rows, Object[] columns) {
            super(rows, columns);
        }

        @Override public boolean isCellEditable(int row, int column) { return false; }
    }
}
