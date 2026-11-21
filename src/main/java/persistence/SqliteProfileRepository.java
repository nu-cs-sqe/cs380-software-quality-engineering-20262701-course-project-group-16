package persistence;

import application.ChipHistoryEntry;
import application.HandCompletion;
import application.ProfileRepository;
import application.TableAccountStore;
import domain.model.GameConfig;
import domain.model.CompletedStatistics;
import domain.model.PasswordHash;
import domain.model.PlayerProfile;
import domain.model.StoredProfile;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/** Persists local accounts and lazily imports the legacy catalog in a single transaction. */
public final class SqliteProfileRepository implements ProfileRepository, TableAccountStore {
    private static final int UNINITIALIZED_SCHEMA_VERSION = 0;
    private static final int PROFILE_SCHEMA_VERSION = 1;
    private static final int HISTORY_SCHEMA_VERSION = 2;
    private static final int SCHEMA_VERSION = 3;
    private static final String CREATE_PROFILES = "CREATE TABLE profiles ("
            + "id TEXT NOT NULL PRIMARY KEY, display_name TEXT NOT NULL, salt TEXT NOT NULL, "
            + "password_hash TEXT NOT NULL, completed_statistics TEXT NOT NULL, "
            + "chip_balance INTEGER NOT NULL CHECK (chip_balance >= 0)) STRICT";
    private static final String STORE_PROFILE = "INSERT INTO profiles "
            + "(id, display_name, salt, password_hash, completed_statistics, chip_balance) "
            + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET "
            + "display_name=excluded.display_name, salt=excluded.salt, "
            + "password_hash=excluded.password_hash, "
            + "completed_statistics=excluded.completed_statistics, "
            + "chip_balance=excluded.chip_balance";
    private static final String STATS_PREFIX = "profile";

    private final Path directory;
    private final String databaseUrl;
    private final ProfileRepository legacy;
    private final Clock clock;
    private final StatisticsProperties statistics = new StatisticsProperties();

    public SqliteProfileRepository(Path directory) {
        this(directory, Clock.systemUTC());
    }

    SqliteProfileRepository(Path directory, Clock clock) {
        this.directory = directory;
        this.clock = clock;
        databaseUrl = "jdbc:sqlite:" + directory.resolve("profiles.db").toUri().toASCIIString();
        legacy = new FileProfileRepository(directory);
    }

    @Override
    public List<StoredProfile> load() throws IOException {
        List<StoredProfile> profiles = new ArrayList<>();
        transaction(connection -> readProfiles(connection, profiles));
        return List.copyOf(profiles);
    }

    @Override
    public void save(List<StoredProfile> profiles) throws IOException {
        requireUniqueProfiles(profiles);
        transaction(connection -> writeProfiles(connection, profiles));
    }

    @Override
    public void save(HandCompletion hand) throws IOException {
        requireSettlementIdentity(hand);
        transaction(connection -> applyHand(connection, hand));
    }

    @Override
    public Map<String, Long> getBalances(Set<String> accountIds) throws IOException {
        Map<String, Long> balances = new LinkedHashMap<>();
        transaction(connection -> readBalances(connection, accountIds, balances));
        return Map.copyOf(balances);
    }

    private void readBalances(Connection connection, Set<String> accountIds,
            Map<String, Long> balances) throws SQLException, IOException {
        for (String id : accountIds) {
            balances.put(id, findProfile(connection, id).getChipBalance());
        }
    }

    @Override
    public List<ChipHistoryEntry> loadChipHistory(String profileId) throws IOException {
        List<ChipHistoryEntry> entries = new ArrayList<>();
        transaction(connection -> SqliteChipLedger.readHistory(connection, profileId, entries));
        return List.copyOf(entries);
    }

    private static void requireSettlementIdentity(HandCompletion hand) {
        if (hand.getHandId().isBlank() || hand.getSessionId().isBlank()) {
            throw new IllegalArgumentException("A hand and session ID are required.");
        }
        if (hand.getAccounts().isEmpty() || hand.getAccounts().size() > GameConfig.SEAT_COUNT) {
            throw new IllegalArgumentException("Supply one to six completed accounts.");
        }
    }

    private void applyHand(Connection connection, HandCompletion hand)
            throws SQLException, IOException {
        if (!SqliteChipLedger.recordHand(connection, hand, clock.instant())) {
            return;
        }
        for (Map.Entry<String, HandCompletion.AccountChange> entry
                : hand.getAccounts().entrySet()) {
            applyAccountHand(connection, hand.getHandId(), entry.getKey(), entry.getValue());
        }
    }

    private void applyAccountHand(Connection connection, String handId, String profileId,
            HandCompletion.AccountChange result) throws SQLException, IOException {
        StoredProfile before = findProfile(connection, profileId);
        if (before.getChipBalance() != result.getBalanceBefore()) {
            throw new IOException("Saved balance changed before settlement: " + profileId);
        }
        StoredProfile updated = combineHand(before, result);
        writeProfiles(connection, List.of(updated));
        SqliteChipLedger.recordChange(connection, profileId, handId, result.getNetChange(),
                updated.getChipBalance());
    }

    private static StoredProfile combineHand(StoredProfile before,
            HandCompletion.AccountChange result) {
        CompletedStatistics stats = before.getStatistics().addHand(result.getStatistics());
        if (stats == before.getStatistics()) {
            throw new IllegalArgumentException("The hand already appears in legacy statistics.");
        }
        return new StoredProfile(before.getProfile(), before.getPasswordHash(), stats,
                result.getBalanceAfter());
    }

    private StoredProfile findProfile(Connection connection, String id)
            throws SQLException, IOException {
        try (PreparedStatement query =
                     connection.prepareStatement("SELECT * FROM profiles WHERE id = ?")) {
            query.setString(1, id);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {
                    throw new IOException("The settlement profile does not exist: " + id);
                }
                return readProfile(row);
            }
        }
    }

    private void transaction(DatabaseWork work) throws IOException {
        Files.createDirectories(directory);
        try (Connection connection = DriverManager.getConnection(databaseUrl)) {
            enableForeignKeys(connection);
            connection.setAutoCommit(false);
            initializeSchemaIfNeeded(connection);
            work.run(connection);
            connection.commit();
        } catch (SQLException | IllegalArgumentException | ArithmeticException
                | DateTimeException invalid) {
            throw new IOException("Could not access the local profile database.", invalid);
        }
    }

    private static void enableForeignKeys(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    private void initializeSchemaIfNeeded(Connection connection)
            throws SQLException, IOException {
        int version = schemaVersion(connection);
        if (version == SCHEMA_VERSION) {
            return;
        }
        upgradeSchema(connection, version);
        try (Statement schema = connection.createStatement()) {
            schema.executeUpdate("PRAGMA user_version = " + SCHEMA_VERSION);
        }
    }

    private void upgradeSchema(Connection connection, int version)
            throws SQLException, IOException {
        if (version == HISTORY_SCHEMA_VERSION) {
            SqliteChipLedger.upgradeReceipts(connection);
            return;
        }
        if (version != PROFILE_SCHEMA_VERSION) {
            requireUninitializedSchema(version);
            importLegacyCatalog(connection);
        }
        SqliteChipLedger.createTables(connection);
    }

    private static void requireUninitializedSchema(int version) throws IOException {
        if (version != UNINITIALIZED_SCHEMA_VERSION) {
            throw new IOException("Unsupported local profile database version: " + version);
        }
    }

    private void importLegacyCatalog(Connection connection) throws SQLException, IOException {
        List<StoredProfile> imported = legacy.load();
        try (Statement schema = connection.createStatement()) {
            schema.executeUpdate(CREATE_PROFILES);
            writeProfiles(connection, imported);
        }
    }

    private static int schemaVersion(Connection connection) throws SQLException {
        try (Statement query = connection.createStatement();
                ResultSet result = query.executeQuery("PRAGMA user_version")) {
            return result.getInt(1);
        }
    }

    private void readProfiles(Connection connection, List<StoredProfile> profiles)
            throws SQLException, IOException {
        try (Statement query = connection.createStatement();
                ResultSet rows = query.executeQuery("SELECT * FROM profiles ORDER BY rowid")) {
            while (rows.next()) {
                profiles.add(readProfile(rows));
            }
        }
    }

    private StoredProfile readProfile(ResultSet row) throws SQLException, IOException {
        PlayerProfile profile = new PlayerProfile(requiredText(row, "id"),
                requiredText(row, "display_name"));
        PasswordHash credential = new PasswordHash(requiredText(row, "salt"),
                requiredText(row, "password_hash"));
        Properties completed = new Properties();
        completed.load(new StringReader(requiredText(row, "completed_statistics")));
        long balance = Long.parseLong(requiredText(row, "chip_balance"));
        return new StoredProfile(profile, credential, statistics.read(completed, STATS_PREFIX),
                balance);
    }

    static String requiredText(ResultSet row, String column) throws SQLException {
        return Optional.ofNullable(row.getString(column)).orElseThrow(() ->
                new SQLException("Missing profile field: " + column));
    }

    private void writeProfiles(Connection connection, List<StoredProfile> profiles)
            throws SQLException, IOException {
        try (PreparedStatement store = connection.prepareStatement(STORE_PROFILE)) {
            for (StoredProfile profile : profiles) {
                writeProfile(store, profile);
            }
        }
    }

    private void writeProfile(PreparedStatement store, StoredProfile profile)
            throws SQLException, IOException {
        store.setString(1, profile.getProfile().getProfileId());
        store.setString(2, profile.getProfile().getDisplayName());
        store.setString(3, profile.getPasswordHash().getSaltBase64());
        store.setString(4, profile.getPasswordHash().getHashBase64());
        store.setString(5, encodeStatistics(profile.getStatistics()));
        store.setLong(6, profile.getChipBalance());
        store.executeUpdate();
    }

    private String encodeStatistics(CompletedStatistics completed) throws IOException {
        Properties properties = new Properties();
        statistics.write(properties, STATS_PREFIX, completed);
        StringWriter output = new StringWriter();
        properties.store(output, "");
        return output.toString();
    }

    private static void requireUniqueProfiles(List<StoredProfile> profiles) {
        Set<String> ids = new HashSet<>();
        for (StoredProfile profile : profiles) {
            if (!ids.add(profile.getProfile().getProfileId())) {
                throw new IllegalArgumentException("Profiles must have unique IDs.");
            }
        }
    }

    private interface DatabaseWork {
        void run(Connection connection) throws SQLException, IOException;
    }
}
