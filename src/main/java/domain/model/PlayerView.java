package domain.model;

import domain.model.Card;

import java.util.List;

/** Public seat information plus hole cards only for the view owner's seat. */
public final class PlayerView {
    private final Player player;
    private final PlayerStatus status;
    private final long roundContribution;
    private final long handContribution;
    private final List<Card> holeCards;

    PlayerView(PlayerState state, boolean ownCards) {
        this.player = state.getPlayer();
        this.status = state.getStatus();
        this.roundContribution = state.getRoundContribution();
        this.handContribution = state.getHandContribution();
        this.holeCards = ownCards ? state.getHoleCards() : List.of();
    }

    public String getId() { return player.getId(); }
    public int getSeat() { return player.getSeat(); }
    public PlayerType getType() { return player.getType(); }
    public long getChipStack() { return player.getChipStack(); }
    public PlayerStatus getStatus() { return status; }
    public long getRoundContribution() { return roundContribution; }
    public long getHandContribution() { return handContribution; }
    public List<Card> getHoleCards() { return holeCards; }
}
