package domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Best-five-of-seven evaluator under development.
 * Input validation is implemented; valid holdings still return a placeholder tie.
 * Hand ranking and application integration are not implemented yet.
 */
public final class PokerHandEvaluator implements HandEvaluator {

    @Override
    public int compare(List<Card> firstCards, List<Card> secondCards) {
        validate(firstCards);
        validate(secondCards);
        return 0;
    }

    private static void validate(List<Card> cards) {
        Objects.requireNonNull(cards, "cards");
        if (cards.size() != 7) {
            throw new IllegalArgumentException("A holding must contain exactly seven cards.");
        }
        Set<Card> unique = new HashSet<>();
        for (Card card : cards) {
            Objects.requireNonNull(card, "card");
            if (!unique.add(card)) {
                throw new IllegalArgumentException("A holding must not contain duplicate cards.");
            }
        }
    }
}
