package application;

import java.util.Optional;

/** Controls new-match setup and private-card reveal state. */
public final class SetupSession {
    public SetupSession() { this(new GameInitializer()); }
    public SetupSession(GameInitializer initializer) { }
    public void start(int humanCount) { }
    public void selectViewer(String playerId) { }
    public void revealCards() { }
    public void hideCards() { }
    public Optional<SetupView> getView() { return Optional.empty(); }
}
