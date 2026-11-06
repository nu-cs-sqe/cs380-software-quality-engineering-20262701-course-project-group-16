package application;

import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameSetup;
import domain.model.GameState;
import domain.model.PlayerState;
import domain.model.Card;
import domain.model.Deck;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Combines table setup and dealing into the first preflop state of a new match. */
public final class GameInitializer {
    private final GameSetup setup;
    private final Random deckRandom;

    /** Uses the default match settings and normal random sources. */
    public GameInitializer() {
        this(new GameConfig(), new Random(), new Random());
    }

    /**
     * Uses separate random sources for dealer selection and deck shuffling.
     * @throws IllegalArgumentException if any dependency is null
     */
    public GameInitializer(GameConfig config, Random dealerRandom, Random deckRandom) {
        if (config == null || dealerRandom == null || deckRandom == null) {
            throw new IllegalArgumentException("Settings and both random sources are required.");
        }
        this.setup = new GameSetup(config, dealerRandom);
        this.deckRandom = deckRandom;
    }

    /**
     * Starts a new six-seat match, dealing two rounds clockwise after the dealer.
     * Returns a PREFLOP state with two cards per player and 40 cards remaining.
     * Each call starts a separate match; it does not resume or advance an existing hand.
     * @throws IllegalArgumentException unless humanCount is between one and six
     */
    public GameState start(int humanCount) {
        GameState table = setup.create(humanCount);
        Deck deck = new Deck(deckRandom);
        deck.shuffle();

        List<List<Card>> hands = dealHoleCards(table, deck);
        List<PlayerState> dealtPlayers = playersWithCards(table, hands);
        return new GameState(table.getConfig(), table.getHandId(), GamePhase.PREFLOP,
                dealtPlayers, table.getPositions(), table.getCurrentActorId().orElseThrow(),
                table.getCommunityCards(), deck.deal(deck.remainingCards()), table.getBettingProgress());
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
