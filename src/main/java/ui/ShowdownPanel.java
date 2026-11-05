package ui;

import domain.model.GameView;
import domain.model.ShowdownResult;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** Displays a supplied showdown result. Construct and use on the Swing event thread. */
public final class ShowdownPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel title = new JLabel();
    private final JTextArea summary = new JTextArea();
    private final JLabel status = new JLabel();

    public ShowdownPanel() {
        title.setName("resultTitle");
        summary.setName("resultSummary");
        status.setName("resultStatus");
        add(title);
        add(summary);
        add(status);
        setVisible(false);
    }

    public void showResult(GameView view, ShowdownResult result, String language) {
    }

    public void clearResult() {
    }
}
