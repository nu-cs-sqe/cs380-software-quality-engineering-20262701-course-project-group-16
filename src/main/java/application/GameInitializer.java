package application;

import domain.GameConfig;
import domain.GamePhase;
import domain.GameSetup;
import domain.GameState;
import domain.PlayerState;
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

        List<List<Card>> hands = new ArrayList<>();
        int seatCount = table.getConfig().getSeatCount();
        for (int seat = 0; seat < seatCount; seat++) {
            hands.add(new ArrayList<>());
        }
        for (int round = 0; round < 2; round++) {
            for (int distance = 1; distance <= seatCount; distance++) {
                int seat = (table.getDealerSeat() + distance) % seatCount;
                hands.get(seat).addAll(deck.deal(1));
            }
        }

        List<PlayerState> dealtPlayers = new ArrayList<>();
        for (PlayerState player : table.getPlayerStates()) {
            dealtPlayers.add(new PlayerState(player.getPlayer(), player.getStatus(),
                    hands.get(player.getPlayer().getSeat()),
                    player.getRoundContribution(), player.getHandContribution()));
        }
        return new GameState(table.getConfig(), table.getHandId(), GamePhase.PREFLOP,
                dealtPlayers, table.getPositions(), table.getCurrentActorId().orElseThrow(),
                table.getCommunityCards(), deck.deal(deck.remainingCards()), table.getBettingProgress());
    }
}
