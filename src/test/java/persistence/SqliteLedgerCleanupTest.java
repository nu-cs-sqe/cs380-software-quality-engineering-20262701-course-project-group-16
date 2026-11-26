package persistence;

import application.HandCompletion;
import application.HandCompletionFixture;
import domain.model.PlayStats;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteLedgerCleanupTest {
    private static final String HAND = "cleanup-hand";
    private static final String INSERT = "PreparedStatement.executeUpdate";
    private static final String READ = "ResultSet.next";
    private static final String QUERY = "PreparedStatement.executeQuery";
    private static final String CLOSE_STATEMENT = "PreparedStatement.close";
    private static final String CLOSE_ROW = "ResultSet.close";

    @Test
    void failedHandInsertKeepsTheWriteErrorAndSuppressesTheCloseError()
            throws SQLException, IOException {
        SQLException write = new SQLException("write failed");
        SQLException close = new SQLException("statement close failed");
        JdbcTrace trace = new JdbcTrace(Map.of(INSERT, write, CLOSE_STATEMENT, close));
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SqliteChipLedger.createTables(connection);
            Connection traced = trace.wrap(Connection.class, connection);
            assertSame(write, assertThrows(SQLException.class,
                    () -> SqliteChipLedger.recordHand(traced, hand(), Instant.EPOCH)));
            assertArrayEquals(new Throwable[] {close}, write.getSuppressed());
            assertEquals(List.of(INSERT, CLOSE_STATEMENT), trace.events);
            assertTrue(SqliteChipLedger.recordHand(connection, hand(), Instant.EPOCH));
            assertFalse(SqliteChipLedger.recordHand(connection, hand(), Instant.EPOCH));
        }
    }

    @Test
    void failedClosePreventsAHandInsertFromReturningASuccessfulReceipt() throws SQLException {
        SQLException close = new SQLException("statement close failed");
        JdbcTrace trace = new JdbcTrace(Map.of(CLOSE_STATEMENT, close));
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SqliteChipLedger.createTables(connection);
            connection.setAutoCommit(false);
            Connection traced = trace.wrap(Connection.class, connection);
            assertSame(close, assertThrows(SQLException.class,
                    () -> SqliteChipLedger.recordHand(traced, hand(), Instant.EPOCH)));
            assertEquals(List.of(INSERT, CLOSE_STATEMENT), trace.events);
            connection.rollback();
        }
    }

    @Test
    void failedRecoveryRowReadClosesBothResourcesAndKeepsSuppressedErrorsInOrder()
            throws SQLException {
        SQLException read = new SQLException("row read failed");
        SQLException closeRow = new SQLException("row close failed");
        SQLException closeStatement = new SQLException("statement close failed");
        JdbcTrace trace = new JdbcTrace(Map.of(
                READ, read, CLOSE_ROW, closeRow, CLOSE_STATEMENT, closeStatement));
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SqliteRecoveryLedger.createTable(connection);
            Connection traced = trace.wrap(Connection.class, connection);
            assertSame(read, assertThrows(SQLException.class,
                    () -> SqliteRecoveryLedger.wasClaimed(traced, "account", "challenge")));
            assertArrayEquals(new Throwable[] {closeRow, closeStatement}, read.getSuppressed());
            assertEquals(List.of(QUERY, READ, CLOSE_ROW, CLOSE_STATEMENT), trace.events);
        }
    }

    @Test
    void failedRecoveryQueryClosesTheStatementBeforeAnyRowExists() throws SQLException {
        SQLException query = new SQLException("query failed");
        JdbcTrace trace = new JdbcTrace(Map.of(QUERY, query));
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SqliteRecoveryLedger.createTable(connection);
            Connection traced = trace.wrap(Connection.class, connection);
            assertSame(query, assertThrows(SQLException.class,
                    () -> SqliteRecoveryLedger.wasClaimed(traced, "account", "challenge")));
            assertArrayEquals(new Throwable[0], query.getSuppressed());
            assertEquals(List.of(QUERY, CLOSE_STATEMENT), trace.events);
        }
    }

    private static HandCompletion hand() {
        PlayStats statistics = new PlayStats();
        statistics.beginHand(HAND, true);
        statistics.completeHand(HAND, false, false, 0, 0);
        return HandCompletionFixture.hand(HAND, "session", Map.of("account",
                HandCompletionFixture.change("account", 1000, 0, statistics.completedSnapshot())));
    }

    /** Delegates to real SQLite and injects a checked failure at a named JDBC operation. */
    private static final class JdbcTrace {
        private final Map<String, SQLException> failures;
        private final List<String> events = new ArrayList<>();

        JdbcTrace(Map<String, SQLException> failures) {
            this.failures = Map.copyOf(failures);
        }

        <T> T wrap(Class<T> type, T resource) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                    (proxy, method, args) -> {
                        String operation = type.getSimpleName() + "." + method.getName();
                        if (List.of(INSERT, QUERY, READ, CLOSE_STATEMENT, CLOSE_ROW)
                                .contains(operation)) {
                            events.add(operation);
                        }
                        if (!method.getName().equals("close") && failures.containsKey(operation)) {
                            throw failures.get(operation);
                        }
                        Object result;
                        try {
                            result = method.invoke(resource, args);
                        } catch (InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                        if (failures.containsKey(operation)) {
                            throw failures.get(operation);
                        }
                        return wrapResult(result);
                    }));
        }

        private Object wrapResult(Object result) {
            if (result instanceof PreparedStatement) {
                return wrap(PreparedStatement.class, (PreparedStatement) result);
            }
            if (result instanceof ResultSet) {
                return wrap(ResultSet.class, (ResultSet) result);
            }
            return result;
        }
    }
}
