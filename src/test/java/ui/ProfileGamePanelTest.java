package ui;

import application.*;
import domain.model.*;
import persistence.FileProfileRepository;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ProfileGamePanelTest {
    private static final String PASSWORD = "PokerPlayer123";
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void uiAc01RequiresAnAccountAndOffersLocalizedMaskedForms(int language)
            throws InterruptedException, InvocationTargetException {
        GamePanel panel = panel(service(), session(), language);
        event(() -> {
            assertEquals(language == 0 ? "Profiles" : "账号", find(panel, "sections", JTabbedPane.class).getTitleAt(0));
            assertFalse(find(panel, "start", JButton.class).isEnabled());
            assertEquals(0, find(panel, "joinedProfiles", JList.class).getModel().getSize());
            assertNotEquals(0, find(panel, "createPassword", JPasswordField.class).getEchoChar());
            assertNotEquals(0, find(panel, "loginPassword", JPasswordField.class).getEchoChar());
            assertEquals(0, find(panel, "lifetimeTable", JTable.class).getRowCount());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void uiAc02ProfilesNameTheSeatsAndPreservePrivateHandoffs(int language)
            throws InterruptedException, InvocationTargetException {
        SetupSession session = session();
        GamePanel panel = panel(service(), session, language);
        registerAndLogin(panel, "Estella");
        registerAndLogin(panel, "Kevin 玩家");
        event(() -> {
            assertEquals("2", find(panel, "humanCount", JTextField.class).getText());
            assertFalse(find(panel, "humanCount", JTextField.class).isEditable());
            find(panel, "start", JButton.class).doClick();
            assertEquals(2, session.getProfileIds().size());
            assertEquals(4, session.getView().orElseThrow().getPlayers().stream()
                    .filter(p -> p.getType() == PlayerType.BOT).count());
            assertEquals("Estella", find(panel, "seatName0", JLabel.class).getText());
            assertEquals("Kevin 玩家", find(panel, "seatName1", JLabel.class).getText());
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            find(panel, "reveal", JButton.class).doClick();
            assertNotEquals("--", find(panel, "hole0", JLabel.class).getText());
            find(panel, "viewers", JComboBox.class).setSelectedIndex(1);
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertTrue(find(panel, "handoff", JLabel.class).getText().contains("Kevin 玩家"));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void uiAc03RealProfileFlowDisplaysSavedLifetimeTotalsAfterRestart(int language)
            throws InterruptedException, InvocationTargetException {
        ProfileService profiles = new ProfileService(new FileProfileRepository(directory));
        SetupSession session = session();
        GamePanel panel = panel(profiles, session, language);
        registerAndLogin(panel, "Estella 玩家");
        event(() -> {
            find(panel, "start", JButton.class).doClick();
            find(panel, "fold", JButton.class).doClick();
            assertTrue(session.getResult().isPresent());
            find(panel, "settle", JButton.class).doClick();
        });
        idle(panel);
        event(() -> {
            JTable lifetime = find(panel, "lifetimeTable", JTable.class);
            assertEquals(1, lifetime.getRowCount());
            assertEquals("1", lifetime.getValueAt(0, 1));
            assertEquals("Estella 玩家", lifetime.getValueAt(0, 0));
            assertFalse(lifetime.isCellEditable(0, 1));
        });
        SetupSession restarted = session();
        GamePanel reopened = panel(new ProfileService(new FileProfileRepository(directory)), restarted, language);
        event(() -> {
            assertEquals(0, find(reopened, "joinedProfiles", JList.class).getModel().getSize());
            assertEquals(1, find(reopened, "savedProfiles", JComboBox.class).getItemCount());
            assertEquals(0, find(reopened, "lifetimeTable", JTable.class).getRowCount());
        });
        login(reopened, PASSWORD);
        event(() -> {
            assertEquals("1", find(reopened, "lifetimeTable", JTable.class).getValueAt(0, 1));
            find(reopened, "start", JButton.class).doClick();
            assertEquals(1000, restarted.getView().orElseThrow().getPlayers().get(0).getChipStack());
            assertTrue(restarted.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 0));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void uiAc04WrongPasswordIsLocalizedAndDuplicateLoginIsDisabled(int language)
            throws IOException, GeneralSecurityException, InterruptedException, InvocationTargetException {
        ProfileService profiles = service();
        profiles.create("Player", PASSWORD.toCharArray());
        GamePanel panel = panel(profiles, session(), language);
        login(panel, "incorrect password");
        event(() -> {
            assertEquals(0, find(panel, "joinedProfiles", JList.class).getModel().getSize());
            assertTrue(find(panel, "profileError", JLabel.class).getText()
                    .contains(language == 0 ? "Incorrect password" : "密码错误"));
            assertEquals(0, find(panel, "loginPassword", JPasswordField.class).getPassword().length);
        });
        login(panel, PASSWORD);
        event(() -> {
            assertEquals(1, find(panel, "joinedProfiles", JList.class).getModel().getSize());
            assertFalse(find(panel, "loginProfile", JButton.class).isEnabled());
        });
    }

    @Test
    void uiAc05AtMostSixProfilesCanBeSelectedForTheNextMatch()
            throws IOException, GeneralSecurityException, InterruptedException, InvocationTargetException {
        ProfileService profiles = service();
        for (int i = 0; i < 7; i++) profiles.create("Player " + i, PASSWORD.toCharArray());
        GamePanel panel = panel(profiles, session(), 0);
        for (int i = 0; i < 6; i++) {
            final int index = i;
            event(() -> find(panel, "savedProfiles", JComboBox.class).setSelectedIndex(index));
            login(panel, PASSWORD);
        }
        event(() -> {
            find(panel, "savedProfiles", JComboBox.class).setSelectedIndex(6);
            assertFalse(find(panel, "loginProfile", JButton.class).isEnabled());
            assertEquals("6", find(panel, "humanCount", JTextField.class).getText());
            find(panel, "removeProfile", JButton.class).doClick();
            assertEquals("5", find(panel, "humanCount", JTextField.class).getText());
            assertTrue(find(panel, "loginProfile", JButton.class).isEnabled());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void uiAc06InvalidRegistrationNeverSavesAnAccount(int language)
            throws InterruptedException, InvocationTargetException, IOException {
        GamePanel panel = panel(new ProfileService(new FileProfileRepository(directory)), session(), language);
        for (String[] input : List.of(new String[]{"", PASSWORD}, new String[]{"Player", "a".repeat(5)},
                new String[]{"Player", "a".repeat(16)})) {
            event(() -> {
                find(panel, "profileName", JTextField.class).setText(input[0]);
                find(panel, "createPassword", JPasswordField.class).setText(input[1]);
                find(panel, "createProfile", JButton.class).doClick();
            });
            idle(panel);
            event(() -> {
                assertTrue(find(panel, "profileError", JLabel.class).getText()
                        .contains(language == 0 ? "6 to 15" : "6到15"));
                assertTrue(find(panel, "profileError", JLabel.class).getText()
                        .contains(language == 0 ? "nonblank name" : "非空名称"));
                assertEquals(0, find(panel, "createPassword", JPasswordField.class).getPassword().length);
                assertEquals(0, find(panel, "savedProfiles", JComboBox.class).getItemCount());
            });
        }
        assertFalse(Files.exists(directory.resolve("profiles.properties")));
    }

    @Test
    void uiAc07HashingDoesNotBlockTheEventThreadAndPasswordCopiesAreCleared()
            throws InterruptedException, InvocationTargetException {
        GateHasher hasher = new GateHasher();
        ProfileService profiles = new ProfileService(new FileProfileRepository(directory), hasher,
                () -> "profile");
        GamePanel panel = panel(profiles, session(), 0);
        event(() -> {
            find(panel, "profileName", JTextField.class).setText("Player");
            find(panel, "createPassword", JPasswordField.class).setText(PASSWORD);
            find(panel, "createProfile", JButton.class).doClick();
        });
        try {
            assertTrue(hasher.entered.await(10, TimeUnit.SECONDS));
            event(() -> {
                assertTrue(Boolean.TRUE.equals(panel.getClientProperty("profileBusy")));
                assertFalse(find(panel, "start", JButton.class).isEnabled());
                assertEquals(0, find(panel, "createPassword", JPasswordField.class).getPassword().length);
            });
        } finally { hasher.release.countDown(); }
        idle(panel);
        assertFalse(hasher.buffers.isEmpty());
        for (char[] buffer : hasher.buffers) assertArrayEquals(new char[buffer.length], buffer);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void uiAc08CorruptCatalogIsPreservedAndReloadCanRecover(int language)
            throws IOException, InterruptedException, InvocationTargetException {
        Path catalog = directory.resolve("profiles.properties");
        Files.writeString(catalog, "corrupt");
        GamePanel panel = panel(service(), session(), language);
        event(() -> assertTrue(find(panel, "profileError", JLabel.class).getText()
                .contains(language == 0 ? "saved profiles" : "本地账号")));
        assertEquals("corrupt", Files.readString(catalog));
        Files.delete(catalog);
        event(() -> find(panel, "reloadProfiles", JButton.class).doClick());
        idle(panel);
        event(() -> assertEquals(" ", find(panel, "profileError", JLabel.class).getText()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void uiAc09SettlementSaveFailureIsDisplayedAndRetryable(int language)
            throws InterruptedException, InvocationTargetException {
        FailingRepository repo = new FailingRepository(new FileProfileRepository(directory));
        ProfileService profiles = new ProfileService(repo, new FixedHasher(), () -> "profile");
        SetupSession session = session();
        GamePanel panel = panel(profiles, session, language);
        registerAndLogin(panel, "Player");
        event(() -> {
            find(panel, "start", JButton.class).doClick();
            find(panel, "fold", JButton.class).doClick();
            long pot = session.getView().orElseThrow().getPot();
            repo.fail = true;
            find(panel, "settle", JButton.class).doClick();
            assertTrue(session.getSettlement().isEmpty());
            assertEquals(pot, session.getView().orElseThrow().getPot());
            assertTrue(find(panel, "settle", JButton.class).isEnabled());
            assertTrue(find(panel, "error", JLabel.class).getText()
                    .contains(language == 0 ? "saved profiles" : "本地账号"));
            repo.fail = false;
            find(panel, "settle", JButton.class).doClick();
        });
        idle(panel);
        event(() -> assertEquals("1", find(panel, "lifetimeTable", JTable.class).getValueAt(0, 1)));
    }

    @Test
    void uiAc10LanguageTabsAndFocusClearCredentialsAndPrivateCards()
            throws InterruptedException, InvocationTargetException {
        GamePanel panel = panel(service(), session(), 0);
        registerAndLogin(panel, "Player");
        event(() -> {
            find(panel, "createPassword", JPasswordField.class).setText(PASSWORD);
            find(panel, "language", JComboBox.class).setSelectedIndex(1);
            assertEquals(0, find(panel, "createPassword", JPasswordField.class).getPassword().length);
            find(panel, "loginPassword", JPasswordField.class).setText(PASSWORD);
            find(panel, "start", JButton.class).doClick();
            assertEquals(0, find(panel, "loginPassword", JPasswordField.class).getPassword().length);
            find(panel, "reveal", JButton.class).doClick();
            panel.hidePrivateCards();
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
        });
    }

    @Test
    void uiAc11RosterChangesAffectOnlyTheNextMatch()
            throws InterruptedException, InvocationTargetException {
        SetupSession session = session();
        GamePanel panel = panel(service(), session, 0);
        registerAndLogin(panel, "Estella");
        registerAndLogin(panel, "Kevin");
        event(() -> {
            find(panel, "start", JButton.class).doClick();
            var binding = session.getProfileIds();
            find(panel, "sections", JTabbedPane.class).setSelectedIndex(0);
            find(panel, "removeProfile", JButton.class).doClick();
            assertEquals(1, find(panel, "joinedProfiles", JList.class).getModel().getSize());
            assertEquals(binding, session.getProfileIds());
            find(panel, "start", JButton.class).doClick();
            assertEquals(1, session.getProfileIds().size());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void uiAc12ProfileControlsFitTheDefaultWindow(int language)
            throws InterruptedException, InvocationTargetException {
        GamePanel panel = panel(service(), session(), language);
        event(() -> {
            panel.setSize(1100, 740);
            layout(panel);
            for (String name : List.of("createProfile", "loginProfile", "reloadProfiles")) {
                JButton button = find(panel, name, JButton.class);
                assertTrue(button.getWidth() > 0 && button.getHeight() > 0, name);
                assertTrue(button.getX() >= 0 && button.getX() + button.getWidth() <= button.getParent().getWidth(), name);
                assertTrue(button.getFontMetrics(button.getFont()).stringWidth(button.getText())
                        + button.getInsets().left + button.getInsets().right <= button.getWidth(), name);
            }
        });
    }

    @Test
    void uiAc13DisplayNamesAreRenderedAsTextRatherThanMarkup()
            throws InterruptedException, InvocationTargetException {
        GamePanel panel = panel(service(), session(), 0);
        registerAndLogin(panel, "<html><b>A&B</b>");
        event(() -> {
            find(panel, "start", JButton.class).doClick();
            String seat = find(panel, "seatName0", JLabel.class).getText();
            assertTrue(seat.contains("&lt;b&gt;A&amp;B&lt;/b&gt;"));
            assertFalse(seat.contains("<b>"));
            assertTrue(find(panel, "handoff", JLabel.class).getText().contains("&lt;html&gt;"));
        });
    }

    @Test
    void uiAc14ProviderFailureClearsPasswordBuffersAndShowsALocalizedError()
            throws InterruptedException, InvocationTargetException {
        UnavailableHasher hasher = new UnavailableHasher();
        ProfileService profiles = new ProfileService(new FileProfileRepository(directory), hasher,
                () -> "profile");
        GamePanel panel = panel(profiles, session(), 1);
        event(() -> {
            find(panel, "profileName", JTextField.class).setText("Player");
            find(panel, "createPassword", JPasswordField.class).setText(PASSWORD);
            find(panel, "createProfile", JButton.class).doClick();
        });
        idle(panel);
        char[] buffer = hasher.buffer.get();
        assertNotNull(buffer);
        assertArrayEquals(new char[buffer.length], buffer);
        event(() -> {
            assertTrue(find(panel, "profileError", JLabel.class).getText().contains("验证密码"));
            assertEquals(0, find(panel, "savedProfiles", JComboBox.class).getItemCount());
        });
    }

    @Test
    void uiAc15SameNameAccountsRemainDistinctInThePicker()
            throws IOException, GeneralSecurityException, InterruptedException, InvocationTargetException {
        ProfileService profiles = service();
        profiles.create("Player", PASSWORD.toCharArray());
        profiles.create("Player", PASSWORD.toCharArray());
        GamePanel panel = panel(profiles, session(), 0);
        event(() -> {
            JComboBox<?> choices = find(panel, "savedProfiles", JComboBox.class);
            assertNotEquals(choices.getItemAt(0), choices.getItemAt(1));
            choices.setSelectedIndex(1);
        });
        login(panel, PASSWORD);
        event(() -> {
            assertEquals(1, find(panel, "savedProfiles", JComboBox.class).getSelectedIndex());
            find(panel, "start", JButton.class).doClick();
        });
    }

    private ProfileService service() {
        java.util.concurrent.atomic.AtomicInteger ids = new java.util.concurrent.atomic.AtomicInteger();
        return new ProfileService(new FileProfileRepository(directory), new FixedHasher(),
                () -> "profile-" + ids.incrementAndGet());
    }

    private static SetupSession session() {
        Random dealer = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return 0; }
        };
        return new SetupSession(new GameInitializer(new GameConfig(), dealer, new Random(12)));
    }

    private static GamePanel panel(ProfileService service, SetupSession session, int language)
            throws InterruptedException, InvocationTargetException {
        GamePanel panel = edt(() -> new GamePanel(session, service));
        idle(panel);
        event(() -> find(panel, "language", JComboBox.class).setSelectedIndex(language));
        return panel;
    }

    private static void registerAndLogin(GamePanel panel, String name)
            throws InterruptedException, InvocationTargetException {
        event(() -> {
            find(panel, "profileName", JTextField.class).setText(name);
            find(panel, "createPassword", JPasswordField.class).setText(PASSWORD);
            find(panel, "createProfile", JButton.class).doClick();
        });
        idle(panel);
        login(panel, PASSWORD);
    }

    private static void login(GamePanel panel, String password)
            throws InterruptedException, InvocationTargetException {
        event(() -> {
            find(panel, "loginPassword", JPasswordField.class).setText(password);
            find(panel, "loginProfile", JButton.class).doClick();
        });
        idle(panel);
    }

    private static void idle(GamePanel panel) throws InterruptedException, InvocationTargetException {
        CountDownLatch done = new CountDownLatch(1);
        event(() -> {
            if (!Boolean.TRUE.equals(panel.getClientProperty("profileBusy"))) done.countDown();
            else panel.addPropertyChangeListener("profileBusy", change -> {
                if (Boolean.FALSE.equals(change.getNewValue())) done.countDown();
            });
        });
        assertTrue(done.await(15, TimeUnit.SECONDS), "Profile work must finish");
        event(() -> { });
    }

    private static <T> T edt(Supplier<T> action) throws InterruptedException, InvocationTargetException {
        AtomicReference<T> value = new AtomicReference<>();
        event(() -> value.set(action.get()));
        return value.get();
    }

    private static void event(Runnable action) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(action);
    }

    private static <T extends Component> T find(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return type.cast(child);
            if (child instanceof Container) {
                T result = search((Container) child, name, type);
                if (result != null) return result;
            }
        }
        throw new AssertionError("Missing " + name);
    }

    private static <T extends Component> T search(Container parent, String name, Class<T> type) {
        try { return find(parent, name, type); } catch (AssertionError missing) { return null; }
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container) layout((Container) child);
    }

    private static class FixedHasher implements PasswordHasher {
        private static final PasswordHash HASH = new PasswordHash("AAECAwQFBgcICQoLDA0ODw==",
                "7xdxRO7JQgy8EJPSqLNEqSvFBtDU7JwCjdGfgyTYweY=");
        @Override public PasswordHash hash(char[] password) throws GeneralSecurityException {
            assertFalse(SwingUtilities.isEventDispatchThread());
            return HASH;
        }
        @Override public boolean matches(char[] password, PasswordHash credential) {
            assertFalse(SwingUtilities.isEventDispatchThread());
            return Arrays.equals(PASSWORD.toCharArray(), password);
        }
    }

    private static final class GateHasher extends FixedHasher {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final List<char[]> buffers = new CopyOnWriteArrayList<>();
        @Override public PasswordHash hash(char[] password) throws GeneralSecurityException {
            buffers.add(password);
            entered.countDown();
            try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
            return super.hash(password);
        }
    }

    private static final class UnavailableHasher extends FixedHasher {
        private final AtomicReference<char[]> buffer = new AtomicReference<>();
        @Override public PasswordHash hash(char[] password) throws GeneralSecurityException {
            buffer.set(password);
            throw new GeneralSecurityException("Provider unavailable");
        }
    }

    private static final class FailingRepository implements ProfileRepository {
        private final ProfileRepository delegate;
        private volatile boolean fail;
        private FailingRepository(ProfileRepository delegate) { this.delegate = delegate; }
        @Override public List<StoredProfile> load() throws IOException { return delegate.load(); }
        @Override public void save(List<StoredProfile> profiles) throws IOException {
            if (fail) throw new IOException("Disk full");
            delegate.save(profiles);
        }
    }
}
