package bot;

import domain.model.*;
import java.util.*;

final class EquityFixtures {
    static final List<Card> HOLE = List.of(new Card(Rank.TEN, Suit.SPADES), new Card(Rank.ACE, Suit.SPADES));
    static final List<Card> BOARD = List.of(new Card(Rank.TWO, Suit.CLUBS), new Card(Rank.THREE, Suit.CLUBS),
            new Card(Rank.FOUR, Suit.CLUBS), new Card(Rank.FIVE, Suit.CLUBS), new Card(Rank.SIX, Suit.CLUBS));
    static EquityRequest request(int simulations, Rank... ranks) {
        List<String> opponents = opponents(ranks.length);
        Set<String> all = new HashSet<>(opponents);
        all.add("hero");
        return new EquityRequest("hero", HOLE, BOARD, opponents,
                List.of(new EquityPot("main", 120, all)), simulations);
    }
    static List<String> opponents(int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) ids.add("p" + i);
        return ids;
    }
    static Map<String, List<Card>> deal(Rank... ranks) {
        Map<String, List<Card>> holdings = new LinkedHashMap<>();
        holdings.put("hero", holding(HOLE, BOARD));
        Suit[] suits = {Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS};
        for (int i = 0; i < ranks.length; i++) {
            holdings.put("p" + (i + 1), holding(List.of(new Card(ranks[i], suits[i]),
                    new Card(Rank.KING, suits[i])), BOARD));
        }
        return Map.copyOf(holdings);
    }
    static List<Card> holding(List<Card> holes, List<Card> board) {
        List<Card> result = new ArrayList<>(holes);
        result.addAll(board);
        return List.copyOf(result);
    }
}
