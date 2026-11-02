package application;

import domain.model.Card;
import domain.model.HandPositions;
import domain.model.Player;
import java.util.List;

/** Immutable display data; no internal game-state reference is exposed. */
public final class SetupView {
    public List<Player> getPlayers() { return List.of(); }
    public HandPositions getPositions() { return null; }
    public int getCurrentActorSeat() { return 0; }
    public int getViewerSeat() { return 0; }
    public long getPot() { return 0; }
    public int getRemainingCardCount() { return 0; }
    public List<Card> getHoleCards() { return List.of(); }
}
