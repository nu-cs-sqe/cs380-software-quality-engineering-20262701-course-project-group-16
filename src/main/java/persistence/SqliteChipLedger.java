package persistence;

import application.ChipHistoryEntry;
import application.HandCompletion;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

/** Hand identities and chip events written inside the caller's SQLite transaction. */
final class SqliteChipLedger {
    private static final String CREATE_HANDS = "CREATE TABLE settled_hands ("
            + "hand_id TEXT NOT NULL PRIMARY KEY, session_id TEXT NOT NULL, "
            + "settled_at TEXT NOT NULL, receipt TEXT NOT NULL) STRICT";
    private static final String CREATE_HISTORY = "CREATE TABLE chip_history ("
            + "profile_id TEXT NOT NULL REFERENCES profiles(id), "
            + "hand_id TEXT NOT NULL REFERENCES settled_hands(hand_id), "
            + "event_type TEXT NOT NULL, chip_change INTEGER NOT NULL, "
            + "balance INTEGER NOT NULL CHECK (balance >= 0), "
            + "PRIMARY KEY (profile_id, hand_id)) STRICT";
    private static final String INSERT_HAND = "INSERT INTO settled_hands "
            + "(hand_id, session_id, settled_at, receipt) VALUES (?, ?, ?, ?) "
            + "ON CONFLICT(hand_id) DO NOTHING";
    private static final String INSERT_EVENT = "INSERT INTO chip_history "
            + "(profile_id, hand_id, event_type, chip_change, balance) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_HISTORY = "SELECT h.hand_id, h.session_id, h.settled_at, "
            + "c.event_type, c.chip_change, c.balance, SUM(c.chip_change) OVER "
            + "(PARTITION BY h.session_id ORDER BY h.rowid) AS session_net "
            + "FROM chip_history c JOIN settled_hands h ON c.hand_id = h.hand_id "
            + "WHERE c.profile_id = ? ORDER BY h.rowid DESC";

    private SqliteChipLedger() { }

    static void createTables(Connection connection) throws SQLException {
        try (Statement schema = connection.createStatement()) {
            schema.executeUpdate(CREATE_HANDS);
            schema.executeUpdate(CREATE_HISTORY);
        }
    }

    static void upgradeReceipts(Connection connection) throws SQLException {
        try (Statement schema = connection.createStatement()) {
            schema.executeUpdate("ALTER TABLE settled_hands ADD COLUMN receipt TEXT NOT NULL "
                    + "DEFAULT ''");
        }
    }

    static boolean recordHand(Connection connection, HandCompletion hand, Instant timestamp)
            throws SQLException, IOException {
        String receipt = HandReceipt.encode(hand);
        try (PreparedStatement insert = connection.prepareStatement(INSERT_HAND)) {
            insert.setString(1, hand.getHandId());
            insert.setString(2, hand.getSessionId());
            insert.setString(3, timestamp.toString());
            insert.setString(4, receipt);
            if (insert.executeUpdate() == 1) {
                return true;
            }
        }
        requireIdenticalRetry(connection, hand.getHandId(), receipt);
        return false;
    }

    private static void requireIdenticalRetry(Connection connection, String handId, String receipt)
            throws SQLException, IOException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT receipt FROM settled_hands WHERE hand_id = ?")) {
            query.setString(1, handId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next() || !receipt.equals(SqliteProfileRepository.requiredText(
                        row, "receipt"))) {
                    throw new IOException("Conflicting retry for completed hand: " + handId);
                }
            }
        }
    }

    static void recordChange(Connection connection, String profileId, String handId, long change,
            long balance) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(INSERT_EVENT)) {
            insert.setString(1, profileId);
            insert.setString(2, handId);
            insert.setString(3, ChipHistoryEntry.Type.HAND_SETTLEMENT.name());
            insert.setLong(4, change);
            insert.setLong(5, balance);
            insert.executeUpdate();
        }
    }

    static void readHistory(Connection connection, String profileId, List<ChipHistoryEntry> entries)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(SELECT_HISTORY)) {
            query.setString(1, profileId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    entries.add(readEntry(rows));
                }
            }
        }
    }

    private static ChipHistoryEntry readEntry(ResultSet row) throws SQLException {
        String eventType = SqliteProfileRepository.requiredText(row, "event_type");
        return new ChipHistoryEntry(SqliteProfileRepository.requiredText(row, "hand_id"),
                SqliteProfileRepository.requiredText(row, "session_id"),
                Instant.parse(SqliteProfileRepository.requiredText(row, "settled_at")),
                ChipHistoryEntry.Type.valueOf(eventType),
                row.getLong("chip_change"), row.getLong("balance"), row.getLong("session_net"));
    }
}
