package domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
}
