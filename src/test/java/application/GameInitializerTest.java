package application;

import domain.model.BettingProgress;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.HandPositions;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.PlayerType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Isolates orchestration from setup, randomness and card dealing. */
class GameInitializerTest {
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 6})
    void passesRequestedCountAndCreatedTableToDealer(int humans) {
        GameState table = table("table");
        GameState result = table("result");
        AtomicInteger setupCalls = new AtomicInteger();
        AtomicInteger dealCalls = new AtomicInteger();
        GameInitializer initializer = new GameInitializer(count -> {
            assertEquals(humans, count);
            setupCalls.incrementAndGet();
            return table;
        }, created -> {
            assertSame(table, created);
            dealCalls.incrementAndGet();
            return result;
        });

        assertSame(result, initializer.start(humans));
        assertEquals(1, setupCalls.get());
        assertEquals(1, dealCalls.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, 0, 7, Integer.MAX_VALUE})
    void setupRejectionStopsBeforeDealing(int humans) {
        IllegalArgumentException rejection = new IllegalArgumentException("Invalid human count");
        GameInitializer initializer = new GameInitializer(count -> {
            assertEquals(humans, count);
            throw rejection;
        }, created -> { throw new AssertionError("Rejected setup must not deal cards"); });

        assertSame(rejection, assertThrows(IllegalArgumentException.class,
                () -> initializer.start(humans)));
    }

    @Test
    void eachStartUsesTheNewlyCreatedTable() {
        GameState first = table("first");
        GameState second = table("second");
        List<GameState> tables = List.of(first, second);
        AtomicInteger calls = new AtomicInteger();
        GameInitializer initializer = new GameInitializer(count -> tables.get(calls.getAndIncrement()),
                created -> created);

        assertSame(first, initializer.start(1));
        assertSame(second, initializer.start(6));
        assertEquals(2, calls.get());
    }

    @Test
    void dealingFailureIsReportedToTheCaller() {
        GameState table = table("table");
        IllegalStateException failure = new IllegalStateException("Cannot deal");
        GameInitializer initializer = new GameInitializer(count -> table, created -> { throw failure; });

        assertSame(failure, assertThrows(IllegalStateException.class, () -> initializer.start(2)));
    }

    @Test
    void injectedOperationsMustBePresent() {
        assertThrows(NullPointerException.class, () -> new GameInitializer(null, state -> state));
        assertThrows(NullPointerException.class, () -> new GameInitializer(count -> table("table"), null));
    }

    // Immutable values only: no GameSetup, Deck or random source runs in these tests.
    private static GameState table(String id) {
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            Player player = new Player(id + seat, seat, PlayerType.HUMAN, 1000);
            players.add(new PlayerState(player, PlayerStatus.ACTIVE, List.of(), 0, 0));
        }
        return new GameState(new GameConfig(), id, GamePhase.SETUP, players,
                new HandPositions(0, 1, 2), id + "0", List.of(), List.of(),
                new BettingProgress(0, 20, Set.of(id + "0"), Set.of(id + "0")));
    }
}
