package domain.model;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BettingHistoryTest {
    @Test
    @DisplayName("BH-01: a new unraised hand has no aggressor or continuation bet")
    void defaultHistoryIsEmpty() {
        BettingHistory history = new BettingHistory();
        assertTrue(history.getFinalPreflopAggressorId().isEmpty());
        assertTrue(history.getFlopCBetAmount().isEmpty());
    }

    @ParameterizedTest(name = "BH-02-{0}: explicit aggressor and c-bet amount")
    @ValueSource(longs = {0, 1, 20, Long.MAX_VALUE})
    void explicitHistoryRetainsIdentityAndOptionalAmount(long amount) {
        BettingHistory history = new BettingHistory("p1", amount);
        assertEquals("p1", history.getFinalPreflopAggressorId().orElseThrow());
        if (amount == 0) {
            assertTrue(history.getFlopCBetAmount().isEmpty());
        } else {
            assertEquals(amount, history.getFlopCBetAmount().orElseThrow());
        }
    }

    @Test
    @DisplayName("BH-03: explicit absence matches the default")
    void explicitNoAggressorIsAllowedWithoutACBet() {
        BettingHistory history = new BettingHistory(null, 0);
        assertTrue(history.getFinalPreflopAggressorId().isEmpty());
        assertTrue(history.getFlopCBetAmount().isEmpty());
    }

    @ParameterizedTest(name = "BH-04: reject blank aggressor [{0}]")
    @ValueSource(strings = {"", " ", "\t"})
    void blankAggressorsAreRejected(String id) {
        assertThrows(IllegalArgumentException.class, () -> new BettingHistory(id, 0));
    }

    @ParameterizedTest(name = "BH-05-{0}: reject a negative c-bet amount")
    @ValueSource(longs = {-1, Long.MIN_VALUE})
    void negativeAmountsAreRejected(long amount) {
        assertThrows(IllegalArgumentException.class, () -> new BettingHistory("p1", amount));
    }

    @ParameterizedTest(name = "BH-06-{0}: an actual c-bet requires a preflop aggressor")
    @ValueSource(longs = {1, 20})
    void aCBetCannotExistWithoutItsAggressor(long amount) {
        assertThrows(IllegalArgumentException.class, () -> new BettingHistory(null, amount));
    }

    @Test
    @DisplayName("GS-H01: the original GameState constructor supplies empty history")
    void existingStateConstructorRemainsCompatible() {
        GameState state = FoundationFixtures.fixture().build();
        assertTrue(state.getBettingHistory().getFinalPreflopAggressorId().isEmpty());
        assertTrue(state.getBettingHistory().getFlopCBetAmount().isEmpty());
    }

    @Test
    @DisplayName("GS-H02: reject a null history")
    void nullStateHistoryIsRejected() {
        GameState before = PostflopFixtures.street(GamePhase.FLOP).build();
        assertThrows(IllegalArgumentException.class, () -> PostflopFixtures.withHistory(before, null));
    }

    @Test
    @DisplayName("GS-H03: reject an unknown aggressor")
    void unknownAggressorIsRejected() {
        GameState before = PostflopFixtures.street(GamePhase.FLOP).build();
        assertThrows(IllegalArgumentException.class,
                () -> PostflopFixtures.withHistory(before, new BettingHistory("unknown", 0)));
    }

    @Test
    @DisplayName("GS-H04: setup cannot contain accepted preflop aggression")
    void setupCannotHaveAnAggressor() {
        GameState before = FoundationFixtures.fixture().build();
        assertThrows(IllegalArgumentException.class,
                () -> PostflopFixtures.withHistory(before, new BettingHistory("p1", 0)));
    }

    @Test
    @DisplayName("GS-H05: preflop permits aggression but not a flop c-bet")
    void preflopHistoryCannotAlreadyContainACBet() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.deal();
        GameState before = fixture.build();
        BettingHistory aggression = new BettingHistory("p1", 0);
        GameState after = PostflopFixtures.withHistory(before, aggression);
        assertEquals("p1", after.getBettingHistory().getFinalPreflopAggressorId().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> PostflopFixtures.withHistory(before, new BettingHistory("p1", 20)));
    }

    @ParameterizedTest(name = "GS-H06: recorded c-bet 20 with actual flop contribution {0}")
    @CsvSource({"19, false", "20, true", "21, true"})
    void flopHistoryMustBeBackedByCommittedChips(long committed, boolean valid) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(GamePhase.FLOP);
        PostflopFixtures.contribution(fixture, 0, committed);
        fixture.betting = new BettingProgress(committed, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        BettingHistory history = new BettingHistory("p0", 20);
        if (valid) {
            GameState state = PostflopFixtures.build(fixture, history);
            assertEquals(20, state.getBettingHistory().getFlopCBetAmount().orElseThrow());
        } else {
            assertThrows(IllegalArgumentException.class, () -> PostflopFixtures.build(fixture, history));
        }
    }

    @Test
    @DisplayName("GS-H07: an eliminated seat cannot be a current-hand aggressor")
    void anEliminatedAggressorIsRejectedBeforeCompletion() {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(GamePhase.FLOP);
        fixture.player(4, 0, PlayerStatus.ELIMINATED, 0, 0);
        fixture.player(0, 1960, PlayerStatus.ACTIVE, 0, 40);
        fixture.betting = new BettingProgress(0, 20, Set.of("p0", "p1", "p2", "p3", "p5"), Set.of());
        assertThrows(IllegalArgumentException.class,
                () -> PostflopFixtures.build(fixture, new BettingHistory("p4", 0)));
    }

    @Test
    @DisplayName("GS-H08: a completed hand retains historical aggression after elimination/payout")
    void completedHistoryCanOutlivePlayerElimination() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.player(4, 0, PlayerStatus.ELIMINATED, 0, 0);
        fixture.player(0, 2000, PlayerStatus.ACTIVE, 0, 0);
        fixture.phase = GamePhase.COMPLETION;
        fixture.actor = null;
        fixture.betting = new BettingProgress(0, 20, Set.of(), Set.of());
        GameState state = PostflopFixtures.build(fixture, new BettingHistory("p4", 20));
        assertEquals("p4", state.getBettingHistory().getFinalPreflopAggressorId().orElseThrow());
        assertEquals(20, state.getBettingHistory().getFlopCBetAmount().orElseThrow());
    }

    @ParameterizedTest(name = "GS-H09: historical c-bet survives advancement from {0}")
    @CsvSource({"FLOP, TURN", "TURN, RIVER", "RIVER, SHOWDOWN"})
    void advancingDoesNotLoseBettingHistory(GamePhase phase, GamePhase expected) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(phase);
        if (phase == GamePhase.FLOP) {
            for (int seat = 0; seat < 6; seat++) {
                PostflopFixtures.contribution(fixture, seat, 20);
            }
            fixture.betting = new BettingProgress(20, 20, Set.of(), Set.of());
        }
        PostflopFixtures.close(fixture);
        GameState before = PostflopFixtures.build(fixture, new BettingHistory("p0", 20));

        GameState after = new BettingEngine().advanceStreet(before);

        assertEquals(expected, after.getPhase());
        assertEquals("p0", after.getBettingHistory().getFinalPreflopAggressorId().orElseThrow());
        assertEquals(20, after.getBettingHistory().getFlopCBetAmount().orElseThrow());
        assertEquals(20, before.getBettingHistory().getFlopCBetAmount().orElseThrow());
    }

    @ParameterizedTest(name = "GS-H10-{0}: historic c-bet cannot exceed committed hand chips")
    @CsvSource({"TURN", "RIVER"})
    void historicalCBetRequiresEnoughHandCommitment(GamePhase phase) {
        FoundationFixtures.Fixture fixture = PostflopFixtures.street(phase);
        assertThrows(IllegalArgumentException.class,
                () -> PostflopFixtures.build(fixture, new BettingHistory("p0", 41)));
    }
}
