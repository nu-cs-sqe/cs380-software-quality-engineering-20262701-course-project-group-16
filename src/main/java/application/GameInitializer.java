package application;

import domain.GameConfig;
import domain.GameSetup;
import domain.GameState;
import java.util.Random;

/** Starts a new match; dealing is the next TDD implementation step. */
public final class GameInitializer {
    private final GameConfig config;
    private final Random dealerRandom;
    private final Random deckRandom;

    public GameInitializer() {
        this(new GameConfig(), new Random(), new Random());
    }

    public GameInitializer(GameConfig config, Random dealerRandom, Random deckRandom) {
        this.config = config;
        this.dealerRandom = dealerRandom;
        this.deckRandom = deckRandom;
    }

    public GameState start(int humanCount) {
        return new GameSetup(config, dealerRandom).create(humanCount);
    }
}
