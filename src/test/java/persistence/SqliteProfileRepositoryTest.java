package persistence;

import application.ProfileService;
import domain.model.CompletedStatistics;
import domain.model.PasswordHash;
import domain.model.PlayStats;
import domain.model.PlayerProfile;
import domain.model.StoredProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SqliteProfileRepositoryTest {
    private static final String SALT = "AAECAwQFBgcICQoLDA0ODw==";
    private static final String HASH = "7xdxRO7JQgy8EJPSqLNEqSvFBtDU7JwCjdGfgyTYweY=";
    private static final String PASSWORD = "PokerPlayer123";
    @TempDir Path directory;

    @Test
    void migratesCredentialsAndCompletedStatisticsWithoutResettingBalance()
            throws IOException, GeneralSecurityException {
        FileProfileRepository legacy = new FileProfileRepository(directory);
        ProfileService original = new ProfileService(legacy);
        PlayerProfile player = original.create("Estella 玩家", PASSWORD.toCharArray());
        StoredProfile saved = legacy.load().get(0);
        CompletedStatistics stats = completed("imported-hand");
        legacy.save(List.of(new StoredProfile(player, saved.getPasswordHash(), stats)));
        Path catalog = directory.resolve("profiles.properties");
        byte[] originalBytes = Files.readAllBytes(catalog);

        SqliteProfileRepository migrated = new SqliteProfileRepository(directory);
        StoredProfile imported = migrated.load().get(0);
        assertEquals(player.getProfileId(), imported.getProfile().getProfileId());
        assertEquals(player.getDisplayName(), imported.getProfile().getDisplayName());
        assertEquals(saved.getPasswordHash().getHashBase64(),
                imported.getPasswordHash().getHashBase64());
        assertEquals(saved.getPasswordHash().getSaltBase64(),
                imported.getPasswordHash().getSaltBase64());
        assertEquals(stats.getCounts(), imported.getStatistics().getCounts());
        assertEquals(stats.getCompletedHandIds(), imported.getStatistics().getCompletedHandIds());
        assertEquals(2_000, imported.getChipBalance());
        migrated.save(List.of(new StoredProfile(player, saved.getPasswordHash(),
                stats.addHand(completed("new-hand")), 0)));

        SqliteProfileRepository reopened = new SqliteProfileRepository(directory);
        ProfileService service = new ProfileService(reopened);
        assertTrue(service.login(player.getProfileId(), "wrong".toCharArray()).isEmpty());
        var account = service.login(player.getProfileId(), PASSWORD.toCharArray()).orElseThrow();
        assertEquals(2, service.getLifetimeStatistics(account).getHandsPlayed());
        assertEquals(0, service.getChipBalance(account));
        assertEquals(1, reopened.load().size());
        assertArrayEquals(originalBytes, Files.readAllBytes(catalog));
    }

    @Test
    void registersNewAccountOnceAndReopensItFromSqlite()
            throws IOException, GeneralSecurityException {
        Path data = directory.resolve("profiles with space #?");
        SqliteProfileRepository repository = new SqliteProfileRepository(data);
        assertFalse(Files.exists(data));
        assertTrue(repository.load().isEmpty());
        ProfileService service = new ProfileService(repository);
        PlayerProfile player = service.create("O'Brien", PASSWORD.toCharArray());
        assertTrue(Files.isRegularFile(data.resolve("profiles.db")));
        ProfileService reopened = new ProfileService(new SqliteProfileRepository(data));
        var account = reopened.login(player.getProfileId(), PASSWORD.toCharArray()).orElseThrow();
        assertEquals("O'Brien", account.getProfile().getDisplayName());
        assertEquals(2_000, reopened.getChipBalance(account));
        assertThrows(IllegalArgumentException.class, () -> service.getChipBalance(account));
        assertEquals(0, reopened.getLifetimeStatistics(account).getHandsPlayed());
        assertEquals(1, reopened.list().size());
        assertFalse(Files.exists(data.resolve("profiles.properties")));
        assertThrows(UnsupportedOperationException.class, () -> repository.load().clear());
    }

    @Test
    void corruptLegacyCatalogLeavesMigrationRetryable() throws IOException {
        Path catalog = directory.resolve("profiles.properties");
        String broken = "formatVersion=2\nprofileCount=1\nprofile.0.id=legacy\n";
        Files.writeString(catalog, broken);
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        assertThrows(IOException.class, repository::load);
        assertEquals(broken, Files.readString(catalog));
        Files.writeString(catalog, broken + "profile.0.name=Old account\nprofile.0.salt="
                + SALT + "\nprofile.0.hash=" + HASH + "\n");
        StoredProfile migrated = repository.load().get(0);
        assertEquals("legacy", migrated.getProfile().getProfileId());
        assertEquals(2_000, migrated.getChipBalance());
        assertTrue(migrated.getStatistics().getCompletedHandIds().isEmpty());
    }

    @Test
    void transactionRollsBackEarlierProfilesWhenLaterWriteFails()
            throws IOException, SQLException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        StoredProfile existing = profile("existing", 1_200);
        repository.save(List.of(existing));
        execute("CREATE TRIGGER reject_account BEFORE INSERT ON profiles "
                + "WHEN NEW.id = 'blocked' BEGIN SELECT RAISE(ABORT, 'blocked'); END");
        StoredProfile updated = new StoredProfile(existing.getProfile(), existing.getPasswordHash(),
                completed("would-rollback"), 0);
        assertThrows(IOException.class,
                () -> repository.save(List.of(updated, profile("blocked", 0))));
        List<StoredProfile> saved = repository.load();
        assertEquals(1, saved.size());
        StoredProfile reopened = saved.get(0);
        assertEquals(1_200, reopened.getChipBalance());
        assertTrue(reopened.getStatistics().getCompletedHandIds().isEmpty());
    }

    @Test
    void rejectsDuplicateIdsBeforeCreatingDatabase() throws IOException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        assertThrows(IllegalArgumentException.class,
                () -> repository.save(List.of(profile("duplicate", 0), profile("duplicate", 1))));
        assertFalse(Files.exists(directory.resolve("profiles.db")));
        assertTrue(repository.load().isEmpty());
    }

    @Test
    void refusesToInitializeAnEmptyDatabaseMarkedWithAnUnsupportedVersion()
            throws IOException, SQLException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        execute("PRAGMA user_version = 99");
        assertThrows(IOException.class, repository::load);
        assertThrows(IOException.class, () -> repository.save(List.of(profile("new", 0))));
        execute("PRAGMA user_version = 0");
        assertTrue(repository.load().isEmpty());
    }

    @Test
    void rejectsMissingAndMalformedStoredStatisticsWithoutResettingProfiles()
            throws IOException, SQLException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        StoredProfile player = profile("original", 0);
        repository.save(List.of(player));
        execute("ALTER TABLE profiles RENAME TO original_profiles");
        execute("CREATE TABLE profiles AS SELECT * FROM original_profiles");
        execute("UPDATE profiles SET completed_statistics = 'profile.stats.HANDS=oops'");
        assertThrows(IOException.class, repository::load);
        execute("UPDATE profiles SET completed_statistics = NULL");
        assertThrows(IOException.class, repository::load);
        execute("DROP TABLE profiles");
        execute("ALTER TABLE original_profiles RENAME TO profiles");
        assertEquals(0, repository.load().get(0).getChipBalance());
    }

    @Test
    void unreadableDatabaseDoesNotFallBackToTheLegacyCatalog() throws IOException {
        FileProfileRepository legacy = new FileProfileRepository(directory);
        legacy.save(List.of(profile("legacy", 2_000)));
        Path database = directory.resolve("profiles.db");
        Files.writeString(database, "This is not a SQLite database.");
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        assertThrows(IOException.class, repository::load);
        assertEquals("This is not a SQLite database.", Files.readString(database));
        assertEquals("legacy", legacy.load().get(0).getProfile().getProfileId());
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection =
                     DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("profiles.db"));
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static StoredProfile profile(String id, long balance) {
        return new StoredProfile(new PlayerProfile(id, "Player"), new PasswordHash(SALT, HASH),
                new CompletedStatistics(), balance);
    }

    private static CompletedStatistics completed(String id) {
        PlayStats stats = new PlayStats();
        stats.beginHand(id, true);
        stats.completeHand(id, false, false, 0, 0);
        return stats.completedSnapshot();
    }
}
