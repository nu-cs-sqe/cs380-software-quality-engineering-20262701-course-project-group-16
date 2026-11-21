package ui;

import application.SetupSession;
import application.ProfileService;
import application.RecoveryService;
import persistence.SqliteProfileRepository;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Desktop entry point for the played match. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Path directory = Path.of(System.getProperty("cs380.profileDirectory",
                    Path.of(System.getProperty("user.home"), ".cs380-holdem").toString()));
            SqliteProfileRepository repository = new SqliteProfileRepository(directory);
            ProfileService profiles = new ProfileService(repository);
            RecoveryService recovery = new RecoveryService(profiles, repository,
                    Clock.systemDefaultZone(), ThreadLocalRandom.current().nextLong());
            GamePanel panel = new GamePanel(new SetupSession(), profiles, repository, recovery);
            JFrame frame = new JFrame((String) panel.getClientProperty("windowTitle"));
            panel.addPropertyChangeListener("windowTitle",
                    event -> frame.setTitle((String) event.getNewValue()));
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setContentPane(panel);
            frame.setMinimumSize(new Dimension(1080, 780));
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
                    panel.hidePrivateCards();
                    if (!panel.isSavingHand()) {
                        panel.stopBots();
                        frame.dispose();
                    }
                }
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
