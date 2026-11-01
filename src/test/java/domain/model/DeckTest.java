package domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckTest {

    @Test
    void newDeckHas52Cards() {
        assertEquals(52, new Deck().remainingCards());
    }

    @Test
    void newDeckContainsEveryCardOnce() {
        Deck deck = new Deck();
        List<Card> cards = deck.deal(52);
        assertAll(
            () -> assertEquals(52, cards.size()),
            () -> assertEquals(52, new HashSet<>(cards).size()),
            () -> assertEquals(0, deck.remainingCards())
        );
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                assertTrue(cards.contains(new Card(rank, suit)), rank + " of " + suit);
            }
        }
    }

    @ParameterizedTest(name = "{0}: consumed {1}, request {2}")
    @CsvSource({
        "DEAL-V01, 0, 0, 52",
        "DEAL-V02, 0, 1, 51",
        "DEAL-V03, 0, 51, 1",
        "DEAL-V04, 0, 52, 0",
        "DEAL-V05, 1, 0, 51",
        "DEAL-V06, 1, 1, 50",
        "DEAL-V07, 1, 50, 1",
        "DEAL-V08, 1, 51, 0",
        "DEAL-V09, 12, 0, 40",
        "DEAL-V10, 12, 1, 39",
        "DEAL-V11, 12, 39, 1",
        "DEAL-V12, 12, 40, 0",
        "DEAL-V13, 51, 0, 1",
        "DEAL-V14, 51, 1, 0",
        "DEAL-V15, 52, 0, 0"
    })
    void dealsValidNumberOfCards(String caseId, int consumed, int count, int remaining) {
        Deck deck = new Deck();
        List<Card> prefix = deck.deal(consumed);
        List<Card> hand = deck.deal(count);
        assertAll(
            () -> assertEquals(consumed, prefix.size()),
            () -> assertEquals(count, hand.size()),
            () -> assertEquals(remaining, deck.remainingCards())
        );
        List<Card> allCards = new ArrayList<>(prefix);
        allCards.addAll(hand);
        allCards.addAll(deck.deal(remaining));
        assertAll(
            () -> assertEquals(52, allCards.size()),
            () -> assertEquals(52, new HashSet<>(allCards).size()),
            () -> assertEquals(0, deck.remainingCards())
        );
    }

    @Test
    void dealtHandIsReadOnlyAndIndependent() {
        Deck deck = new Deck();
        List<Card> hand = deck.deal(2);
        assertEquals(2, hand.size());
        List<Card> originalHand = new ArrayList<>(hand);
        assertThrows(UnsupportedOperationException.class, hand::clear);
        deck.deal(50);
        assertEquals(originalHand, hand);
        assertEquals(0, deck.remainingCards());
    }

    @ParameterizedTest(name = "{0}: consumed {1}, invalid request {2}")
    @MethodSource("invalidDeals")
    void rejectsInvalidDealWithoutChangingState(String caseId, int consumed, int count) {
        Deck deck = new Deck();
        Deck control = new Deck();
        deck.deal(consumed);
        control.deal(consumed);
        int remaining = control.remainingCards();
        List<Card> expectedCards = control.deal(remaining);

        assertThrows(IllegalArgumentException.class, () -> deck.deal(count));
        assertEquals(remaining, deck.remainingCards());
        assertEquals(expectedCards, deck.deal(remaining));
    }

    static Stream<Arguments> invalidDeals() {
        Stream.Builder<Arguments> cases = Stream.builder();
        int id = 1;
        for (int consumed : new int[] {0, 1, 12, 51, 52}) {
            int remaining = 52 - consumed;
            for (int count : new int[] {-1, Integer.MIN_VALUE, remaining + 1, Integer.MAX_VALUE}) {
                cases.add(Arguments.of(String.format("DEAL-I%02d", id++), consumed, count));
            }
        }
        return cases.build();
    }

    @Test
    void rejectsNullRandomSource() {
        assertThrows(NullPointerException.class, () -> new Deck(null));
    }

    @Test
    void shuffleUsesProvidedRandom() {
        List<Card> unshuffled = new Deck().deal(52);
        Deck first = new Deck(new Random(7));
        Deck sameSeed = new Deck(new Random(7));
        Deck otherSeed = new Deck(new Random(8));
        first.shuffle();
        sameSeed.shuffle();
        otherSeed.shuffle();
        List<Card> firstOrder = first.deal(52);
        List<Card> sameOrder = sameSeed.deal(52);
        List<Card> otherOrder = otherSeed.deal(52);
        assertAll(
            () -> assertEquals(firstOrder, sameOrder),
            () -> assertNotEquals(unshuffled, firstOrder),
            () -> assertNotEquals(firstOrder, otherOrder)
        );
    }

    @ParameterizedTest(name = "use upper random boundary: {0}")
    @CsvSource({"false", "true"})
    void shuffleUsesBoundaryRandomChoices(boolean useUpperBoundary) {
        Deck deck = new Deck(new BoundaryRandomStub(useUpperBoundary));
        deck.deal(50);
        List<Card> expected = new ArrayList<>(new Deck().deal(52).subList(50, 52));
        // With two cards, choice 0 swaps them; choice 1 keeps their order.
        if (!useUpperBoundary) {
            Collections.reverse(expected);
        }

        deck.shuffle();

        assertEquals(2, deck.remainingCards());
        assertEquals(expected, deck.deal(2));
        assertEquals(0, deck.remainingCards());
    }

    private static final class BoundaryRandomStub extends Random {
        private static final long serialVersionUID = 1L;
        private final boolean useUpperBoundary;

        private BoundaryRandomStub(boolean useUpperBoundary) {
            this.useUpperBoundary = useUpperBoundary;
        }

        @Override
        public int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("bound must be positive");
            }
            return useUpperBoundary ? bound - 1 : 0;
        }
    }

    @ParameterizedTest
    @CsvSource({"0, 52", "1, 51", "12, 40", "51, 1", "52, 0"})
    void shuffleOnlyChangesRemainingCardOrder(int consumed, int remaining) {
        Deck deck = new Deck(new Random(7));
        Deck control = new Deck();
        List<Card> dealt = deck.deal(consumed);
        control.deal(consumed);
        Set<Card> expected = new HashSet<>(control.deal(remaining));
        deck.shuffle();
        deck.shuffle();
        assertEquals(remaining, deck.remainingCards());
        List<Card> after = deck.deal(remaining);
        assertAll(
            () -> assertEquals(remaining, after.size()),
            () -> assertEquals(expected, new HashSet<>(after)),
            () -> assertTrue(Collections.disjoint(dealt, after))
        );
    }

    @Test
    void defaultDeckCanBeShuffled() {
        Deck deck = new Deck();
        deck.shuffle();
        assertEquals(52, deck.remainingCards());
        assertEquals(new HashSet<>(new Deck().deal(52)), new HashSet<>(deck.deal(52)));
    }
}
