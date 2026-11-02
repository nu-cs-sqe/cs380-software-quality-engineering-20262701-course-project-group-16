package ui;

import application.SetupSession;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Desktop entry point for the setup preview. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel panel = new GamePanel(new SetupSession(), Locale.getDefault());
            JFrame frame = new JFrame("Texas Hold'em - Group 16");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(panel);
            frame.setMinimumSize(new Dimension(1080, 780));
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowDeactivated(WindowEvent event) {
                    panel.hidePrivateCards();
                }
            });
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
