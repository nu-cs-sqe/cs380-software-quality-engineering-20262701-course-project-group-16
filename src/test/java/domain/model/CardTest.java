package domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @ParameterizedTest
    @CsvSource(value = {
        "NULL, SPADES",
        "ACE, NULL",
        "NULL, NULL"
    }, nullValues = "NULL")
    void cardRejectsNullRankOrSuit(Rank rank, Suit suit) {
        assertThrows(NullPointerException.class, () -> new Card(rank, suit));
    }

    @ParameterizedTest
    @MethodSource("equalityCases")
    void cardEqualityUsesRankAndSuit(Card card, Object other, boolean expected) {
        assertEquals(expected, card.equals(other));
        if (other instanceof Card) {
            assertEquals(expected, other.equals(card));
        }
    }

    static Stream<Arguments> equalityCases() {
        Card aceOfSpades = new Card(Rank.ACE, Suit.SPADES);
        return Stream.of(
            Arguments.of(aceOfSpades, aceOfSpades, true),
            Arguments.of(aceOfSpades, new Card(Rank.ACE, Suit.SPADES), true),
            Arguments.of(aceOfSpades, new Card(Rank.KING, Suit.SPADES), false),
            Arguments.of(aceOfSpades, new Card(Rank.ACE, Suit.HEARTS), false),
            Arguments.of(aceOfSpades, new Card(Rank.KING, Suit.HEARTS), false),
            Arguments.of(aceOfSpades, null, false),
            Arguments.of(aceOfSpades, "ACE of SPADES", false)
        );
    }

    @Test
    void equalCardsHaveSameHashAndDeduplicateInASet() {
        Card first = new Card(Rank.ACE, Suit.SPADES);
        Card sameValue = new Card(Rank.ACE, Suit.SPADES);
        Card differentValue = new Card(Rank.ACE, Suit.HEARTS);
        Set<Card> cards = new HashSet<>(Arrays.asList(first, sameValue, differentValue));

        assertAll(
            () -> assertEquals(first.hashCode(), sameValue.hashCode()),
            () -> assertEquals(2, cards.size())
        );
    }
}
