package domain.model;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PokerHandEvaluatorTest {
    private final HandEvaluator evaluator = new PokerHandEvaluator();

    @ParameterizedTest(name = "{0}")
    @CsvSource({"PHE-N01, 1", "PHE-N02, 2"})
    void rejectsNullList(String caseId, int argument) {
        List<Card> first = argument == 1 ? null : cards(7);
        List<Card> second = argument == 2 ? null : cards(7);
        assertRejected(NullPointerException.class, first, second, caseId);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "PHE-N03, 1, 0", "PHE-N04, 1, 3", "PHE-N05, 1, 6",
        "PHE-N06, 2, 0", "PHE-N07, 2, 3", "PHE-N08, 2, 6"
    })
    void rejectsNullCard(String caseId, int argument, int position) {
        List<Card> first = cards(7);
        List<Card> second = cards(7);
        (argument == 1 ? first : second).set(position, null);
        assertRejected(NullPointerException.class, first, second, caseId);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "PHE-S01, 1, 0", "PHE-S02, 1, 6", "PHE-S03, 1, 8",
        "PHE-S04, 2, 0", "PHE-S05, 2, 6", "PHE-S06, 2, 8"
    })
    void rejectsCardCountOutsideSeven(String caseId, int argument, int count) {
        List<Card> first = cards(argument == 1 ? count : 7);
        List<Card> second = cards(argument == 2 ? count : 7);
        assertRejected(IllegalArgumentException.class, first, second, caseId);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"PHE-D01, 1", "PHE-D02, 2"})
    void rejectsDuplicateCardValues(String caseId, int argument) {
        List<Card> first = cards(7);
        List<Card> second = cards(7);
        List<Card> invalid = argument == 1 ? first : second;
        invalid.set(6, new Card(Rank.ACE, Suit.HEARTS));
        assertRejected(IllegalArgumentException.class, first, second, caseId);
    }

    private void assertRejected(Class<? extends Throwable> exception,
            List<Card> first, List<Card> second, String caseId) {
        List<Card> originalFirst = first == null ? null : new ArrayList<>(first);
        List<Card> originalSecond = second == null ? null : new ArrayList<>(second);
        assertThrows(exception, () -> evaluator.compare(first, second), caseId);
        assertEquals(originalFirst, first, caseId + ": first input must stay unchanged");
        assertEquals(originalSecond, second, caseId + ": second input must stay unchanged");
    }

    private static List<Card> cards(int count) {
        List<Card> pool = List.of(
                new Card(Rank.ACE, Suit.HEARTS),
                new Card(Rank.KING, Suit.SPADES),
                new Card(Rank.QUEEN, Suit.DIAMONDS),
                new Card(Rank.JACK, Suit.CLUBS),
                new Card(Rank.NINE, Suit.HEARTS),
                new Card(Rank.FOUR, Suit.SPADES),
                new Card(Rank.TWO, Suit.CLUBS),
                new Card(Rank.TEN, Suit.DIAMONDS));
        return new ArrayList<>(pool.subList(0, count));
    }
}
