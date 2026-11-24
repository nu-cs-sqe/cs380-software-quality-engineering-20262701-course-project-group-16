package domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Standard high-hand Hold'em comparison using the best five of seven cards.
 * The 21 five-card combinations are ranked without modifying either input.
 */
public final class PokerHandEvaluator implements HandEvaluator {
    public static final int HAND_SIZE = 5;
    private static final int COMPLETE_HOLDING_SIZE = 7;
    private static final List<String> CATEGORIES = List.of("HIGH_CARD", "ONE_PAIR", "TWO_PAIR",
            "THREE_OF_A_KIND", "STRAIGHT", "FLUSH", "FULL_HOUSE", "FOUR_OF_A_KIND",
            "STRAIGHT_FLUSH");
    private static final int STRAIGHT_FLUSH = 8;
    private static final int FOUR_OF_A_KIND = 7;
    private static final int FULL_HOUSE = 6;
    private static final int FLUSH = 5;
    private static final int STRAIGHT = 4;
    private static final int THREE_OF_A_KIND = 3;
    private static final int TWO_PAIR = 2;
    private static final int ONE_PAIR = 1;
    private static final int HIGH_CARD = 0;
    private static final int RANK_RADIX = 15;

    @Override
    public int compare(List<Card> firstCards, List<Card> secondCards) {
        validate(firstCards);
        validate(secondCards);
        return Integer.compare(bestFive(firstCards), bestFive(secondCards));
    }

    /** Classifies a shown holding on a flop, turn or river using the same five-card score. */
    public String category(List<Card> cards) {
        validateShown(cards);
        int score = bestFive(cards);
        for (int digit = 0; digit < HAND_SIZE; digit++) {
            score /= RANK_RADIX;
        }
        return CATEGORIES.get(score);
    }

    /** Returns one actual best five. Input order breaks equivalent subset ties. */
    public List<Card> winningCards(List<Card> cards) {
        validateShown(cards);
        int highest = bestFive(cards);
        List<Card> best = new ArrayList<>(cards);
        for (int index = best.size() - 1; index >= 0 && best.size() > HAND_SIZE; index--) {
            Card removed = best.remove(index);
            if (bestFive(best) != highest) {
                best.add(index, removed);
            }
        }
        return List.copyOf(best);
    }

    private static void validateShown(List<Card> cards) {
        if (cards.size() < HAND_SIZE || cards.size() > COMPLETE_HOLDING_SIZE) {
            throw new IllegalArgumentException("Use five to seven cards.");
        }
        requireDistinct(cards);
    }

    private static int bestFive(List<Card> cards) {
        int count = cards.size();
        int best = 0;
        // Increasing indices visit each five-card subset exactly once.
        for (int firstIndex = 0; firstIndex < count - 4; firstIndex++) {
            for (int secondIndex = firstIndex + 1; secondIndex < count - 3; secondIndex++) {
                for (int thirdIndex = secondIndex + 1; thirdIndex < count - 2; thirdIndex++) {
                    for (int fourthIndex = thirdIndex + 1; fourthIndex < count - 1;
                            fourthIndex++) {
                        for (int fifthIndex = fourthIndex + 1; fifthIndex < count; fifthIndex++) {
                            best = Math.max(best, rankFive(cards.get(firstIndex),
                                    cards.get(secondIndex), cards.get(thirdIndex),
                                    cards.get(fourthIndex), cards.get(fifthIndex)));
                        }
                    }
                }
            }
        }
        return best;
    }

    private static int rankFive(Card... cards) {
        int[] counts = countRanks(cards);
        boolean flush = isFlush(cards);
        List<Integer> groups = ranksByMultiplicity(counts);
        int straight = straightHighCard(groups);
        int first = groups.get(0);
        if (flush && straight > 0) {
            return score(STRAIGHT_FLUSH, straight);
        }
        if (counts[first] == 4) {
            return score(FOUR_OF_A_KIND, first, groups.get(1));
        }
        if (counts[first] == 3 && counts[groups.get(1)] == 2) {
            return score(FULL_HOUSE, first, groups.get(1));
        }
        if (flush) {
            return score(FLUSH, groups.stream().mapToInt(Integer::intValue).toArray());
        }
        if (straight > 0) {
            return score(STRAIGHT, straight);
        }
        if (counts[first] == 3) {
            return score(THREE_OF_A_KIND, first, groups.get(1), groups.get(2));
        }
        if (counts[first] == 2 && counts[groups.get(1)] == 2) {
            return score(TWO_PAIR, first, groups.get(1), groups.get(2));
        }
        if (counts[first] == 2) {
            return score(ONE_PAIR, first, groups.get(1), groups.get(2), groups.get(3));
        }
        return score(HIGH_CARD, groups.stream().mapToInt(Integer::intValue).toArray());
    }

    private static int[] countRanks(Card[] cards) {
        int[] counts = new int[RANK_RADIX];
        for (Card card : cards) {
            counts[card.getRank().ordinal() + 2]++;
        }
        return counts;
    }

    private static boolean isFlush(Card[] cards) {
        boolean flush = true;
        for (Card card : cards) {
            flush &= card.getSuit() == cards[0].getSuit();
        }
        return flush;
    }

    private static List<Integer> ranksByMultiplicity(int[] counts) {
        List<Integer> groups = new ArrayList<>();
        for (int rank = 14; rank >= 2; rank--) {
            if (counts[rank] > 0) {
                groups.add(rank);
            }
        }
        groups.sort((left, right) -> counts[left] == counts[right]
                ? Integer.compare(right, left) : Integer.compare(counts[right], counts[left]));
        return groups;
    }

    private static int straightHighCard(List<Integer> groups) {
        if (groups.size() != 5) {
            return 0;
        }
        if (groups.get(0) - groups.get(4) == 4) {
            return groups.get(0);
        }
        return groups.equals(List.of(14, 5, 4, 3, 2)) ? 5 : 0;
    }

    private static int score(int category, int... kickers) {
        // Base 15 preserves the category and lexicographic kicker order.
        int result = category;
        for (int position = 0; position < HAND_SIZE; position++) {
            result = result * RANK_RADIX + (position < kickers.length ? kickers[position] : 0);
        }
        return result;
    }

    private static void validate(List<Card> cards) {
        if (cards.size() != COMPLETE_HOLDING_SIZE) {
            throw new IllegalArgumentException("A holding must contain exactly seven cards.");
        }
        requireDistinct(cards);
    }

    private static void requireDistinct(List<Card> cards) {
        Set<Card> unique = new HashSet<>();
        for (Card card : cards) {
            if (!unique.add(card)) {
                throw new IllegalArgumentException("A holding must not contain duplicate cards.");
            }
        }
    }
}
