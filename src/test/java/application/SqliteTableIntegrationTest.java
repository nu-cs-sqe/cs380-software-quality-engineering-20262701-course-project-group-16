package application;

import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.CompletedStatistics;
import domain.model.PlayStats;
import domain.model.Statistic;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import persistence.SqliteProfileRepository;
import static org.junit.jupiter.api.Assertions.*;

class SqliteTableIntegrationTest {
    private static final String PASSWORD = "PokerPlayer123";
    @TempDir Path directory;

    @Test
    void paysRealHandAndRejectsConflictingRetryAndStaleBalances()
            throws IOException, GeneralSecurityException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService service = new ProfileService(repository);
        AuthenticatedProfile first = createAccount(service, "First");
        AuthenticatedProfile second = createAccount(service, "Second");
        SetupSession session = new SetupSession();
        session.startTable(service, List.of(first, second), repository);
        while (session.getResult().isEmpty()) {
            SetupView view = session.getView().orElseThrow();
            session.submitAction(new ActionRequest(
                    view.getPlayers().get(view.getCurrentActorSeat()).getId(), ActionType.FOLD));
        }
        PreparedSettlement prepared = session.prepareSettlement();
        HandCompletion hand = prepared.getHandCompletion().orElseThrow();
        prepared.save();
        assertTrue(session.getSettlement().isEmpty());
        session.publishSettlement(prepared);

        SqliteProfileRepository reopened = new SqliteProfileRepository(directory);
        reopened.save(hand);
        for (HandCompletion.AccountChange change : hand.getAccounts().values()) {
            String id = change.getAccountId();
            assertEquals(change.getBalanceAfter(), reopened.getBalances(Set.of(id)).get(id));
            List<ChipHistoryEntry> history = reopened.loadChipHistory(id);
            assertEquals(1, history.size());
            assertEquals(change.getNetChange(), history.get(0).getChange());
            assertEquals(hand.getSessionId(), history.get(0).getSessionId().orElseThrow());
        }
        verifyRejectedWrites(reopened, hand, first.getProfile().getProfileId());
        assertEquals(1, service.getLifetimeStatistics(first).getHandsPlayed());
        assertEquals(1, service.getLifetimeStatistics(second).getHandsPlayed());
    }

    private static void verifyRejectedWrites(SqliteProfileRepository repository,
            HandCompletion hand, String id) throws IOException {
        String differentHandId = hand.getHandId() + "-stale";
        long balance = hand.getAccounts().get(id).getBalanceAfter();
        HandCompletion conflicting = new HandCompletion(hand.getSessionId(), hand.getHandId(),
                Map.of(id, change(id, balance, hand.getHandId())), Map.of());
        assertThrows(IOException.class, () -> repository.save(conflicting));
        HandCompletion stale = new HandCompletion(hand.getSessionId(), differentHandId,
                Map.of(id, change(id, balance + 1, differentHandId)), Map.of());
        assertThrows(IOException.class, () -> repository.save(stale));
        assertEquals(balance, repository.getBalances(Set.of(id)).get(id));
        assertEquals(1, repository.loadChipHistory(id).size());
    }

    @Test
    void rejectsMalformedCompletionAndMissingBalanceWithoutChangingStorage()
            throws IOException, GeneralSecurityException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService service = new ProfileService(repository);
        String id = createAccount(service, "Player").getProfile().getProfileId();
        Map<String, HandCompletion.AccountChange> valid = Map.of(id, change(id, 2_000, "hand"));
        assertThrows(IllegalArgumentException.class,
                () -> repository.save(new HandCompletion(" ", "hand", valid, Map.of())));
        assertThrows(IllegalArgumentException.class,
                () -> repository.save(new HandCompletion("s", " ", valid, Map.of())));
        assertThrows(IllegalArgumentException.class,
                () -> repository.save(new HandCompletion("s", "hand", Map.of(), Map.of())));
        Map<String, HandCompletion.AccountChange> six = new HashMap<>();
        for (int seat = 0; seat < 6; seat++) {
            String other = "account-" + seat;
            six.put(other, change(other, 2_000, "hand"));
        }
        assertThrows(IOException.class,
                () -> repository.save(new HandCompletion("s", "hand", six, Map.of())));
        six.put(id, valid.get(id));
        assertThrows(IllegalArgumentException.class,
                () -> repository.save(new HandCompletion("s", "hand", six, Map.of())));
        assertThrows(IOException.class, () -> repository.save(new HandCompletion(
                "s", "hand", Map.of(" ", change(" ", 2_000, "hand")), Map.of())));
        assertThrows(IOException.class, () -> repository.save(new HandCompletion(
                "s", "other-hand", valid, Map.of())));
        assertThrows(IOException.class, () -> repository.save(new HandCompletion(
                "s", "hand", Map.of("other", valid.get(id)), Map.of())));
        assertThrows(IOException.class, () -> repository.getBalances(Set.of(id, "absent")));
        assertEquals(Map.of(), repository.getBalances(Set.of()));
        assertTrue(repository.loadChipHistory(id).isEmpty());
        assertEquals(Map.of(id, 2_000L), repository.getBalances(Set.of(id)));
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "2001,0", "0,-1"})
    void rejectsInvalidTableStacks(long starting, long ending)
            throws IOException, GeneralSecurityException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        String id = createAccount(new ProfileService(repository), "Player")
                .getProfile().getProfileId();
        HandCompletion.AccountChange result = new HandCompletion.AccountChange(id, 2_000,
                starting, ending, change(id, 2_000, "hand").getStatistics());
        assertThrows(IOException.class, () -> repository.save(new HandCompletion(
                "s", "hand", Map.of(id, result), Map.of())));
        assertEquals(Map.of(id, 2_000L), repository.getBalances(Set.of(id)));
    }

    @Test
    void rejectsRetriesWithOneChangedFieldAndAcceptsReorderedAccounts()
            throws IOException, GeneralSecurityException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService service = new ProfileService(repository);
        String first = createAccount(service, "First").getProfile().getProfileId();
        String second = createAccount(service, "Second").getProfile().getProfileId();
        Map<String, HandCompletion.AccountChange> accounts = new LinkedHashMap<>();
        accounts.put(first, change(first, 2_000, "hand"));
        accounts.put(second, change(second, 2_000, "hand"));
        Map<String, Long> bots = Map.of("bot", 0L);
        repository.save(new HandCompletion("s", "hand", accounts, bots));
        repository.save(new HandCompletion("s", "hand",
                Map.of(second, accounts.get(second), first, accounts.get(first)), bots));
        verifyRetryFields(repository, first, accounts, bots);
        assertEquals(1, repository.loadChipHistory(first).size());
        verifyRenamedAccount(repository, first);
    }

    private static void verifyRetryFields(SqliteProfileRepository repository, String id,
            Map<String, HandCompletion.AccountChange> accounts, Map<String, Long> bots) {
        assertConflict(repository, "other", accounts, bots);
        assertConflict(repository, "s", accounts, Map.of("other-bot", 0L));
        assertConflict(repository, "s", accounts, Map.of("bot", 1L));
        CompletedStatistics original = accounts.get(id).getStatistics();
        assertModifiedAccount(repository, accounts, id, 2_001, 0, 0, original);
        assertModifiedAccount(repository, accounts, id, 2_000, 1, 0, original);
        assertModifiedAccount(repository, accounts, id, 2_000, 0, 1, original);
        Map<Statistic, Long> counts = new HashMap<>(original.getCounts());
        counts.put(Statistic.VPIP, 1L);
        CompletedStatistics changed = new CompletedStatistics(counts, Set.of("hand"));
        assertModifiedAccount(repository, accounts, id, 2_000, 0, 0, changed);
    }

    private static void verifyRenamedAccount(SqliteProfileRepository repository, String id)
            throws IOException {
        String oneHand = "one";
        Map<String, HandCompletion.AccountChange> singleAccount =
                Map.of(id, change(id, 2_000, oneHand));
        repository.save(new HandCompletion("s", oneHand, singleAccount, Map.of()));
        HandCompletion renamed = new HandCompletion("s", oneHand,
                Map.of("absent", change("absent", 2_000, oneHand)), Map.of());
        IOException conflict = assertThrows(IOException.class, () -> repository.save(renamed));
        assertEquals("Conflicting retry for completed hand: one", conflict.getMessage());
    }

    private static void assertModifiedAccount(SqliteProfileRepository repository,
            Map<String, HandCompletion.AccountChange> accounts, String id, long balance,
            long starting, long ending, CompletedStatistics stats) {
        Map<String, HandCompletion.AccountChange> modified = new HashMap<>(accounts);
        modified.put(id, new HandCompletion.AccountChange(id, balance, starting, ending, stats));
        assertConflict(repository, "s", modified, Map.of("bot", 0L));
    }

    private static void assertConflict(SqliteProfileRepository repository, String session,
            Map<String, HandCompletion.AccountChange> accounts, Map<String, Long> bots) {
        IOException failure = assertThrows(IOException.class,
                () -> repository.save(new HandCompletion(session, "hand", accounts, bots)));
        assertEquals("Conflicting retry for completed hand: hand", failure.getMessage());
    }

    @Test
    void usesLoadedBalancesWithoutReadingStoreOnTheOwner()
            throws IOException, GeneralSecurityException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService service = new ProfileService(repository);
        AuthenticatedProfile account = createAccount(service, "Player");
        String id = account.getProfile().getProfileId();
        SetupSession session = new SetupSession();
        TableAccountStore store = new NoIoStore();
        session.startTableWithLoadedBalances(service, List.of(account), store, Map.of(id, 400L));
        assertEquals(400, session.getTableFunds().get(id).getTableChips());
        String handId = session.getView().orElseThrow().getHandId();
        assertThrows(IllegalArgumentException.class, () -> session.startTableWithLoadedBalances(
                service, List.of(account), store, Map.of(id, 399L)));
        assertEquals(handId, session.getView().orElseThrow().getHandId());

        Map<String, Long> loaded = Map.of(id, 500L);
        assertThrows(IllegalArgumentException.class, () -> session.startTableWithLoadedBalances(
                service, List.of(account), store, loaded, Map.of(id, 501L)));
        assertEquals(handId, session.getView().orElseThrow().getHandId());
        session.startTableWithLoadedBalances(service, List.of(account), store,
                loaded, Map.of(id, 450L));
        assertEquals(450, session.getTableFunds().get(id).getTableChips());
        assertEquals(50, session.getTableFunds().get(id).getAvailableChips());
    }

    private static final class NoIoStore implements TableAccountStore {
        @Override
        public Map<String, Long> getBalances(Set<String> ids) {
            return fail("The owner must not reload balances.");
        }

        @Override
        public void save(HandCompletion hand) {
            fail("Opening the table must not save a hand.");
        }
    }

    private static HandCompletion.AccountChange change(String id, long balance, String handId) {
        PlayStats stats = new PlayStats();
        stats.beginHand(handId, true);
        stats.completeHand(handId, false, false, 0, 0);
        CompletedStatistics result = stats.completedSnapshot();
        return new HandCompletion.AccountChange(id, balance, 0, 0, result);
    }

    private static AuthenticatedProfile createAccount(ProfileService service, String name)
            throws IOException, GeneralSecurityException {
        String id = service.create(name, PASSWORD.toCharArray()).getProfileId();
        return service.login(id, PASSWORD.toCharArray()).orElseThrow();
    }
}
