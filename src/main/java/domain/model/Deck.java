package domain.model;

import java.util.ArrayList;
import java.util.List;

/** A standard deck that owns the cards not yet dealt. */
public final class Deck {

    private final List<Card> cards = new ArrayList<>();

    /** Creates an unshuffled deck containing each rank/suit pair once. */
    public Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
    }

    public int remainingCards() {
        return cards.size();
    }

    /** Removes the next cards and returns an immutable independent hand. */
    public List<Card> deal(int count) {
        List<Card> hand = List.copyOf(cards.subList(0, count));
        cards.subList(0, count).clear();
        return hand;
    }
}
