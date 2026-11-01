package domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class DeckTest {

    @Test
    void newDeckHas52Cards() {
        Deck deck = new Deck();

        assertEquals(52, deck.remainingCards());
    }
}