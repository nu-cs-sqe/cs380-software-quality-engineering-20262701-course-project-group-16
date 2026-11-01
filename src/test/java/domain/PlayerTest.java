package domain;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerTest {

    @ParameterizedTest(name = "{0}: rejects invalid player ID")
    @MethodSource("invalidIds")
    void rejectsInvalidIds(String caseId, String id) {
        assertThrows(IllegalArgumentException.class,
                () -> new Player(id, 2, PlayerType.HUMAN, 1_000L), caseId);
    }

    static Stream<Arguments> invalidIds() {
        return Stream.of(
                Arguments.of("P-C01", (String) null),
                Arguments.of("P-C02", ""),
                Arguments.of("P-C03", " "),
                Arguments.of("P-C04", "\t"),
                Arguments.of("P-C05", "\n"),
                Arguments.of("P-C06", "\u2003"));
    }

    @ParameterizedTest(name = "{0}: retains a valid player ID")
    @MethodSource("validIds")
    void retainsValidIds(String caseId, String id) {
        Player player = new Player(id, 2, PlayerType.HUMAN, 1_000L);

        assertEquals(id, player.getId(), caseId);
    }

    static Stream<Arguments> validIds() {
        return Stream.of(
                Arguments.of("P-C07", "p"),
                Arguments.of("P-C08", " player-2 "));
    }

    @ParameterizedTest(name = "{0}: rejects a seat outside the six-seat table")
    @MethodSource("invalidSeats")
    void rejectsInvalidSeats(String caseId, int seat) {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("player-1", seat, PlayerType.HUMAN, 1_000L), caseId);
    }

    static Stream<Arguments> invalidSeats() {
        return Stream.of(
                Arguments.of("P-C09", Integer.MIN_VALUE),
                Arguments.of("P-C10", -1),
                Arguments.of("P-C11", 6),
                Arguments.of("P-C12", Integer.MAX_VALUE));
    }

    @ParameterizedTest(name = "{0}: retains a valid seat")
    @MethodSource("validSeats")
    void retainsValidSeats(String caseId, int seat) {
        Player player = new Player("player-1", seat, PlayerType.HUMAN, 1_000L);

        assertEquals(seat, player.getSeat(), caseId);
    }

    static Stream<Arguments> validSeats() {
        return Stream.of(
                Arguments.of("P-C13", 0),
                Arguments.of("P-C14", 1),
                Arguments.of("P-C15", 4),
                Arguments.of("P-C16", 5));
    }

    @Test
    @DisplayName("P-C17: rejects a null player type")
    void rejectsNullPlayerType() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("player-1", 2, null, 1_000L), "P-C17");
    }

    @ParameterizedTest(name = "{0}: retains the player type")
    @MethodSource("validPlayerTypes")
    void retainsPlayerTypes(String caseId, PlayerType type) {
        Player player = new Player("player-1", 2, type, 1_000L);

        assertSame(type, player.getType(), caseId);
    }

    static Stream<Arguments> validPlayerTypes() {
        return Stream.of(
                Arguments.of("P-C18", PlayerType.HUMAN),
                Arguments.of("P-C19", PlayerType.BOT));
    }

    @ParameterizedTest(name = "{0}: rejects a negative chip stack")
    @MethodSource("invalidChipStacks")
    void rejectsNegativeChipStacks(String caseId, long chipStack) {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("player-1", 2, PlayerType.HUMAN, chipStack), caseId);
    }

    static Stream<Arguments> invalidChipStacks() {
        return Stream.of(
                Arguments.of("P-C20", Long.MIN_VALUE),
                Arguments.of("P-C21", -1L));
    }

    @ParameterizedTest(name = "{0}: retains a nonnegative chip stack")
    @MethodSource("validChipStacks")
    void retainsValidChipStacks(String caseId, long chipStack) {
        Player player = new Player("player-1", 2, PlayerType.HUMAN, chipStack);

        assertEquals(chipStack, player.getChipStack(), caseId);
    }

    static Stream<Arguments> validChipStacks() {
        return Stream.of(
                Arguments.of("P-C22", 0L),
                Arguments.of("P-C23", 1L),
                Arguments.of("P-C24", Long.MAX_VALUE - 1),
                Arguments.of("P-C25", Long.MAX_VALUE));
    }
}
