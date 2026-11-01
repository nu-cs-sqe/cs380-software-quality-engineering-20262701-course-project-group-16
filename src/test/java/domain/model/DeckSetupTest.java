package domain.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckSetupTest {

    @Test
    void sixPlayersReceiveTwoUniqueCardsEach() {
        Deck deck = new Deck(new Random(7));
        deck.shuffle();
        List<List<Card>> hands = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            hands.add(new ArrayList<>());
        }
        for (int round = 0; round < 2; round++) {
            for (List<Card> hand : hands) {
                hand.addAll(deck.deal(1));
            }
        }

        Set<Card> seen = new HashSet<>();
        for (List<Card> hand : hands) {
            assertEquals(2, hand.size());
            for (Card card : hand) {
                assertTrue(seen.add(card), "A card was dealt to more than one hand");
            }
        }
        assertEquals(12, seen.size());
        assertEquals(40, deck.remainingCards());
        for (Card card : deck.deal(40)) {
            assertTrue(seen.add(card), "A dealt card remained in the deck");
        }
        assertEquals(52, seen.size());
        assertEquals(0, deck.remainingCards());
    }
}
