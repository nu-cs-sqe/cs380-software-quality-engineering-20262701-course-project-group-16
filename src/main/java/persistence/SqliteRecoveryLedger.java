package persistence;

import application.ChipHistoryEntry;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Successful recovery claims and their history, written inside the caller's transaction. */
final class SqliteRecoveryLedger {
    private static final String CREATE_CLAIMS = "CREATE TABLE recovery_claims ("
            + "challenge_id TEXT NOT NULL PRIMARY KEY, profile_id TEXT NOT NULL "
            + "REFERENCES profiles(id), local_date TEXT NOT NULL, claimed_at TEXT NOT NULL, "
            + "reward INTEGER NOT NULL CHECK (reward > 0), "
            + "balance INTEGER NOT NULL CHECK (balance >= reward)) STRICT";

    private SqliteRecoveryLedger() { }

    static void createTable(Connection connection) throws SQLException {
        try (Statement schema = connection.createStatement()) {
            schema.executeUpdate(CREATE_CLAIMS);
            schema.executeUpdate("CREATE INDEX recovery_by_profile_day "
                    + "ON recovery_claims (profile_id, local_date)");
        }
    }

    static boolean wasClaimed(Connection connection, String profileId, String challengeId)
            throws SQLException, IOException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT profile_id FROM recovery_claims WHERE challenge_id = ?")) {
            query.setString(1, challengeId);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {
                    return false;
                }
                if (!profileId.equals(SqliteProfileRepository.requiredText(row, "profile_id"))) {
                    throw new IOException("The challenge was claimed by another account.");
                }
                return true;
            }
        }
    }

    static long countClaims(Connection connection, String profileId, LocalDate date)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT COUNT(*) FROM recovery_claims WHERE profile_id = ? AND local_date = ?")) {
            query.setString(1, profileId);
            query.setString(2, date.toString());
            try (ResultSet row = query.executeQuery()) {
                return row.getLong(1);
            }
        }
    }

    static void recordClaim(Connection connection, String profileId, String challengeId,
            LocalDate date, Instant timestamp, long reward, long balance) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO recovery_claims (challenge_id, profile_id, local_date, "
                + "claimed_at, reward, balance) VALUES (?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, challengeId);
            insert.setString(2, profileId);
            insert.setString(3, date.toString());
            insert.setString(4, timestamp.toString());
            insert.setLong(5, reward);
            insert.setLong(6, balance);
            if (insert.executeUpdate() != 1) {
                throw new SQLException("Recovery claim could not be recorded.");
            }
        }
    }

    static void readHistory(Connection connection, String profileId, List<ChipHistoryEntry> entries)
            throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT claimed_at, reward, balance FROM recovery_claims "
                + "WHERE profile_id = ? ORDER BY rowid DESC")) {
            query.setString(1, profileId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    Instant time = Instant.parse(
                            SqliteProfileRepository.requiredText(rows, "claimed_at"));
                    entries.add(new ChipHistoryEntry(time, rows.getLong("reward"),
                            rows.getLong("balance")));
                }
            }
        }
    }
}
