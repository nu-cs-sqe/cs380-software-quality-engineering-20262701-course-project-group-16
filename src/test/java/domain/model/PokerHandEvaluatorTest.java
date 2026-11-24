package domain.model;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PokerHandEvaluatorTest {
    private final HandEvaluator evaluator = new PokerHandEvaluator();

    @Test
    void categoryRequiresFiveToSevenDistinctCards() {
        PokerHandEvaluator ranker = new PokerHandEvaluator();
        for (int count : List.of(5, 6, 7)) {
            assertEquals("HIGH_CARD", ranker.category(cards(count)));
        }
        for (int count : List.of(4, 8)) {
            assertThrows(IllegalArgumentException.class, () -> ranker.category(cards(count)));
        }
        List<Card> duplicate = cards(5);
        duplicate.set(4, duplicate.get(0));
        assertThrows(IllegalArgumentException.class, () -> ranker.category(duplicate));
    }

    @Test
    void bestFiveUsesTheHoleCardCompletingTheStraightAndKeepsTheBoardOrder() {
        PokerHandEvaluator ranker = new PokerHandEvaluator();
        List<Card> board = cards(5);
        List<Card> holding = new ArrayList<>(board);
        holding.add(new Card(Rank.TWO, Suit.CLUBS));
        Card ten = new Card(Rank.TEN, Suit.DIAMONDS);
        holding.add(ten);
        assertEquals(List.of(board.get(0), board.get(1), board.get(2), board.get(3), ten),
                ranker.winningCards(holding));
        assertEquals(board, ranker.winningCards(board));
        assertThrows(IllegalArgumentException.class, () -> ranker.winningCards(cards(4)));
    }

    @Test
    void theFirstCardCanBeTheOnlyCardOutsideTheWinningFive() {
        List<Card> straight = new ArrayList<>(cards(4));
        straight.add(new Card(Rank.TEN, Suit.DIAMONDS));
        List<Card> holding = new ArrayList<>(List.of(new Card(Rank.TWO, Suit.CLUBS)));
        holding.addAll(straight);

        assertEquals(straight, new PokerHandEvaluator().winningCards(holding));
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
        List<Card> originalFirst = new ArrayList<>(first);
        List<Card> originalSecond = new ArrayList<>(second);
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
