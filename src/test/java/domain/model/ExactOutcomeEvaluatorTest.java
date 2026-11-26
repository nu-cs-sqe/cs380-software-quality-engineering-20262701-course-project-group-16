package domain.model;

import domain.model.RiverOutcomeEvaluator.Outcome;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExactOutcomeEvaluatorTest {
    private static final List<Card> HEARTS_FLOP = List.of(new Card(Rank.TEN, Suit.HEARTS),
            new Card(Rank.JACK, Suit.HEARTS), new Card(Rank.QUEEN, Suit.HEARTS));
    private static final List<Card> ROYAL_HOLES = List.of(new Card(Rank.ACE, Suit.HEARTS),
            new Card(Rank.KING, Suit.HEARTS));
    private static final List<Card> ACES = List.of(new Card(Rank.ACE, Suit.SPADES),
            new Card(Rank.ACE, Suit.HEARTS));
    private static final List<Card> QUADS_FLOP = List.of(new Card(Rank.ACE, Suit.DIAMONDS),
            new Card(Rank.ACE, Suit.CLUBS), new Card(Rank.TEN, Suit.SPADES));
    private static final List<Card> KING_FLUSH_TURN = List.of(new Card(Rank.TEN, Suit.SPADES),
            new Card(Rank.JACK, Suit.SPADES), new Card(Rank.QUEEN, Suit.SPADES),
            new Card(Rank.ACE, Suit.DIAMONDS));
    private static final List<Card> KING_FLUSH_HOLES = List.of(new Card(Rank.NINE, Suit.SPADES),
            new Card(Rank.KING, Suit.SPADES));

    @Test
    void aPrivateRoyalProvesAWinEvenWithZeroComparisonBudget() {
        ExactOutcomeEvaluator evaluator = new ExactOutcomeEvaluator(0);
        List<Card> turn = List.of(HEARTS_FLOP.get(0), HEARTS_FLOP.get(1), HEARTS_FLOP.get(2),
                new Card(Rank.TWO, Suit.CLUBS));
        assertEquals(Outcome.GUARANTEED_WIN, evaluator.evaluate(HEARTS_FLOP, ROYAL_HOLES,
                () -> false));
        assertEquals(Outcome.GUARANTEED_WIN, evaluator.evaluate(turn, ROYAL_HOLES, () -> false));
    }

    @Test
    void aPublicRoyalFlushLocksTheRiverBoard() {
        List<Card> board = List.of(HEARTS_FLOP.get(0), HEARTS_FLOP.get(1), HEARTS_FLOP.get(2),
                ROYAL_HOLES.get(0), ROYAL_HOLES.get(1));
        List<Card> holes = List.of(new Card(Rank.TWO, Suit.CLUBS),
                new Card(Rank.THREE, Suit.CLUBS));
        assertEquals(Outcome.BOARD_LOCKED_TIE,
                new ExactOutcomeEvaluator(0).evaluate(board, holes, () -> false));
    }

    @Test
    void aceQuadsNeedTheSuitLimitToBeSafeBeforeTheRiver() {
        ExactOutcomeEvaluator evaluator = new ExactOutcomeEvaluator();
        List<Card> safeTurn = List.of(QUADS_FLOP.get(0), QUADS_FLOP.get(1), QUADS_FLOP.get(2),
                new Card(Rank.SIX, Suit.HEARTS));
        List<Card> vulnerableTurn = List.of(QUADS_FLOP.get(0), QUADS_FLOP.get(1),
                QUADS_FLOP.get(2), new Card(Rank.NINE, Suit.SPADES));
        assertEquals(Outcome.GUARANTEED_WIN, evaluator.evaluate(safeTurn, ACES, () -> false));
        assertEquals(Outcome.UNCERTAIN, evaluator.evaluate(QUADS_FLOP, ACES, () -> false));
        assertEquals(Outcome.UNCERTAIN, evaluator.evaluate(vulnerableTurn, ACES, () -> false));
    }

    @ParameterizedTest
    @CsvSource({"0,UNCERTAIN", "45539,UNCERTAIN", "45540,GUARANTEED_WIN"})
    void aTurnSearchNeedsEveryLegalComparisonBeforeProvingAWin(int limit, Outcome expected) {
        assertEquals(expected, new ExactOutcomeEvaluator(limit).evaluate(KING_FLUSH_TURN,
                KING_FLUSH_HOLES, () -> false));
    }

    @Test
    void publicAceQuadsWithAPrivateKingCanStillTie() {
        List<Card> board = List.of(new Card(Rank.ACE, Suit.CLUBS),
                new Card(Rank.ACE, Suit.DIAMONDS), new Card(Rank.ACE, Suit.HEARTS),
                new Card(Rank.ACE, Suit.SPADES));
        List<Card> holes = List.of(new Card(Rank.KING, Suit.CLUBS),
                new Card(Rank.TWO, Suit.DIAMONDS));
        assertEquals(Outcome.UNCERTAIN,
                new ExactOutcomeEvaluator().evaluate(board, holes, () -> false));
        assertEquals(Outcome.GUARANTEED_NON_LOSS,
                new ExactOutcomeEvaluator(45_540).evaluate(board, holes, () -> false));
    }

    @Test
    void rejectsOutOfRangeAndRepeatedKnownCards() {
        ExactOutcomeEvaluator evaluator = new ExactOutcomeEvaluator();
        assertThrows(IllegalArgumentException.class, () -> new ExactOutcomeEvaluator(-1));
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(HEARTS_FLOP.subList(0, 2), ACES, () -> false));
        List<Card> sixCards = List.of(HEARTS_FLOP.get(0), HEARTS_FLOP.get(1), HEARTS_FLOP.get(2),
                new Card(Rank.FOUR, Suit.CLUBS), new Card(Rank.FIVE, Suit.CLUBS),
                new Card(Rank.SIX, Suit.CLUBS));
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(sixCards, ACES, () -> false));
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(HEARTS_FLOP, ACES.subList(0, 1), () -> false));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(HEARTS_FLOP,
                List.of(HEARTS_FLOP.get(0), ACES.get(0)), () -> false));
    }

    @Test
    void cancellationAbortsWorkOnAnIncompleteBoard() {
        ExactOutcomeEvaluator evaluator = new ExactOutcomeEvaluator();
        AtomicInteger polls = new AtomicInteger();
        assertThrows(CancellationException.class, () -> evaluator.evaluate(KING_FLUSH_TURN,
                KING_FLUSH_HOLES, () -> polls.incrementAndGet() == 4));
        assertEquals(4, polls.get());
        assertThrows(CancellationException.class,
                () -> evaluator.evaluate(HEARTS_FLOP, ROYAL_HOLES, () -> true));
    }

    @Test
    void interruptionAbortsAnIncompleteBoardDespiteAClearFlag() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> new ExactOutcomeEvaluator()
                    .evaluate(HEARTS_FLOP, ROYAL_HOLES, () -> false));
        } finally {
            Thread.interrupted();
        }
    }
}
