package persistence;

import application.ChipHistoryEntry;
import application.HandCompletion;
import application.HandCompletionFixture;
import application.ProfileService;
import domain.model.CompletedStatistics;
import domain.model.PasswordHash;
import domain.model.PlayStats;
import domain.model.PlayerProfile;
import domain.model.Statistic;
import domain.model.StoredProfile;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SqliteHandSettlementTest {
    private static final Instant PAID_AT = Instant.parse("2026-11-19T18:00:00Z");
    @TempDir Path directory;

    @Test
    void settlesMultipleProfilesOnceAndPreservesSessionHistoryAcrossReopen() throws IOException {
        SqliteProfileRepository repository = repository();
        repository.save(List.of(profile("winner", 2_000), profile("loser", 2_000)));
        HandCompletion first = hand("hand-1", "session-a", 20, -20);
        repository.save(first);
        repository.save(first);
        repository.save(hand("hand-2", "session-a", 2_020, 1_980, -10, 10));
        repository.save(hand("hand-3", "session-b", 2_010, 1_990, 0, 0));

        SqliteProfileRepository reopened = repository();
        assertProfile(reopened, "winner", 2_010, 3);
        assertProfile(reopened, "loser", 1_990, 3);
        List<ChipHistoryEntry> history = reopened.loadChipHistory("winner");
        assertEquals(3, history.size());
        assertEquals(List.of("hand-3", "hand-2", "hand-1"),
                history.stream().map(ChipHistoryEntry::getHandId)
                        .collect(java.util.stream.Collectors.toList()));
        ChipHistoryEntry firstEntry = history.get(2);
        assertEquals("session-a", firstEntry.getSessionId());
        assertEquals(PAID_AT, firstEntry.getTimestamp());
        assertEquals(ChipHistoryEntry.Type.HAND_SETTLEMENT, firstEntry.getType());
        assertEquals(20, firstEntry.getChange());
        assertEquals(2_020, firstEntry.getBalance());
        assertEquals(10, history.get(1).getSessionNet());
        assertEquals(-10, history.get(1).getChange());
        assertEquals(0, history.get(0).getSessionNet());
        assertEquals(-10, reopened.loadChipHistory("loser").get(1).getSessionNet());
        assertTrue(reopened.loadChipHistory("other").isEmpty());
        assertThrows(UnsupportedOperationException.class, history::clear);
    }

    @Test
    void failedHistoryWriteRollsBackAllAccountsAndHandMarker() throws IOException, SQLException {
        SqliteProfileRepository repository = repository();
        repository.save(List.of(profile("winner", 2_000), profile("loser", 2_000)));
        execute("CREATE TRIGGER reject_history BEFORE INSERT ON chip_history "
                + "WHEN EXISTS (SELECT 1 FROM chip_history WHERE hand_id = NEW.hand_id) "
                + "BEGIN SELECT RAISE(ABORT, 'blocked'); END");
        HandCompletion hand = hand("rollback", "session", 50, -50);
        assertThrows(IOException.class, () -> repository.save(hand));
        assertProfile(repository, "winner", 2_000, 0);
        assertProfile(repository, "loser", 2_000, 0);
        assertTrue(repository.loadChipHistory("winner").isEmpty());
        assertTrue(repository.loadChipHistory("loser").isEmpty());
        execute("DROP TRIGGER reject_history");
        repository.save(hand);
        repository.save(hand);
        assertProfile(repository, "winner", 2_050, 1);
        assertProfile(repository, "loser", 1_950, 1);
        assertEquals(1, repository.loadChipHistory("winner").size());
    }

    @Test
    void rejectsUnknownAccountOverdrawOverflowAndLegacyDuplicate() throws IOException {
        SqliteProfileRepository repository = repository();
        StoredProfile winner = profile("winner", Long.MAX_VALUE);
        StoredProfile loser = profile("loser", 20);
        repository.save(List.of(new StoredProfile(winner.getProfile(), winner.getPasswordHash(),
                completed("legacy"), winner.getChipBalance()), loser));
        assertThrows(IOException.class,
                () -> repository.save(hand("under", "s", Long.MAX_VALUE, 20, 0, -21)));
        assertThrows(ArithmeticException.class,
                () -> hand("overflow", "s", Long.MAX_VALUE, 20, 1, 0));
        assertThrows(IOException.class,
                () -> repository.save(hand("legacy", "s", Long.MAX_VALUE, 20, 0, -1)));
        HandCompletion unknown = HandCompletionFixture.hand("unknown", "s",
                Map.of("other", HandCompletionFixture.change("other", 0, 1, completed("unknown"))));
        IOException missing = assertThrows(IOException.class, () -> repository.save(unknown));
        assertEquals("The settlement profile does not exist: other", missing.getMessage());
        assertProfile(repository, "winner", Long.MAX_VALUE, 1);
        assertProfile(repository, "loser", 20, 0);
        assertTrue(repository.loadChipHistory("winner").isEmpty());
        repository.save(hand("exact", "s", Long.MAX_VALUE, 20, 0, -20));
        assertProfile(repository, "loser", 0, 1);
    }

    @Test
    void upgradesVersionOneAccountsWithoutRegrantingOrInventingHistory()
            throws IOException, SQLException {
        SqliteProfileRepository repository = repository();
        repository.save(List.of(profile("winner", 42), profile("loser", 0)));
        execute("DROP TABLE chip_history");
        execute("DROP TABLE settled_hands");
        execute("PRAGMA user_version = 1");
        SqliteProfileRepository upgraded = repository();
        assertProfile(upgraded, "winner", 42, 0);
        assertProfile(upgraded, "loser", 0, 0);
        assertTrue(upgraded.loadChipHistory("winner").isEmpty());
        upgraded.save(hand("new", "s", 42, 0, -5, 5));
        assertProfile(repository(), "winner", 37, 1);
        assertProfile(repository(), "loser", 5, 1);
    }

    @Test
    void upgradesExistingHistoryAndRejectsRetryWithoutAnOriginalReceipt()
            throws IOException, SQLException {
        SqliteProfileRepository repository = repository();
        repository.save(List.of(profile("winner", 2_000), profile("loser", 2_000)));
        HandCompletion original = hand("previous", "s", 10, -10);
        repository.save(original);
        execute("ALTER TABLE settled_hands DROP COLUMN receipt");
        execute("PRAGMA user_version = 2");
        assertEquals(1, repository().loadChipHistory("winner").size());
        assertThrows(IOException.class, () -> repository().save(original));
        repository().save(hand("next", "s", 2_010, 1_990, 5, -5));
        assertProfile(repository(), "winner", 2_015, 2);
        assertEquals(15, repository().loadChipHistory("winner").get(0).getSessionNet());
    }

    @Test
    void authenticatedOverviewUsesOnlyThatAccountsHistory()
            throws IOException, GeneralSecurityException {
        SqliteProfileRepository repository = repository();
        ProfileService service = new ProfileService(repository);
        String password = "PokerPlayer123";
        String firstId = service.create("First", password.toCharArray()).getProfileId();
        String secondId = service.create("Second", password.toCharArray()).getProfileId();
        var first = service.login(firstId, password.toCharArray()).orElseThrow();
        var second = service.login(secondId, password.toCharArray()).orElseThrow();
        CompletedStatistics stats = completed("paid");
        repository.save(HandCompletionFixture.hand("paid", "session",
                Map.of(firstId, HandCompletionFixture.change(firstId, 2_000, -25, stats))));
        var overview = service.getOverview(first);
        assertEquals(1_975, overview.getBalance());
        assertEquals(-25, overview.getHistory().get(0).getChange());
        assertThrows(UnsupportedOperationException.class, () -> overview.getHistory().clear());
        assertEquals(2_000, service.getOverview(second).getBalance());
        assertTrue(service.getOverview(second).getHistory().isEmpty());
        ProfileService other = new ProfileService(repository);
        assertThrows(IllegalArgumentException.class, () -> other.getOverview(first));
    }

    @Test
    void malformedSavedEventIsAStorageError() throws IOException, SQLException {
        SqliteProfileRepository repository = repository();
        repository.save(List.of(profile("winner", 2_000), profile("loser", 2_000)));
        repository.save(hand("paid", "session", 10, -10));
        execute("UPDATE settled_hands SET settled_at = 'not-an-instant'");
        assertThrows(IOException.class, () -> repository.loadChipHistory("winner"));
        assertProfile(repository, "winner", 2_010, 1);
    }

    @Test
    void rollsBackWhenAnAccountWouldBeRemovedFromItsNewHistory()
            throws IOException, SQLException {
        SqliteProfileRepository repository = repository();
        repository.save(List.of(profile("winner", 2_000), profile("loser", 2_000)));
        execute("CREATE TRIGGER orphan_history AFTER INSERT ON chip_history "
                + "BEGIN DELETE FROM profiles WHERE id = NEW.profile_id; END");
        assertThrows(IOException.class, () -> repository.save(hand("paid", "s", 10, -10)));
        assertProfile(repository, "winner", 2_000, 0);
        assertProfile(repository, "loser", 2_000, 0);
        assertTrue(repository.loadChipHistory("winner").isEmpty());
    }

    @Test
    void refusesToPayIfTheDatabaseDoesNotRecordTheHand()
            throws IOException, SQLException {
        SqliteProfileRepository repository = repository();
        repository.save(List.of(profile("winner", 2_000), profile("loser", 2_000)));
        execute("CREATE TRIGGER skip_receipt BEFORE INSERT ON settled_hands "
                + "BEGIN SELECT RAISE(IGNORE); END");
        IOException failure = assertThrows(IOException.class,
                () -> repository.save(hand("paid", "s", 10, -10)));
        assertEquals("Conflicting retry for completed hand: paid", failure.getMessage());
        assertProfile(repository, "winner", 2_000, 0);
        assertProfile(repository, "loser", 2_000, 0);
    }

    private SqliteProfileRepository repository() {
        return new SqliteProfileRepository(directory, Clock.fixed(PAID_AT, ZoneOffset.UTC));
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection =
                     DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("profiles.db"));
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static HandCompletion hand(String handId, String sessionId,
            long win, long loss) {
        return hand(handId, sessionId, 2_000, 2_000, win, loss);
    }

    private static HandCompletion hand(String handId, String sessionId, long winner,
            long loser, long win, long loss) {
        CompletedStatistics stats = completed(handId);
        return HandCompletionFixture.hand(handId, sessionId,
                Map.of("winner", HandCompletionFixture.change("winner", winner, win, stats),
                        "loser", HandCompletionFixture.change("loser", loser, loss, stats)));
    }

    private static CompletedStatistics completed(String handId) {
        PlayStats stats = new PlayStats();
        stats.beginHand(handId, true);
        stats.completeHand(handId, false, false, 0, 0);
        return stats.completedSnapshot();
    }

    private static StoredProfile profile(String id, long balance) {
        PasswordHash hash = new PasswordHash("AAECAwQFBgcICQoLDA0ODw==",
                "7xdxRO7JQgy8EJPSqLNEqSvFBtDU7JwCjdGfgyTYweY=");
        return new StoredProfile(new PlayerProfile(id, id), hash, new CompletedStatistics(),
                balance);
    }

    private static void assertProfile(SqliteProfileRepository repository, String id,
            long balance, long hands) throws IOException {
        StoredProfile profile = repository.load().stream()
                .filter(stored -> stored.getProfile().getProfileId().equals(id))
                .findFirst().orElseThrow();
        assertEquals(balance, profile.getChipBalance());
        assertEquals(hands, profile.getStatistics().getCounts().get(Statistic.HANDS));
    }
}
