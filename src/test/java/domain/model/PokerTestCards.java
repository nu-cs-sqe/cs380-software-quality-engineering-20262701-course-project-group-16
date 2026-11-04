package domain.model;

import java.util.ArrayList;
import java.util.List;

/** Small literal-card fixture reader; expected poker results stay in each test. */
final class PokerTestCards {
    private PokerTestCards() { }

    static List<Card> cards(String text) {
        List<Card> result = new ArrayList<>();
        String ranks = "23456789TJQKA";
        String suits = "CDHS";
        for (String token : text.split(" ")) {
            result.add(new Card(Rank.values()[ranks.indexOf(token.charAt(0))],
                    Suit.values()[suits.indexOf(token.charAt(1))]));
        }
        return result;
    }
}
