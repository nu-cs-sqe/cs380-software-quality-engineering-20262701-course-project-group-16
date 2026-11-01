package domain.model;

import java.util.List;

/**
 * Contract for the Week 6 best-five-of-seven evaluator; no implementation yet.
 * Each argument is one player's two hole cards plus the five community cards.
 * Shared community cards may occur in both arguments, but neither argument may
 * contain a duplicate card. Inputs must not be modified by an implementation.
 */
@FunctionalInterface
public interface HandEvaluator {

    /**
     * Compares two seven-card holdings using standard high-hand Hold'em rules.
     *
     * @return a positive value if firstCards is stronger, zero for a tie,
     *         or a negative value if secondCards is stronger
     * @throws NullPointerException if a list or one of its cards is null
     * @throws IllegalArgumentException if either list has a size other than
     *         seven or contains duplicate card values
     */
    int compare(List<Card> firstCards, List<Card> secondCards);
}
