package ui;

import domain.model.GameView;
import domain.model.PlayerType;
import domain.model.PlayerView;
import domain.model.ShowdownResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import localization.Localizations;

/** Displays public result information. Construct and use on the Swing event thread. */
public final class ShowdownPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Localizations MESSAGES = Localizations.load();
    private final JLabel title = new JLabel();
    private final JTextArea summary = new JTextArea(2, 34);
    private final JLabel status = new JLabel();

    public ShowdownPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(237, 242, 236));
        setForeground(new Color(24, 39, 47));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 184, 92), 2),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        title.setName("resultTitle");
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        summary.setName("resultSummary");
        summary.setFont(new Font(Font.DIALOG, Font.PLAIN, 17));
        summary.setEditable(false);
        summary.setOpaque(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        status.setName("resultStatus");
        status.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        add(title, BorderLayout.NORTH);
        add(summary, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        setVisible(false);
    }

    /**
     * Renders using public identity/seat information only; no live game model is retained.
     * Call again with the selected language after a language change.
     * @throws NullPointerException if view or result is null
     * @throws IllegalArgumentException for a different hand, unknown winner, or unsupported language
     */
    public void showResult(GameView view, ShowdownResult result, String language) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(result, "result");
        if (!view.getHandId().equals(result.getHandId())) {
            throw new IllegalArgumentException("The result and view must belong to the same hand.");
        }
        Locale locale = MESSAGES.locale(language);
        List<String> names = new ArrayList<>();
        for (String winnerId : result.getWinnerIds()) {
            PlayerView winner = view.getPlayers().stream()
                    .filter(player -> player.getId().equals(winnerId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("A winner is missing from the view."));
            names.add(MESSAGES.text(language, winner.getType() == PlayerType.HUMAN ? "human" : "bot",
                    winner.getSeat() + 1));
        }
        String key = result.isUncontested() ? "result.uncontested"
                : names.size() == 1 ? "result.winner" : "result.tie";
        String message = MESSAGES.text(language, key,
                String.join(MESSAGES.text(language, "result.separator"), names));
        String heading = MESSAGES.text(language, "result.title");
        String pending = MESSAGES.text(language, "result.awaitingSettlement");
        // Build and validate all text before replacing an earlier valid display.
        setLocale(locale);
        title.setText(heading);
        summary.setText(message);
        summary.setCaretPosition(0);
        status.setText(pending);
        setVisible(true);
        revalidate();
        repaint();
    }

    /** Removes an earlier hand's result before starting or displaying another hand. */
    public void clearResult() {
        title.setText("");
        summary.setText("");
        status.setText("");
        setVisible(false);
        revalidate();
        repaint();
    }
}
