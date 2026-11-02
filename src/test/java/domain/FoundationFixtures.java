package domain;

import domain.model.Card;
import domain.model.Deck;
import java.util.*;

/** Test inputs describe explicit snapshots; expected values stay in individual tests. */
final class FoundationFixtures {
    static Card[] cards() { return new Deck().deal(52).toArray(new Card[0]); }
    static final class Fixture {
        GameConfig config = new GameConfig();
        String hand = "h1";
        GamePhase phase = GamePhase.SETUP;
        List<PlayerState> players = new ArrayList<>();
        HandPositions positions = new HandPositions(0, 1, 2);
        String actor = "p3";
        List<Card> community = new ArrayList<>();
        List<Card> deck = new ArrayList<>();
        BettingProgress betting = new BettingProgress(0, 20, allIds(), allIds());
        Fixture() {
            for (int seat = 0; seat < 6; seat++) {
                players.add(new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN, 1000),
                    PlayerStatus.ACTIVE, List.of(), 0, 0));
            }
        }
        void player(int seat, long chips, PlayerStatus status, long round, long hand) {
            List<Card> cards = players.get(seat).getHoleCards();
            players.set(seat, new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN, chips),
                status, status == PlayerStatus.ELIMINATED ? List.of() : cards, round, hand));
        }
        void deal() {
            for (int seat = 0; seat < 6; seat++) {
                PlayerState old = players.get(seat);
                if (old.getStatus() != PlayerStatus.ELIMINATED) {
                    players.set(seat, new PlayerState(old.getPlayer(), old.getStatus(),
                        List.of(FoundationFixtures.cards()[seat*2], FoundationFixtures.cards()[seat*2+1]), old.getRoundContribution(), old.getHandContribution()));
                }
            }
            deck = new ArrayList<>(Arrays.asList(FoundationFixtures.cards()).subList(12,52));
            phase = GamePhase.PREFLOP;
        }
        void board(GamePhase target, int size) {
            deal(); phase = target;
            community = new ArrayList<>(deck.subList(0, size));
            deck = new ArrayList<>(deck.subList(size, deck.size()));
            if (target == GamePhase.SHOWDOWN || target == GamePhase.COMPLETION) {
                actor = null; betting = new BettingProgress(0,20,Set.of(),Set.of());
            }
        }
        GameState build() {
            return new GameState(config, hand, phase, players, positions, actor, community, deck, betting);
        }
    }
    static Fixture fixture() { return new Fixture(); }
    static Set<String> allIds() { return Set.of("p0","p1","p2","p3","p4","p5"); }
}
