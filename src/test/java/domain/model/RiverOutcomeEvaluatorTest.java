package domain.model;

import domain.model.RiverOutcomeEvaluator.Outcome;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RiverOutcomeEvaluatorTest {
    private static final List<Card> ROYAL = List.of(new Card(Rank.TEN, Suit.SPADES),
            new Card(Rank.JACK, Suit.SPADES), new Card(Rank.QUEEN, Suit.SPADES),
            new Card(Rank.KING, Suit.SPADES), new Card(Rank.ACE, Suit.SPADES));
    private static final List<Card> LOW_HOLES = List.of(new Card(Rank.THREE, Suit.CLUBS),
            new Card(Rank.SIX, Suit.HEARTS));
    private final RiverOutcomeEvaluator outcomes = new RiverOutcomeEvaluator();

    @Test
    void royalFlushAndMaximalBoardQuadsLockTheBoard() {
        List<Card> quads = List.of(new Card(Rank.KING, Suit.CLUBS),
                new Card(Rank.KING, Suit.DIAMONDS), new Card(Rank.KING, Suit.HEARTS),
                new Card(Rank.KING, Suit.SPADES), new Card(Rank.ACE, Suit.CLUBS));
        assertEquals(Outcome.BOARD_LOCKED_TIE, outcomes.evaluate(ROYAL, LOW_HOLES, () -> false));
        assertEquals(Outcome.BOARD_LOCKED_TIE, outcomes.evaluate(quads, LOW_HOLES, () -> false));
    }

    @Test
    void aBroadwayWithThreeSpadesAndALowerStraightFlushCanBeBeaten() {
        List<Card> broadway = List.of(new Card(Rank.ACE, Suit.SPADES),
                new Card(Rank.KING, Suit.SPADES), new Card(Rank.QUEEN, Suit.SPADES),
                new Card(Rank.JACK, Suit.HEARTS), new Card(Rank.TEN, Suit.DIAMONDS));
        List<Card> lowerStraightFlush = List.of(new Card(Rank.FIVE, Suit.SPADES),
                new Card(Rank.SIX, Suit.SPADES), new Card(Rank.SEVEN, Suit.SPADES),
                new Card(Rank.EIGHT, Suit.SPADES), new Card(Rank.NINE, Suit.SPADES));
        assertEquals(Outcome.UNCERTAIN, outcomes.evaluate(broadway, LOW_HOLES, () -> false));
        assertEquals(Outcome.UNCERTAIN,
                outcomes.evaluate(lowerStraightFlush, LOW_HOLES, () -> false));
    }

    @Test
    void privateRoyalsGuaranteeAWinWhileOtherNutsCanTie() {
        List<Card> board = List.of(new Card(Rank.ACE, Suit.SPADES),
                new Card(Rank.KING, Suit.SPADES), new Card(Rank.QUEEN, Suit.SPADES),
                new Card(Rank.FOUR, Suit.DIAMONDS), new Card(Rank.NINE, Suit.HEARTS));
        assertEquals(Outcome.GUARANTEED_WIN, outcomes.evaluate(board, ROYAL.subList(0, 2),
                () -> false));
        List<Card> unsuitedBoard = List.of(new Card(Rank.TWO, Suit.CLUBS),
                new Card(Rank.SEVEN, Suit.DIAMONDS), new Card(Rank.NINE, Suit.HEARTS),
                new Card(Rank.JACK, Suit.SPADES), new Card(Rank.KING, Suit.CLUBS));
        List<Card> nuts = List.of(new Card(Rank.QUEEN, Suit.DIAMONDS),
                new Card(Rank.TEN, Suit.HEARTS));
        assertEquals(Outcome.GUARANTEED_NON_LOSS,
                outcomes.evaluate(unsuitedBoard, nuts, () -> false));
    }

    @ParameterizedTest
    @CsvSource({"100,99,99,GUARANTEED_WIN", "100,100,100,GUARANTEED_TIE",
        "100,99,100,GUARANTEED_NON_LOSS", "100,101,101,GUARANTEED_LOSS",
        "100,99,101,UNCERTAIN", "100,100,101,UNCERTAIN"})
    void comparesOwnRankWithTheExactOpponentBounds(int own, int min, int max, Outcome expected) {
        assertEquals(expected, RiverOutcomeEvaluator.classify(own,
                IntStream.of(min, max).summaryStatistics()));
    }

    @Test
    void rejectsWrongCardCountsAndADuplicateAcrossBoardAndHolding() {
        assertThrows(IllegalArgumentException.class,
                () -> outcomes.evaluate(ROYAL.subList(0, 4), LOW_HOLES, () -> false));
        assertThrows(IllegalArgumentException.class,
                () -> outcomes.evaluate(ROYAL, LOW_HOLES.subList(0, 1), () -> false));
        assertThrows(IllegalArgumentException.class, () -> outcomes.evaluate(ROYAL,
                List.of(ROYAL.get(0), LOW_HOLES.get(0)), () -> false));
    }

    @Test
    void cancellationStopsAnEnumerationAlreadyInProgress() {
        AtomicInteger groups = new AtomicInteger();
        assertThrows(CancellationException.class,
                () -> outcomes.evaluate(ROYAL, LOW_HOLES, () -> groups.incrementAndGet() == 3));
        assertEquals(3, groups.get());
    }

    @Test
    void sevenDistinctCardsMustStillBeSplitIntoFiveBoardCardsAndTwoHoleCards() {
        Card extra = new Card(Rank.KING, Suit.DIAMONDS);
        assertThrows(IllegalArgumentException.class, () -> outcomes.evaluate(ROYAL.subList(0, 4),
                List.of(LOW_HOLES.get(0), LOW_HOLES.get(1), extra), () -> false));
        assertThrows(IllegalArgumentException.class, () -> outcomes.evaluate(
                List.of(ROYAL.get(0), ROYAL.get(1), ROYAL.get(2), ROYAL.get(3),
                        ROYAL.get(4), extra),
                LOW_HOLES.subList(0, 1), () -> false));
    }

    @Test
    void interruptionCancelsEvenWhenTheSuppliedFlagIsClear() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class,
                    () -> outcomes.evaluate(ROYAL, LOW_HOLES, () -> false));
        } finally {
            Thread.interrupted();
        }
    }
}
