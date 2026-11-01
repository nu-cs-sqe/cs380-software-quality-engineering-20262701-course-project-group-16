package domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameStateTest {

    @ParameterizedTest(name = "{0}: rejects invalid player collections")
    @MethodSource("invalidPlayerCollections")
    void rejectsInvalidPlayerCollections(String caseId, List<Player> players) {
        assertThrows(IllegalArgumentException.class,
                () -> createState(players), caseId);
    }

    static Stream<Arguments> invalidPlayerCollections() {
        List<Player> nullFirst = playersWithStack(1_000L);
        nullFirst.set(0, null);
        List<Player> nullLast = playersWithStack(1_000L);
        nullLast.set(5, null);
        List<Player> duplicateSeat = playersWithStack(1_000L);
        duplicateSeat.set(5, new Player("other-player", 0, PlayerType.BOT, 1_000L));
        List<Player> duplicateId = playersWithStack(1_000L);
        duplicateId.set(5, new Player("player-0", 5, PlayerType.BOT, 1_000L));
        return Stream.of(
                Arguments.of("G-P01", (List<Player>) null),
                Arguments.of("G-P02", playersWithCount(0)),
                Arguments.of("G-P03", playersWithCount(1)),
                Arguments.of("G-P04", playersWithCount(5)),
                Arguments.of("G-P05", playersWithCount(7)),
                Arguments.of("G-P06", nullFirst),
                Arguments.of("G-P07", nullLast),
                Arguments.of("G-P08", duplicateSeat),
                Arguments.of("G-P09", duplicateId));
    }

    @ParameterizedTest(name = "{0}: returns players in seat order")
    @MethodSource("validPlayerCollections")
    void returnsPlayersInSeatOrder(String caseId, List<Player> players) {
        GameState state = createState(players);

        assertEquals(6, state.getPlayers().size(), caseId);
        for (int seat = 0; seat < 6; seat++) {
            Player player = state.getPlayers().get(seat);
            assertEquals(seat, player.getSeat(), caseId);
            assertEquals("player-" + seat, player.getId(), caseId);
            assertEquals(PlayerType.HUMAN, player.getType(), caseId);
            assertEquals(1_000L, player.getChipStack(), caseId);
        }
    }

    static Stream<Arguments> validPlayerCollections() {
        List<Player> reversed = playersWithStack(1_000L);
        Collections.reverse(reversed);
        return Stream.of(
                Arguments.of("G-P10", playersWithStack(1_000L)),
                Arguments.of("G-P11", reversed));
    }

    @Test
    @DisplayName("G-I01: copying the source list protects the snapshot")
    void copiesTheSourcePlayerList() {
        List<Player> players = playersWithStack(1_000L);
        GameState state = createState(players);

        players.set(0, new Player("replacement", 0, PlayerType.BOT, 0L));

        assertEquals("player-0", state.getPlayers().get(0).getId());
        assertEquals(1_000L, state.getPlayers().get(0).getChipStack());
    }

    @Test
    @DisplayName("G-I02: the returned player list cannot be changed")
    void returnsAnUnmodifiablePlayerList() {
        GameState state = createState(playersWithStack(1_000L));

        assertThrows(UnsupportedOperationException.class,
                () -> state.getPlayers().set(0,
                        new Player("replacement", 0, PlayerType.BOT, 0L)));
    }

    @ParameterizedTest(name = "{0}: rejects a role seat outside 0..5")
    @MethodSource("invalidPositions")
    void rejectsInvalidPositions(String caseId, int dealer, int smallBlind,
            int bigBlind, int actor) {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(playersWithStack(1_000L), dealer, smallBlind,
                        bigBlind, actor, new long[6]), caseId);
    }

    static Stream<Arguments> invalidPositions() {
        return Stream.of(
                Arguments.of("G-R01", -1, 1, 2, 3),
                Arguments.of("G-R06", 6, 1, 2, 3),
                Arguments.of("G-R07", 0, -1, 2, 3),
                Arguments.of("G-R12", 0, 6, 2, 3),
                Arguments.of("G-R13", 0, 2, -1, 3),
                Arguments.of("G-R18", 0, 2, 6, 3),
                Arguments.of("G-R19", 0, 1, 2, -1),
                Arguments.of("G-R24", 0, 1, 2, 6));
    }

    @ParameterizedTest(name = "{0}: retains valid role seats")
    @MethodSource("validPositions")
    void retainsValidPositions(String caseId, int dealer, int smallBlind,
            int bigBlind, int actor) {
        GameState state = new GameState(playersWithStack(1_000L), dealer,
                smallBlind, bigBlind, actor, new long[6]);

        assertEquals(dealer, state.getDealerSeat(), caseId);
        assertEquals(smallBlind, state.getSmallBlindSeat(), caseId);
        assertEquals(bigBlind, state.getBigBlindSeat(), caseId);
        assertEquals(actor, state.getCurrentActorSeat(), caseId);
    }

    static Stream<Arguments> validPositions() {
        return Stream.of(
                Arguments.of("G-R02", 0, 1, 2, 3),
                Arguments.of("G-R03", 1, 1, 2, 3),
                Arguments.of("G-R04", 4, 1, 2, 3),
                Arguments.of("G-R05", 5, 1, 2, 3),
                Arguments.of("G-R08", 0, 0, 2, 3),
                Arguments.of("G-R09", 0, 1, 2, 3),
                Arguments.of("G-R10", 0, 4, 2, 3),
                Arguments.of("G-R11", 0, 5, 2, 3),
                Arguments.of("G-R14", 0, 2, 0, 3),
                Arguments.of("G-R15", 0, 2, 1, 3),
                Arguments.of("G-R16", 0, 2, 4, 3),
                Arguments.of("G-R17", 0, 2, 5, 3),
                Arguments.of("G-R20", 0, 1, 2, 0),
                Arguments.of("G-R21", 0, 1, 2, 1),
                Arguments.of("G-R22", 0, 1, 2, 4),
                Arguments.of("G-R23", 0, 1, 2, 5));
    }

    @Test
    @DisplayName("G-R25: small and big blinds cannot occupy the same seat")
    void rejectsMatchingBlindSeats() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(playersWithStack(1_000L), 0, 2, 2, 3, new long[6]));
    }

    @ParameterizedTest(name = "{0}: rejects invalid commitments")
    @MethodSource("invalidCommitments")
    void rejectsInvalidCommitments(String caseId, long[] committed) {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(playersWithStack(1_000L), 0, 1, 2, 3, committed),
                caseId);
    }

    static Stream<Arguments> invalidCommitments() {
        return Stream.of(
                Arguments.of("G-C01", (long[]) null),
                Arguments.of("G-C02", new long[0]),
                Arguments.of("G-C03", new long[5]),
                Arguments.of("G-C04", new long[7]),
                Arguments.of("G-C05", commitmentAt(0, -1L)),
                Arguments.of("G-C06", commitmentAt(2, -1L)),
                Arguments.of("G-C07", commitmentAt(5, -1L)),
                Arguments.of("G-C08", commitmentAt(3, Long.MIN_VALUE)));
    }

    @Test
    @DisplayName("G-C09: six zero commitments are valid")
    void acceptsSixZeroCommitments() {
        GameState state = createState(playersWithStack(1_000L));

        for (int seat = 0; seat < 6; seat++) {
            assertEquals(0L, state.getCommittedChips(seat));
        }
    }

    @ParameterizedTest(name = "{0}: rejects an invalid commitment lookup seat")
    @MethodSource("invalidLookupSeats")
    void rejectsInvalidLookupSeats(String caseId, int seat) {
        GameState state = new GameState(playersWithStack(1_000L), 0, 1, 2, 3,
                new long[] {10, 20, 30, 40, 50, 60});

        assertThrows(IllegalArgumentException.class,
                () -> state.getCommittedChips(seat), caseId);
    }

    static Stream<Arguments> invalidLookupSeats() {
        return Stream.of(
                Arguments.of("G-Q01", -1),
                Arguments.of("G-Q06", 6));
    }

    @ParameterizedTest(name = "{0}: returns the commitment at a valid seat")
    @MethodSource("validLookupSeats")
    void returnsCommitmentsBySeat(String caseId, int seat, long expected) {
        GameState state = new GameState(playersWithStack(1_000L), 0, 1, 2, 3,
                new long[] {10, 20, 30, 40, 50, 60});

        assertEquals(expected, state.getCommittedChips(seat), caseId);
    }

    static Stream<Arguments> validLookupSeats() {
        return Stream.of(
                Arguments.of("G-Q02", 0, 10L),
                Arguments.of("G-Q03", 1, 20L),
                Arguments.of("G-Q04", 4, 50L),
                Arguments.of("G-Q05", 5, 60L));
    }

    @Test
    @DisplayName("G-I03: source-array mutation cannot change commitments")
    void copiesTheSourceCommitments() {
        long[] committed = {10, 20, 30, 40, 50, 60};
        GameState state = new GameState(playersWithStack(1_000L), 0, 1, 2, 3,
                committed);

        committed[0] = 999L;

        assertEquals(10L, state.getCommittedChips(0));
    }

    private static long[] commitmentAt(int seat, long amount) {
        long[] committed = new long[6];
        committed[seat] = amount;
        return committed;
    }

    @ParameterizedTest(name = "{0}: totals stacks and commitments without double counting")
    @MethodSource("validAccountingStates")
    void calculatesChipTotals(String caseId, long[] stacks, long[] committed,
            long expectedPot, long expectedTotal) {
        GameState state = new GameState(playersWithStacks(stacks), 0, 1, 2, 3,
                committed);

        assertEquals(expectedPot, state.getPot(), caseId);
        assertEquals(expectedTotal, state.getTotalChips(), caseId);
    }

    static Stream<Arguments> validAccountingStates() {
        return Stream.of(
                Arguments.of("G-A01", new long[6], new long[6], 0L, 0L),
                Arguments.of("G-A02", new long[] {1000, 1000, 1000, 1000, 1000, 1000},
                        new long[6], 0L, 6000L),
                Arguments.of("G-A03", new long[] {1000, 990, 980, 1000, 1000, 1000},
                        new long[] {0, 10, 20, 0, 0, 0}, 30L, 6000L),
                Arguments.of("G-A04", new long[] {999, 998, 997, 996, 995, 994},
                        new long[] {1, 2, 3, 4, 5, 6}, 21L, 6000L),
                Arguments.of("G-A05", new long[6],
                        new long[] {Long.MAX_VALUE, 0, 0, 0, 0, 0},
                        Long.MAX_VALUE, Long.MAX_VALUE),
                Arguments.of("G-A06", new long[] {Long.MAX_VALUE - 1, 0, 0, 0, 0, 0},
                        new long[] {1, 0, 0, 0, 0, 0}, 1L, Long.MAX_VALUE));
    }

    @ParameterizedTest(name = "{0}: rejects chip-total overflow")
    @MethodSource("overflowStates")
    void rejectsChipTotalOverflow(String caseId, long[] stacks, long[] committed) {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(playersWithStacks(stacks), 0, 1, 2, 3, committed),
                caseId);
    }

    static Stream<Arguments> overflowStates() {
        return Stream.of(
                Arguments.of("G-A07", new long[6],
                        new long[] {Long.MAX_VALUE, 1, 0, 0, 0, 0}),
                Arguments.of("G-A08", new long[] {Long.MAX_VALUE, 1, 0, 0, 0, 0},
                        new long[6]),
                Arguments.of("G-A09", new long[] {Long.MAX_VALUE, 0, 0, 0, 0, 0},
                        new long[] {0, 0, 0, 0, 0, 1}));
    }

    private static List<Player> playersWithStacks(long[] stacks) {
        List<Player> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            players.add(new Player("player-" + seat, seat, PlayerType.HUMAN, stacks[seat]));
        }
        return players;
    }

    private static GameState createState(List<Player> players) {
        return new GameState(players, 0, 1, 2, 3, new long[6]);
    }

    private static List<Player> playersWithStack(long stack) {
        List<Player> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            players.add(new Player("player-" + seat, seat, PlayerType.HUMAN, stack));
        }
        return players;
    }

    private static List<Player> playersWithCount(int count) {
        List<Player> players = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            players.add(new Player("player-" + index, index % 6,
                    PlayerType.HUMAN, 1_000L));
        }
        return players;
    }
}
