package application;

import domain.model.GameConfig;
import domain.model.GameSetup;
import domain.model.GameState;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

/** Combines table setup and dealing into the first preflop state of a new match. */
public final class GameInitializer {
    private final IntFunction<GameState> createTable;
    private final UnaryOperator<GameState> dealCards;

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
        this.createTable = new GameSetup(config, dealerRandom)::create;
        this.dealCards = new InitialDeal(deckRandom)::deal;
    }

    // Package composition boundary: setup owns validation; dealing owns cards and snapshots.
    GameInitializer(IntFunction<GameState> createTable, UnaryOperator<GameState> dealCards) {
        this.createTable = Objects.requireNonNull(createTable, "createTable");
        this.dealCards = Objects.requireNonNull(dealCards, "dealCards");
    }

    /**
     * Starts a new six-seat match, dealing two rounds clockwise after the dealer.
     * Returns a PREFLOP state with two cards per player and 40 cards remaining.
     * Each call starts a separate match; it does not resume or advance an existing hand.
     * @throws IllegalArgumentException unless humanCount is between one and six
     */
    public GameState start(int humanCount) {
        GameState table = createTable.apply(humanCount);
        return dealCards.apply(table);
    }
}
