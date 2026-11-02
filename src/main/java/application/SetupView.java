package application;

import domain.model.Card;
import domain.model.GameState;
import domain.model.HandPositions;
import domain.model.Player;
import java.util.List;

/** Immutable display snapshot with public players and only explicitly revealed human cards. */
public final class SetupView {
    private final List<Player> players;
    private final HandPositions positions;
    private final int currentActorSeat;
    private final int viewerSeat;
    private final long pot;
    private final int remainingCardCount;
    private final List<Card> holeCards;

    SetupView(GameState state, int viewerSeat, boolean revealed) {
        this.players = List.copyOf(state.getPlayers());
        this.positions = state.getPositions();
        this.currentActorSeat = state.getCurrentActorSeat().orElseThrow();
        this.viewerSeat = viewerSeat;
        this.pot = state.getPot();
        this.remainingCardCount = state.getRemainingDeck().size();
        this.holeCards = revealed
                ? List.copyOf(state.viewFor(players.get(viewerSeat).getId())
                        .getPlayers().get(viewerSeat).getHoleCards())
                : List.of();
    }

    public List<Player> getPlayers() { return players; }
    public HandPositions getPositions() { return positions; }
    public int getCurrentActorSeat() { return currentActorSeat; }
    public int getViewerSeat() { return viewerSeat; }
    public long getPot() { return pot; }
    public int getRemainingCardCount() { return remainingCardCount; }
    public List<Card> getHoleCards() { return holeCards; }
}
