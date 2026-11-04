package domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Standard high-hand Hold'em comparison using the best five of seven cards.
 * The 21 five-card combinations are ranked without modifying either input.
 */
public final class PokerHandEvaluator implements HandEvaluator {

    @Override
    public int compare(List<Card> firstCards, List<Card> secondCards) {
        validate(firstCards);
        validate(secondCards);
        return Integer.compare(bestFive(firstCards), bestFive(secondCards));
    }

    private static int bestFive(List<Card> cards) {
        int best = 0;
        for (int a = 0; a < 3; a++) {
            for (int b = a + 1; b < 4; b++) {
                for (int c = b + 1; c < 5; c++) {
                    for (int d = c + 1; d < 6; d++) {
                        for (int e = d + 1; e < 7; e++) {
                            best = Math.max(best, rankFive(cards.get(a), cards.get(b),
                                    cards.get(c), cards.get(d), cards.get(e)));
                        }
                    }
                }
            }
        }
        return best;
    }

    private static int rankFive(Card... cards) {
        int[] counts = new int[15];
        boolean flush = true;
        for (Card card : cards) {
            counts[card.getRank().ordinal() + 2]++;
            flush &= card.getSuit() == cards[0].getSuit();
        }
        List<Integer> groups = new ArrayList<>();
        for (int rank = 14; rank >= 2; rank--) {
            if (counts[rank] > 0) {
                groups.add(rank);
            }
        }
        groups.sort((left, right) -> counts[left] == counts[right]
                ? Integer.compare(right, left) : Integer.compare(counts[right], counts[left]));
        int straight = 0;
        if (groups.size() == 5) {
            if (groups.get(0) - groups.get(4) == 4) {
                straight = groups.get(0);
            } else if (groups.equals(List.of(14, 5, 4, 3, 2))) {
                straight = 5;
            }
        }
        int first = groups.get(0);
        if (flush && straight > 0) {
            return score(8, straight);
        }
        if (counts[first] == 4) {
            return score(7, first, groups.get(1));
        }
        if (counts[first] == 3 && counts[groups.get(1)] == 2) {
            return score(6, first, groups.get(1));
        }
        if (flush) {
            return score(5, groups.stream().mapToInt(Integer::intValue).toArray());
        }
        if (straight > 0) {
            return score(4, straight);
        }
        if (counts[first] == 3) {
            return score(3, first, groups.get(1), groups.get(2));
        }
        if (counts[first] == 2 && counts[groups.get(1)] == 2) {
            return score(2, first, groups.get(1), groups.get(2));
        }
        if (counts[first] == 2) {
            return score(1, first, groups.get(1), groups.get(2), groups.get(3));
        }
        return score(0, groups.stream().mapToInt(Integer::intValue).toArray());
    }

    private static int score(int category, int... kickers) {
        int result = category;
        for (int position = 0; position < 5; position++) {
            result = result * 15 + (position < kickers.length ? kickers[position] : 0);
        }
        return result;
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
