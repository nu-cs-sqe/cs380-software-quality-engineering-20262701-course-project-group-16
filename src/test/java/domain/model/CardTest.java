package domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CardTest {

    @ParameterizedTest
    @CsvSource({
        "TWO, CLUBS",
        "THREE, DIAMONDS",
        "KING, HEARTS",
        "ACE, SPADES"
    })
    void cardRetainsRankAndSuit(Rank rank, Suit suit) {
        Card card = new Card(rank, suit);

        assertAll(
            () -> assertEquals(rank, card.getRank()),
            () -> assertEquals(suit, card.getSuit())
        );
    }
}
