package application;

import domain.model.GameState;
import domain.model.Player;
import domain.model.PlayerType;
import java.util.Optional;

/** Owns a setup preview and controls explicit private-card handoffs. Use on the UI thread. */
public final class SetupSession {
    private final GameInitializer initializer;
    private Optional<GameState> state = Optional.empty();
    private int viewerSeat;
    private boolean cardsRevealed;

    public SetupSession() {
        this(new GameInitializer());
    }

    /** @throws IllegalArgumentException if initializer is null */
    public SetupSession(GameInitializer initializer) {
        if (initializer == null) {
            throw new IllegalArgumentException("A game initializer is required.");
        }
        this.initializer = initializer;
    }

    /**
     * Starts a new match with cards hidden. A rejected restart preserves the previous match.
     * @throws IllegalArgumentException unless humanCount is between one and six
     */
    public void start(int humanCount) {
        hideCards();
        state = Optional.of(initializer.start(humanCount));
        viewerSeat = 0; // Setup assigns at least one human, starting at seat zero.
    }

    /**
     * Selects a human viewer and conceals cards, even if selection is rejected.
     * @throws IllegalStateException if no match exists
     * @throws IllegalArgumentException if the ID does not identify a human in this match
     */
    public void selectViewer(String playerId) {
        hideCards();
        GameState match = requireMatch();
        Player viewer = match.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId) && player.getType() == PlayerType.HUMAN)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Select a human in this match."));
        viewerSeat = viewer.getSeat();
    }

    /** @throws IllegalStateException if no match exists */
    public void revealCards() {
        requireMatch();
        cardsRevealed = true;
    }

    /** Conceals private cards; also safe before the first match. */
    public void hideCards() {
        cardsRevealed = false;
    }

    /** Returns immutable display data; empty until a match has been started. */
    public Optional<SetupView> getView() {
        return state.map(match -> new SetupView(match, viewerSeat, cardsRevealed));
    }

    private GameState requireMatch() {
        return state.orElseThrow(() -> new IllegalStateException("Start a match first."));
    }
}
