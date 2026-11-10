package bot;

import domain.model.Card;
import domain.model.Rank;
import domain.model.Suit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntUnaryOperator;

/** Uniform sampling without replacement; instances are confined to one worker. */
public final class UniformDealSampler implements DealSampler {
    private static final List<Card> CARDS = standardCards();
    private final IntUnaryOperator chooseIndex;

    public UniformDealSampler() { this(new Random()::nextInt); }

    public UniformDealSampler(IntUnaryOperator chooseIndex) {
        if (chooseIndex == null) {
            throw new IllegalArgumentException("A random index source is required.");
        }
        this.chooseIndex = chooseIndex;
    }

    private static List<Card> standardCards() {
        List<Card> cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        return List.copyOf(cards);
    }

    @Override
    public Map<String, List<Card>> sample(EquityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("An equity request is required.");
        }
        List<Card> unseen = new ArrayList<>(CARDS);
        unseen.removeAll(request.getHoleCards());
        unseen.removeAll(request.getBoard());
        List<Card> board = completeBoard(request.getBoard(), unseen);
        Map<String, List<Card>> holdings = new LinkedHashMap<>();
        holdings.put(request.getHeroId(), holding(request.getHoleCards(), board));
        for (String opponent : request.getOpponents()) {
            List<Card> holes = List.of(draw(unseen), draw(unseen));
            holdings.put(opponent, holding(holes, board));
        }
        return Map.copyOf(holdings);
    }

    private List<Card> completeBoard(List<Card> publicBoard, List<Card> unseen) {
        List<Card> board = new ArrayList<>(publicBoard);
        while (board.size() < 5) {
            board.add(draw(unseen));
        }
        return board;
    }

    private Card draw(List<Card> unseen) {
        int index = chooseIndex.applyAsInt(unseen.size());
        if (index < 0 || index >= unseen.size()) {
            throw new IllegalArgumentException("Random index is outside the unseen cards.");
        }
        return unseen.remove(index);
    }

    private static List<Card> holding(List<Card> holes, List<Card> board) {
        List<Card> holding = new ArrayList<>(holes);
        holding.addAll(board);
        return List.copyOf(holding);
    }
}
