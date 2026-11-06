package application;

import domain.model.Card;
import domain.model.Deck;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.PlayerState;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Deals a new deck into an initialized table; never owns match setup or player counts. */
final class InitialDeal {
    private final Random random;

    InitialDeal(Random random) {
        this.random = random;
    }

    GameState deal(GameState table) {
        Deck deck = new Deck(random);
        deck.shuffle();
        List<List<Card>> hands = dealHoleCards(table, deck);
        List<PlayerState> players = playersWithCards(table, hands);
        return new GameState(table.getConfig(), table.getHandId(), GamePhase.PREFLOP,
                players, table.getPositions(), table.getCurrentActorId().orElseThrow(),
                table.getCommunityCards(), deck.deal(deck.remainingCards()),
                table.getBettingProgress());
    }

    private static List<List<Card>> dealHoleCards(GameState table, Deck deck) {
        int seatCount = table.getConfig().getSeatCount();
        List<List<Card>> hands = emptyHands(seatCount);
        for (int round = 0; round < 2; round++) {
            dealClockwise(table.getDealerSeat(), hands, deck);
        }
        return hands;
    }

    private static List<List<Card>> emptyHands(int seatCount) {
        List<List<Card>> hands = new ArrayList<>();
        for (int seat = 0; seat < seatCount; seat++) {
            hands.add(new ArrayList<>());
        }
        return hands;
    }

    private static void dealClockwise(int dealerSeat, List<List<Card>> hands, Deck deck) {
        for (int distance = 1; distance <= hands.size(); distance++) {
            int seat = (dealerSeat + distance) % hands.size();
            hands.get(seat).addAll(deck.deal(1));
        }
    }

    private static List<PlayerState> playersWithCards(GameState table, List<List<Card>> hands) {
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState before : table.getPlayerStates()) {
            players.add(new PlayerState(before.getPlayer(), before.getStatus(),
                    hands.get(before.getPlayer().getSeat()),
                    before.getRoundContribution(), before.getHandContribution()));
        }
        return players;
    }

}
