package domain.model;

import domain.model.RiverOutcomeEvaluator.Outcome;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Proves showdown guarantees from known cards with a bounded search on incomplete boards. */
public final class ExactOutcomeEvaluator {
    private static final int FLOP_CARDS = 3;
    private static final int HOLE_CARDS = 2;
    private static final int DEFAULT_COMPARISON_LIMIT = 2_048;
    private final int comparisonLimit;

    public ExactOutcomeEvaluator() {
        this(DEFAULT_COMPARISON_LIMIT);
    }

    public ExactOutcomeEvaluator(int comparisonLimit) {
        if (comparisonLimit < 0) {
            throw new IllegalArgumentException("The exact comparison limit must be nonnegative.");
        }
        this.comparisonLimit = comparisonLimit;
    }

    public Outcome evaluate(List<Card> board, List<Card> holes, BooleanSupplier cancelled) {
        if (board.size() == PokerHandEvaluator.HAND_SIZE) {
            return new RiverOutcomeEvaluator().evaluate(board, holes, cancelled);
        }
        requireRunning(cancelled);
        List<Card> holding = incompleteHolding(board, holes);
        if (hasProvenEarlyWin(board, holes, holding)) {
            return Outcome.GUARANTEED_WIN;
        }
        return searchIncompleteBoard(board, holes, holding, cancelled);
    }

    private Outcome searchIncompleteBoard(List<Card> board, List<Card> holes, List<Card> holding,
            BooleanSupplier cancelled) {
        List<Card> unseen = Deck.standardCards();
        unseen.removeAll(holding);
        return new RunoutSearch(holes, unseen, comparisonLimit, cancelled).prove(board);
    }

    private static List<Card> incompleteHolding(List<Card> board, List<Card> holes) {
        if (board.size() < FLOP_CARDS || board.size() > PokerHandEvaluator.HAND_SIZE
                || holes.size() != HOLE_CARDS) {
            throw new IllegalArgumentException("Use two hole cards and a flop, turn or river.");
        }
        List<Card> holding = holding(board, holes);
        PokerHandEvaluator.requireDistinct(holding);
        return holding;
    }

    private static boolean hasProvenEarlyWin(List<Card> board, List<Card> holes,
            List<Card> holding) {
        // A private royal occupies at least three public suit slots, excluding a rival royal.
        return PokerHandEvaluator.bestFive(holding) == PokerHandEvaluator.royalFlushScore()
                || privateAceQuadsCannotBeBeaten(board, holes, holding);
    }

    // Aces are the highest quads, and a private ace makes an equal set impossible.
    private static boolean privateAceQuadsCannotBeBeaten(List<Card> board, List<Card> holes,
            List<Card> holding) {
        return holes.stream().anyMatch(card -> card.getRank() == Rank.ACE)
                && holding.stream().filter(card -> card.getRank() == Rank.ACE).count()
                        == Suit.values().length
                && straightFlushCannotFit(board);
    }

    private static boolean straightFlushCannotFit(List<Card> board) {
        int future = PokerHandEvaluator.HAND_SIZE - board.size();
        for (Suit suit : Suit.values()) {
            long suited = board.stream().filter(card -> card.getSuit() == suit).count();
            if (suited + future + HOLE_CARDS >= PokerHandEvaluator.HAND_SIZE) {
                return false;
            }
        }
        return true;
    }

    private static List<Card> holding(List<Card> board, List<Card> holes) {
        List<Card> holding = new ArrayList<>(board);
        holding.addAll(holes);
        return holding;
    }

    private static void requireRunning(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Exact outcome calculation was cancelled.");
        }
    }

    private static final class RunoutSearch {
        private final List<Card> holes;
        private final List<Card> unseen;
        private final BooleanSupplier cancelled;
        private final IntSummaryStatistics bounds = new IntSummaryStatistics();
        private int remaining;

        private RunoutSearch(List<Card> holes, List<Card> unseen, int limit,
                BooleanSupplier cancelled) {
            this.holes = holes;
            this.unseen = unseen;
            this.cancelled = cancelled;
            remaining = limit;
        }

        private Outcome prove(List<Card> board) {
            return visitRunouts(new ArrayList<>(board), 0)
                    ? RiverOutcomeEvaluator.classify(0, bounds) : Outcome.UNCERTAIN;
        }

        /** Returns true only when every comparison was completed. */
        private boolean visitRunouts(List<Card> board, int next) {
            requireRunning(cancelled);
            int missing = PokerHandEvaluator.HAND_SIZE - board.size();
            if (missing == 0) {
                return visitHoldings(board);
            }
            for (int index = next; index <= unseen.size() - missing; index++) {
                board.add(unseen.get(index));
                boolean complete = visitRunouts(board, index + 1);
                board.remove(board.size() - 1);
                if (!complete) {
                    return false;
                }
            }
            return true;
        }

        private boolean visitHoldings(List<Card> board) {
            List<Card> available = new ArrayList<>(unseen);
            available.removeAll(board);
            int cards = available.size();
            int pairCount = cards * (cards - 1) / 2;
            int comparisons = Math.min(pairCount, remaining);
            int ownRank = PokerHandEvaluator.bestFive(holding(board, holes));
            boolean mixed = RiverOutcomeEvaluator.candidateRanks(board, available, cancelled)
                    .limit(comparisons).anyMatch(rank -> observe(Integer.compare(rank, ownRank)));
            return !mixed && comparisons == pairCount;
        }

        private boolean observe(int opponentVersusHero) {
            remaining--;
            bounds.accept(opponentVersusHero);
            return bounds.getMax() > 0 && bounds.getMin() <= 0;
        }
    }
}
