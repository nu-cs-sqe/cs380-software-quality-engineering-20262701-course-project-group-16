package domain.model;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

/** Exact river guarantees derived from public cards and the acting player's own holding. */
public final class RiverOutcomeEvaluator {
    private static final int FIRST_HOLE_INDEX = PokerHandEvaluator.HAND_SIZE;
    private static final int SECOND_HOLE_INDEX = FIRST_HOLE_INDEX + 1;

    public enum Outcome {
        BOARD_LOCKED_TIE, GUARANTEED_WIN, GUARANTEED_TIE, GUARANTEED_NON_LOSS, GUARANTEED_LOSS,
        UNCERTAIN
    }

    public Outcome evaluate(List<Card> board, List<Card> holeCards, BooleanSupplier cancelled) {
        List<Card> own = requireRiverHolding(board, holeCards);
        List<Card> unseen = Deck.standardCards();
        unseen.removeAll(board);
        int boardRank = PokerHandEvaluator.bestFive(board);
        if (candidateRanks(board, unseen, cancelled).noneMatch(rank -> rank > boardRank)) {
            return Outcome.BOARD_LOCKED_TIE;
        }

        unseen.removeAll(holeCards);
        IntSummaryStatistics possible = candidateRanks(board, unseen, cancelled)
                .summaryStatistics();
        return classify(PokerHandEvaluator.bestFive(own), possible);
    }

    private static List<Card> requireRiverHolding(List<Card> board, List<Card> holeCards) {
        if (board.size() != PokerHandEvaluator.HAND_SIZE) {
            throw new IllegalArgumentException("A river outcome requires a complete board.");
        }
        List<Card> own = new ArrayList<>(board);
        own.addAll(holeCards);
        PokerHandEvaluator.validate(own);
        return own;
    }

    static Outcome classify(int ownRank, IntSummaryStatistics opponents) {
        if (ownRank > opponents.getMax()) {
            return Outcome.GUARANTEED_WIN;
        }
        if (ownRank < opponents.getMin()) {
            return Outcome.GUARANTEED_LOSS;
        }
        if (ownRank == opponents.getMax()) {
            return opponents.getMin() == ownRank ? Outcome.GUARANTEED_TIE
                    : Outcome.GUARANTEED_NON_LOSS;
        }
        return Outcome.UNCERTAIN;
    }

    static IntStream candidateRanks(List<Card> board, List<Card> available,
            BooleanSupplier cancelled) {
        return IntStream.range(0, available.size() - 1)
                .flatMap(first -> ranksWithFirstCard(board, available, first, cancelled));
    }

    private static IntStream ranksWithFirstCard(List<Card> board, List<Card> available, int first,
            BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("River outcome calculation was cancelled.");
        }
        List<Card> holding = new ArrayList<>(board);
        holding.add(available.get(first));
        holding.add(available.get(first + 1));
        return IntStream.range(first + 1, available.size()).map(second -> {
            holding.set(SECOND_HOLE_INDEX, available.get(second));
            return PokerHandEvaluator.bestFive(holding);
        });
    }
}
