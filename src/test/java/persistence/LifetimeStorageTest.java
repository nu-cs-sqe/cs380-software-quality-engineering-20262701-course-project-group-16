package persistence;

import domain.model.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class LifetimeStorageTest {
    @TempDir Path directory;
    private static final String SALT = "AAECAwQFBgcICQoLDA0ODw==";
    private static final String HASH = "7xdxRO7JQgy8EJPSqLNEqSvFBtDU7JwCjdGfgyTYweY=";

    @Test
    void stC01RoundTripsRawCountersAndOpaqueIdsWithNoPendingHand() throws IOException {
        PlayStats stats = new PlayStats();
        stats.beginHand(" completed=玩家\n ", true);
        stats.completeHand(" completed=玩家\n ", false, false, 0, 0);
        stats.beginHand("unfinished", true);
        FileProfileRepository repo = new FileProfileRepository(directory);
        repo.save(List.of(new StoredProfile(new PlayerProfile("profile", "玩家"),
                new PasswordHash(SALT, HASH), stats.completedSnapshot())));
        StoredProfile reopened = new FileProfileRepository(directory).load().get(0);
        assertEquals(stats.completedSnapshot().getCounts(), reopened.getStatistics().getCounts());
        assertEquals(stats.completedSnapshot().getCompletedHandIds(),
                reopened.getStatistics().getCompletedHandIds());
        assertFalse(Files.readString(directory.resolve("profiles.properties")).contains("unfinished"));
        assertEquals(HASH, reopened.getPasswordHash().getHashBase64());
    }

    @Test
    void stC02LoadsVersionTwoAccountsAndUpgradesOnlyOnSuccessfulSave() throws IOException {
        Path file = directory.resolve("profiles.properties");
        String old = "formatVersion=2\nprofileCount=1\nprofile.0.id=profile\n"
                + "profile.0.name=Player\nprofile.0.salt=" + SALT + "\nprofile.0.hash=" + HASH + "\n";
        Files.writeString(file, old);
        FileProfileRepository repo = new FileProfileRepository(directory);
        var loaded = repo.load();
        assertEquals(0L, loaded.get(0).getStatistics().getCounts().get(Statistic.HANDS));
        assertEquals(old, Files.readString(file));
        repo.save(loaded);
        assertTrue(Files.readString(file).contains("formatVersion=3"));
        assertEquals(HASH, repo.load().get(0).getPasswordHash().getHashBase64());
    }

    @Test
    void stC03CorruptCountersOrHistoryNeverResetTheCatalog() throws IOException {
        FileProfileRepository repo = new FileProfileRepository(directory);
        repo.save(List.of(new StoredProfile(new PlayerProfile("profile", "Player"),
                new PasswordHash(SALT, HASH))));
        Path file = directory.resolve("profiles.properties");
        String valid = Files.readString(file);
        for (String damaged : List.of(valid.replace("stats.HANDS=0", "stats.HANDS=-1"),
                valid.replace("stats.VPIP=0", "stats.VPIP=1"),
                valid.replace("completedCount=0", "completedCount=2147483647"),
                valid.replace("completedCount=0", "completedCount=-1"),
                valid.replace("stats.PFR=0", "stats.PFR=oops"))) {
            assertNotEquals(valid, damaged);
            Files.writeString(file, damaged);
            assertThrows(IOException.class, repo::load);
            assertEquals(damaged, Files.readString(file));
        }
    }

    @Test
    void stC05RoundTripsEveryStatisticAndRejectsDuplicateOrMissingHistory() throws IOException {
        PlayStats stats = new PlayStats();
        StatsContext none = new StatsContext(PreflopRaiseType.NONE, false, false, false);
        stats.beginHand("one", true);
        stats.recordAction(new StatsAction(true, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 40,
                new StatsContext(PreflopRaiseType.THREE_BET, true, false, false)));
        stats.recordAction(new StatsAction(true, BettingStreet.FLOP, ResolvedActionType.BET, 20,
                new StatsContext(PreflopRaiseType.NONE, false, true, false)));
        stats.recordAction(new StatsAction(true, BettingStreet.FLOP, ResolvedActionType.RAISE, 40, none));
        stats.recordAction(new StatsAction(true, BettingStreet.TURN, ResolvedActionType.CALL, 20, none));
        stats.completeHand("one", true, true, 60, 10);
        stats.beginHand("two", true);
        stats.recordAction(new StatsAction(true, BettingStreet.PREFLOP, ResolvedActionType.CALL, 20,
                new StatsContext(PreflopRaiseType.NONE, true, false, false)));
        stats.recordAction(new StatsAction(true, BettingStreet.FLOP, ResolvedActionType.FOLD, 0,
                new StatsContext(PreflopRaiseType.NONE, false, false, true)));
        stats.completeHand("two", true, false, 0, 0);
        FileProfileRepository repo = new FileProfileRepository(directory);
        repo.save(List.of(new StoredProfile(new PlayerProfile("id", "Player"),
                new PasswordHash(SALT, HASH), stats.completedSnapshot())));
        assertEquals(stats.completedSnapshot().getCounts(), repo.load().get(0).getStatistics().getCounts());
        PlayStats restored = PlayStats.fromCompleted(repo.load().get(0).getStatistics());
        assertEquals(50, restored.getPfrPercent().orElseThrow());
        assertEquals(50, restored.getThreeBetPercent().orElseThrow());
        assertEquals(2, restored.getAggressionFactor().orElseThrow());
        assertEquals(50, restored.getWtsdPercent().orElseThrow());
        Path file = directory.resolve("profiles.properties");
        String valid = Files.readString(file);
        java.util.Properties properties = new java.util.Properties();
        properties.load(new java.io.StringReader(valid));
        String first = properties.getProperty("profile.0.completed.0");
        String second = properties.getProperty("profile.0.completed.1");
        for (String corrupted : List.of(valid.replace("completed.1=" + second, "completed.1=" + first),
                valid.replace("profile.0.completed.1=", "profile.0.wrong.1="),
                valid.replace("completedCount=2", "completedCount=-1"),
                valid.replace("stats.PFR=1", "wrong.PFR=1"))) {
            assertNotEquals(valid, corrupted);
            Files.writeString(file, corrupted);
            assertThrows(IOException.class, repo::load);
        }
    }

    @ParameterizedTest(name = "ST-C06 missing catalog field: {0}")
    @MethodSource("requiredCatalogFields")
    void missingRequiredFieldsAreStorageErrorsAndPreserveCatalog(String field) throws IOException {
        PlayStats stats = new PlayStats();
        stats.beginHand("completed", true);
        stats.completeHand("completed", false, false, 0, 0);
        FileProfileRepository repository = new FileProfileRepository(directory);
        repository.save(List.of(new StoredProfile(new PlayerProfile("id", "Player"),
                new PasswordHash(SALT, HASH), stats.completedSnapshot())));
        Path catalog = directory.resolve("profiles.properties");
        Properties data = new Properties();
        data.load(new StringReader(Files.readString(catalog)));
        assertTrue(data.containsKey(field), field);
        data.remove(field);
        StringWriter encoded = new StringWriter();
        data.store(encoded, "Missing-field regression");
        String damaged = encoded.toString();
        Files.writeString(catalog, damaged);

        IOException failure = assertThrows(IOException.class, repository::load);

        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertEquals(damaged, Files.readString(catalog));
    }

    static Stream<String> requiredCatalogFields() {
        return Stream.concat(Stream.of("formatVersion", "profileCount", "profile.0.id",
                "profile.0.name", "profile.0.salt", "profile.0.hash",
                "profile.0.completedCount", "profile.0.completed.0"),
                java.util.Arrays.stream(Statistic.values())
                        .map(counter -> "profile.0.stats." + counter));
    }

    @Test
    void stC04StoredSnapshotRequiresStatisticsAndRetainsImmutableIdentity() {
        PlayerProfile identity = new PlayerProfile("id", "Player");
        PasswordHash hash = new PasswordHash(SALT, HASH);
        CompletedStatistics stats = new CompletedStatistics();
        StoredProfile record = new StoredProfile(identity, hash, stats);
        assertSame(stats, record.getStatistics());
        assertSame(identity, record.getProfile());
        assertSame(hash, record.getPasswordHash());
    }
}
