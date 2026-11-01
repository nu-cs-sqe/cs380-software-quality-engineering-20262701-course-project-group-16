package domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** A standard deck that owns the cards not yet dealt. */
public final class Deck {

    private final List<Card> cards = new ArrayList<>();
    private final Random random;

    /** Creates an unshuffled deck with its own random source. */
    public Deck() {
        this(new Random());
    }

    /**
     * Creates an unshuffled deck with a source used by later shuffle calls.
     *
     * @throws NullPointerException if random is null
     */
    public Deck(Random random) {
        this.random = Objects.requireNonNull(random, "random");
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
    }

    /** Reorders only the cards still in this deck. */
    public void shuffle() {
        Collections.shuffle(cards, random);
    }

    public int remainingCards() {
        return cards.size();
    }

    /**
     * Removes the next cards and returns an immutable independent hand.
     *
     * @throws IllegalArgumentException if count is negative or exceeds the remaining size
     */
    public List<Card> deal(int count) {
        if (count < 0 || count > cards.size()) {
            throw new IllegalArgumentException(
                "Cannot deal " + count + " cards from " + cards.size() + " remaining");
        }
        List<Card> hand = List.copyOf(cards.subList(0, count));
        cards.subList(0, count).clear();
        return hand;
    }
}
