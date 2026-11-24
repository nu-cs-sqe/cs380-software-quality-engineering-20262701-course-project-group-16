package ui;

import application.*;
import domain.model.*;
import persistence.FileProfileRepository;
import persistence.SqliteProfileRepository;
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
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void recoveryKeepsAnswersOnLanguageChangeAndShowsSavedReward()
            throws IOException, GeneralSecurityException, InterruptedException,
            InvocationTargetException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService profiles = new ProfileService(repository);
        profiles.create("Player", PASSWORD.toCharArray());
        StoredProfile stored = repository.load().get(0);
        repository.save(List.of(new StoredProfile(stored.getProfile(), stored.getPasswordHash(),
                stored.getStatistics(), 250)));
        RecoveryService recovery = recoveryFixture(profiles, repository);
        GamePanel panel = edt(() -> UiBotFixtures.passive(session(), profiles, repository,
                recovery));
        idle(panel);
        event(() -> assertFalse(find(panel, "beginRecovery", JButton.class).isEnabled()));
        login(panel, PASSWORD);
        event(() -> {
            find(panel, "beginRecovery", JButton.class).doClick(0);
            find(panel, "submitRecovery", JButton.class).doClick(0);
            assertTrue(find(panel, "profileStatus", JLabel.class).getText()
                    .contains("every question"));
            List<Integer> answers = selectRecoveryAnswers(panel,
                    recoveryFixture(profiles, repository));
            find(panel, "language", JComboBox.class).setSelectedIndex(1);
            assertEquals("检查答案", find(panel, "submitRecovery", JButton.class).getText());
            for (int index = 0; index < answers.size(); index++) {
                assertEquals(answers.get(index).intValue(),
                        find(panel, "recoveryChoice" + index, JComboBox.class).getSelectedIndex());
            }
            find(panel, "submitRecovery", JButton.class).doClick(0);
        });
        idle(panel);
        event(() -> {
            assertTrue(find(panel, "profileStatus", JLabel.class).getText()
                    .contains("恢复奖励已保存"));
            JTable history = find(panel, "chipHistory", JTable.class);
            assertEquals(1, history.getRowCount());
            assertEquals("筹码恢复奖励", history.getValueAt(0, 1));
            assertEquals("+200", history.getValueAt(0, 2));
            assertEquals("450", history.getValueAt(0, 3));
            assertEquals("0", find(panel, "lifetimeTable", JTable.class).getValueAt(0, 1));
            assertChipSummary(panel, 0, 450, "0");
            assertTrue(find(panel, "buyInAmount", JTextField.class).isEnabled());
        });
        assertEquals(450, repository.load().get(0).getChipBalance());
    }

    private static RecoveryService recoveryFixture(ProfileService profiles,
            SqliteProfileRepository repository) {
        return new RecoveryService(profiles, repository, java.time.Clock.systemUTC(), 17);
    }

    private static List<Integer> selectRecoveryAnswers(GamePanel panel, RecoveryService recovery) {
        java.util.Map<String, Integer> key = java.util.Map.of("blinds", 1, "fold", 2,
                "private", 0, "board", 1, "allIn", 2, "refund", 0, "sidePot", 1,
                "tie", 2, "quit", 0, "topUp", 1);
        List<Integer> answers = recovery.begin().getQuestions().stream()
                .map(question -> key.get(question.getId()))
                .collect(java.util.stream.Collectors.toList());
        for (int index = 0; index < answers.size(); index++) {
            find(panel, "recoveryChoice" + index, JComboBox.class)
                    .setSelectedIndex(answers.get(index));
        }
        return answers;
    }

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
        ProfileService profiles = new ProfileService(new SqliteProfileRepository(directory));
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
        ProfileService saved = new ProfileService(new SqliteProfileRepository(directory));
        GamePanel reopened = panel(saved, restarted, language);
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
    void w8E03OnlySignedInProfileSeesLocalizedChipHistory(int language)
            throws IOException, GeneralSecurityException, InterruptedException,
            InvocationTargetException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService profiles = new ProfileService(repository);
        PlayerProfile player = profiles.create("Player", PASSWORD.toCharArray());
        PlayStats stats = new PlayStats();
        stats.beginHand("paid-hand", true);
        stats.completeHand("paid-hand", false, false, 0, 0);
        repository.save(HandCompletionFixture.hand("paid-hand", "match",
                java.util.Map.of(player.getProfileId(),
                        HandCompletionFixture.change(player.getProfileId(), 2_000, 40,
                                stats.completedSnapshot()))));
        GamePanel panel = panel(profiles, session(), language);
        event(() -> assertEquals(0, find(panel, "chipHistory", JTable.class).getRowCount()));
        login(panel, PASSWORD);
        event(() -> {
            JTable history = find(panel, "chipHistory", JTable.class);
            assertEquals(1, history.getRowCount());
            String event = language == 0 ? "Hand settlement" : "手牌结算";
            assertEquals(event, history.getValueAt(0, 1));
            assertEquals("+40", history.getValueAt(0, 2));
            assertEquals("2040", history.getValueAt(0, 3));
            assertEquals("+40", history.getValueAt(0, 4));
            assertFalse(history.isCellEditable(0, 2));
            String balance = language == 0 ? "Saved balance:" : "账户余额：";
            assertTrue(find(panel, "accountChipBalance", JLabel.class).getText()
                    .contains(balance));
            find(panel, "removeProfile", JButton.class).doClick();
            assertEquals(0, history.getRowCount());
            assertFalse(find(panel, "accountAvailableChips", JLabel.class).isVisible());
            assertEquals("", find(panel, "accountAvailableChips", JLabel.class).getText());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void fundedStartUsesEachAccountsBuyInAndKeepsPrivateHandoffs(int language)
            throws IOException, GeneralSecurityException, InterruptedException,
            InvocationTargetException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService profiles = new ProfileService(repository);
        SetupSession session = session();
        GamePanel panel = edt(() -> UiBotFixtures.passive(session, profiles, repository));
        idle(panel);
        event(() -> assertFalse(find(panel, "buyInAmount", JTextField.class).isEnabled()));
        registerAndLogin(panel, "First");
        registerAndLogin(panel, "Second");
        selectBuyIn(panel, 1, "400");
        selectBuyIn(panel, 0, "1500");
        event(() -> find(panel, "joinedProfiles", JList.class).setSelectedIndex(1));
        idle(panel);
        event(() -> {
            find(panel, "language", JComboBox.class).setSelectedIndex(language);
            JTextField amount = find(panel, "buyInAmount", JTextField.class);
            assertEquals("400", amount.getText());
            assertEquals(language == 0 ? "Buy-in" : "买入筹码",
                    amount.getAccessibleContext().getAccessibleName());
            find(panel, "start", JButton.class).doClick(0);
        });
        idle(panel);
        event(() -> {
            assertEquals(1_500, reservedAtSeat(session, 0));
            assertEquals(400, reservedAtSeat(session, 1));
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            find(panel, "reveal", JButton.class).doClick(0);
            assertNotEquals("--", find(panel, "hole0", JLabel.class).getText());
            find(panel, "viewers", JComboBox.class).setSelectedIndex(1);
            assertEquals("--", find(panel, "hole0", JLabel.class).getText());
            assertChipSummary(panel, 400, 1_600, "0");
            find(panel, "viewers", JComboBox.class).setSelectedIndex(0);
            find(panel, "fold", JButton.class).doClick(0);
            find(panel, "fold", JButton.class).doClick(0);
            find(panel, "settle", JButton.class).doClick(0);
        });
        idle(panel);
        event(() -> {
            assertTrue(session.getSettlement().isPresent());
            assertChipSummary(panel, 390, 1_600, "-10");
            String topUp = language == 0 ? "Top up" : "补充筹码";
            String leave = language == 0 ? "Leave table" : "离开牌桌";
            String end = language == 0 ? "End table" : "结束牌桌";
            assertEquals(topUp, find(panel, "topUp", JButton.class).getText());
            assertEquals(leave, find(panel, "quit", JButton.class).getText());
            assertEquals(end, find(panel, "endTable", JButton.class).getText());
        });
        assertEquals(List.of(2_000L, 1_990L), repository.load().stream()
                .map(StoredProfile::getChipBalance).collect(java.util.stream.Collectors.toList()));
    }

    private static void selectBuyIn(GamePanel panel, int index, String amount)
            throws InterruptedException, InvocationTargetException {
        event(() -> find(panel, "joinedProfiles", JList.class).setSelectedIndex(index));
        idle(panel);
        event(() -> find(panel, "buyInAmount", JTextField.class).setText(amount));
    }

    private static long reservedAtSeat(SetupSession session, int seat) {
        String playerId = session.getView().orElseThrow().getPlayers().get(seat).getId();
        String accountId = session.getProfileIds().get(playerId);
        return session.getTableFunds().get(accountId).getTableChips();
    }

    @Test
    void invalidBuyInsPreserveCurrentTableAndClearingChoiceRestoresDefault()
            throws InterruptedException, InvocationTargetException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService profiles = new ProfileService(repository);
        SetupSession session = session();
        GamePanel panel = edt(() -> UiBotFixtures.passive(session, profiles, repository));
        idle(panel);
        registerAndLogin(panel, "Player");
        selectBuyIn(panel, 0, "400");
        event(() -> find(panel, "start", JButton.class).doClick(0));
        idle(panel);
        String original = edt(() -> session.getView().orElseThrow().getPlayers().get(0).getId());
        for (String invalid : List.of("399", "2001", "9223372036854775808", "not chips")) {
            event(() -> {
                find(panel, "reveal", JButton.class).doClick(0);
                find(panel, "buyInAmount", JTextField.class).setText(invalid);
                find(panel, "start", JButton.class).doClick(0);
            });
            idle(panel);
            event(() -> {
                assertEquals(original, session.getView().orElseThrow().getPlayers().get(0).getId());
                assertEquals(400, reservedAtSeat(session, 0));
                assertEquals("--", find(panel, "hole0", JLabel.class).getText());
                assertTrue(find(panel, "error", JLabel.class).getText().contains("buy-in"));
            });
        }
        selectBuyIn(panel, 0, "");
        event(() -> find(panel, "start", JButton.class).doClick(0));
        idle(panel);
        event(() -> assertEquals(1_000, reservedAtSeat(session, 0)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void fundedTableRetriesDurableSaveAndShowsOneHistoryRow(int language)
            throws IOException, GeneralSecurityException, InterruptedException,
            InvocationTargetException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService profiles = new ProfileService(repository);
        SetupSession session = session();
        AmbiguousTableStore store = new AmbiguousTableStore(repository);
        RecoveryService recovery = recoveryFixture(profiles, repository);
        GamePanel panel = edt(() -> UiBotFixtures.passive(session, profiles, store, recovery));
        idle(panel);
        event(() -> find(panel, "language", JComboBox.class).setSelectedIndex(language));
        registerAndLogin(panel, "Player");
        event(() -> find(panel, "beginRecovery", JButton.class).doClick(0));
        event(() -> find(panel, "start", JButton.class).doClick(0));
        idle(panel);
        String id = edt(() -> session.getProfileIds().values().iterator().next());
        event(() -> {
            assertEquals(2_000, session.getTableFunds().get(id).getBalance());
            assertEquals(1_000, session.getTableFunds().get(id).getTableChips());
            assertChipSummary(panel, 1_000, 1_000, "0");
            assertFalse(find(panel, "topUp", JButton.class).isEnabled());
            assertTrue(find(panel, "submitRecovery", JButton.class).isEnabled());
            find(panel, "fold", JButton.class).doClick(0);
            find(panel, "settle", JButton.class).doClick(0);
        });
        try {
            assertTrue(store.entered.await(5, TimeUnit.SECONDS));
            event(() -> assertFundedControlsLocked(panel));
        } finally {
            store.release.countDown();
        }
        idle(panel);
        event(() -> {
            assertTrue(session.getSettlement().isEmpty());
            assertTrue(find(panel, "settle", JButton.class).isEnabled());
            assertFalse(find(panel, "nextHand", JButton.class).isEnabled());
            assertTrue(find(panel, "submitRecovery", JButton.class).isEnabled());
            find(panel, "settle", JButton.class).doClick(0);
        });
        idle(panel);
        assertFundedTablePaid(panel, session, repository, id);
    }

    private static void assertFundedControlsLocked(GamePanel panel) {
        for (String name : List.of("start", "quit", "topUp", "endTable", "nextHand", "reveal",
                "beginRecovery", "submitRecovery")) {
            assertFalse(find(panel, name, JButton.class).isEnabled(), name);
        }
        assertFalse(find(panel, "buyInAmount", JTextField.class).isEnabled());
        assertFalse(find(panel, "topUpAmount", JTextField.class).isEnabled());
        assertFalse(find(panel, "recoveryChoice0", JComboBox.class).isEnabled());
        assertEquals("--", find(panel, "hole0", JLabel.class).getText());
    }

    private static void assertChipSummary(GamePanel panel, long reserved, long available,
            String sessionNet) {
        assertTextEndsWith(panel, "accountTableChips", reserved);
        assertTextEndsWith(panel, "accountAvailableChips", available);
        assertTrue(find(panel, "accountSessionNet", JLabel.class).getText().endsWith(sessionNet));
    }

    private static void assertTextEndsWith(GamePanel panel, String label, long amount) {
        String text = find(panel, label, JLabel.class).getText().replace(",", "");
        assertTrue(text.endsWith(Long.toString(amount)), text);
    }

    private static void assertFundedTablePaid(GamePanel panel, SetupSession session,
            SqliteProfileRepository repository, String id)
            throws IOException, InterruptedException, InvocationTargetException {
        long savedBalance = repository.getBalances(java.util.Set.of(id)).get(id);
        assertEquals(1, repository.loadChipHistory(id).size());
        event(() -> {
            assertTrue(session.getSettlement().isPresent());
            assertEquals(1, find(panel, "chipHistory", JTable.class).getRowCount());
            assertEquals(savedBalance, session.getTableFunds().get(id).getBalance());
            find(panel, "topUpAmount", JTextField.class).setText("20");
            find(panel, "topUp", JButton.class).doClick(0);
            assertEquals(1_020, session.getTableFunds().get(id).getTableChips());
            assertChipSummary(panel, 1_020, savedBalance - 1_020, "0");
        });
        idle(panel);
        event(() -> {
            assertTrue(find(panel, "nextHand", JButton.class).isEnabled());
            find(panel, "quit", JButton.class).doClick(0);
            assertFalse(find(panel, "nextHand", JButton.class).isEnabled());
        });
        idle(panel);
        event(() -> {
            find(panel, "endTable", JButton.class).doClick(0);
            assertFalse(find(panel, "nextHand", JButton.class).isEnabled());
            assertEquals(0, session.getTableFunds().get(id).getTableChips());
            assertChipSummary(panel, 0, savedBalance, "0");
        });
        idle(panel);
        assertEquals(savedBalance, repository.getBalances(java.util.Set.of(id)).get(id));
    }

    private static final class AmbiguousTableStore implements TableAccountStore {
        private final SqliteProfileRepository repository;
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private boolean firstSave = true;

        private AmbiguousTableStore(SqliteProfileRepository repository) {
            this.repository = repository;
        }

        @Override
        public java.util.Map<String, Long> getBalances(java.util.Set<String> ids)
                throws IOException {
            assertFalse(SwingUtilities.isEventDispatchThread());
            return repository.getBalances(ids);
        }

        @Override
        public void save(HandCompletion hand) throws IOException {
            assertFalse(SwingUtilities.isEventDispatchThread());
            if (firstSave) {
                entered.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IOException("Timed out waiting for the test to release the save");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(interrupted);
                }
            }
            repository.save(hand);
            if (firstSave) {
                firstSave = false;
                throw new IOException("First commit acknowledgement lost");
            }
        }
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
        long pot = edt(() -> {
            find(panel, "start", JButton.class).doClick();
            find(panel, "fold", JButton.class).doClick();
            repo.fail = true;
            find(panel, "settle", JButton.class).doClick(0);
            return session.getView().orElseThrow().getPot();
        });
        idle(panel);
        event(() -> {
            assertTrue(session.getSettlement().isEmpty());
            assertEquals(pot, session.getView().orElseThrow().getPot());
            assertEquals("0", find(panel, "lifetimeTable", JTable.class).getValueAt(0, 1));
            assertTrue(find(panel, "settle", JButton.class).isEnabled());
            assertTrue(find(panel, "error", JLabel.class).getText()
                    .contains(language == 0 ? "saved profiles" : "本地账号"));
            repo.fail = false;
            find(panel, "settle", JButton.class).doClick(0);
        });
        idle(panel);
        event(() -> {
            assertTrue(session.getSettlement().isPresent());
            assertEquals(" ", find(panel, "error", JLabel.class).getText());
            assertEquals("1", find(panel, "lifetimeTable", JTable.class).getValueAt(0, 1));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void w8E01SlowSettlementLeavesSwingResponsiveAndPublishesOnce(int language)
            throws InterruptedException, InvocationTargetException {
        GatedSettlementRepository repo =
                new GatedSettlementRepository(new FileProfileRepository(directory));
        ProfileService profiles = new ProfileService(repo, new FixedHasher(), () -> "profile");
        SetupSession session = session();
        GamePanel panel = panel(profiles, session, language);
        registerAndLogin(panel, "Player");
        long pot = edt(() -> {
            find(panel, "start", JButton.class).doClick();
            find(panel, "fold", JButton.class).doClick();
            return session.getView().orElseThrow().getPot();
        });
        repo.holdSettlement = true;
        SwingUtilities.invokeLater(() -> find(panel, "settle", JButton.class).doClick(0));

        CountDownLatch responsive = new CountDownLatch(1);
        try {
            assertTrue(repo.entered.await(5, TimeUnit.SECONDS), "Settlement must attempt a save");
            SwingUtilities.invokeLater(responsive::countDown);
            assertTrue(responsive.await(2, TimeUnit.SECONDS), "Save must leave Swing responsive");
            event(() -> assertSettlementPending(panel, session, pot, language));
            assertEquals(1, repo.attempts.get());
        } finally {
            repo.release.countDown();
            idle(panel);
        }
        event(() -> {
            assertTrue(session.getSettlement().isPresent());
            assertEquals(0, session.getView().orElseThrow().getPot());
            assertFalse(find(panel, "settle", JButton.class).isEnabled());
            find(panel, "settle", JButton.class).doClick(0);
            assertEquals("1", find(panel, "lifetimeTable", JTable.class).getValueAt(0, 1));
        });
        assertEquals(1, repo.attempts.get());
    }

    private static void assertSettlementPending(GamePanel panel, SetupSession session, long pot,
            int language) {
        for (String control : List.of("start", "quit", "reveal", "fold", "call", "check", "wager",
                "settle", "nextHand", "createProfile", "loginProfile", "reloadProfiles",
                "refreshLifetime", "removeProfile")) {
            assertFalse(find(panel, control, JButton.class).isEnabled(), control);
        }
        find(panel, "settle", JButton.class).doClick(0);
        find(panel, "humanCount", JTextField.class).postActionEvent();
        assertTrue(session.getSettlement().isEmpty());
        assertEquals(pot, session.getView().orElseThrow().getPot());
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 0));
        assertEquals("--", find(panel, "hole0", JLabel.class).getText());
        assertTrue(find(panel, "status", JLabel.class).getText()
                .contains(language == 0 ? "Saving hand result" : "正在保存本手结果"));
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

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void uiAc16ProfileWorkDisablesMergedWagerAndDepartureControls(int language)
            throws IOException, GeneralSecurityException, InterruptedException,
            InvocationTargetException {
        service().create("Player", PASSWORD.toCharArray());
        GateHasher hasher = new GateHasher();
        ProfileService profiles = new ProfileService(new FileProfileRepository(directory), hasher,
                () -> "another-profile");
        SetupSession session = session();
        GamePanel panel = panel(profiles, session, language);
        login(panel, PASSWORD);
        event(() -> {
            find(panel, "start", JButton.class).doClick();
            assertTrue(find(panel, "wager", JButton.class).isEnabled());
            assertTrue(find(panel, "quit", JButton.class).isEnabled());
            find(panel, "profileName", JTextField.class).setText("Another player");
            find(panel, "createPassword", JPasswordField.class).setText(PASSWORD);
            find(panel, "createProfile", JButton.class).doClick();
        });
        try {
            assertTrue(hasher.entered.await(10, TimeUnit.SECONDS));
            event(() -> {
                assertTrue(Boolean.TRUE.equals(panel.getClientProperty("profileBusy")));
                for (String name : List.of("quit", "wager", "wagerTarget")) {
                    assertFalse(find(panel, name, JComponent.class).isEnabled(), name);
                }
                assertFalse(session.hasDeparted(session.getView().orElseThrow()
                        .getPlayers().get(0).getId()));
            });
        } finally {
            hasher.release.countDown();
        }
        idle(panel);
        event(() -> {
            assertTrue(find(panel, "wager", JButton.class).isEnabled());
            assertTrue(find(panel, "quit", JButton.class).isEnabled());
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
        GamePanel panel = edt(() -> UiBotFixtures.passive(session, service));
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
            Runnable signalIfIdle = () -> {
                if (!Boolean.TRUE.equals(panel.getClientProperty("profileBusy"))
                        && !Boolean.TRUE.equals(panel.getClientProperty("settlementBusy"))) {
                    done.countDown();
                }
            };
            panel.addPropertyChangeListener("profileBusy", change -> signalIfIdle.run());
            panel.addPropertyChangeListener("settlementBusy", change -> signalIfIdle.run());
            signalIfIdle.run();
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

    private static final class GatedSettlementRepository implements ProfileRepository {
        private final ProfileRepository delegate;
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger attempts = new AtomicInteger();
        private volatile boolean holdSettlement;

        private GatedSettlementRepository(ProfileRepository delegate) {
            this.delegate = delegate;
        }

        @Override public List<StoredProfile> load() throws IOException {
            return delegate.load();
        }

        @Override public void save(List<StoredProfile> profiles) throws IOException {
            if (holdSettlement) {
                attempts.incrementAndGet();
                entered.countDown();
                try {
                    assertTrue(release.await(10, TimeUnit.SECONDS), "Save gate must be released");
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Save interrupted", failure);
                }
            }
            delegate.save(profiles);
        }
    }
}
