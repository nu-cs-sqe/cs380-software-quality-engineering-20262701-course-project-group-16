package application;

import domain.model.Card;
import domain.model.GameState;
import domain.model.HandPositions;
import domain.model.Player;
import domain.model.GamePhase;
import domain.model.PlayerStatus;
import java.util.List;
import java.util.stream.Collectors;

/** Immutable display snapshot with public players and only explicitly revealed human cards. */
public final class SetupView {
    private final List<Player> players;
    private final HandPositions positions;
    private final int currentActorSeat;
    private final int viewerSeat;
    private final long pot;
    private final int remainingCardCount;
    private final List<Card> holeCards;
    private final String handId;
    private final GamePhase phase;
    private final List<Card> communityCards;
    private final List<PlayerStatus> statuses;
    private final List<Long> roundContributions;
    private final List<Long> handContributions;
    private final long currentBet;

    SetupView(GameState state, int viewerSeat, boolean revealed) {
        this.players = List.copyOf(state.getPlayers());
        this.positions = state.getPositions();
        this.currentActorSeat = state.getCurrentActorSeat().orElse(-1);
        this.viewerSeat = viewerSeat;
        this.pot = state.getPot();
        this.remainingCardCount = state.getRemainingDeck().size();
        this.handId = state.getHandId();
        this.phase = state.getPhase();
        this.communityCards = List.copyOf(state.getCommunityCards());
        this.statuses = List.copyOf(state.getPlayerStates().stream()
                .map(player -> player.getStatus())
                .collect(Collectors.toList()));
        this.roundContributions = state.getPlayerStates().stream()
                .map(player -> player.getRoundContribution())
                .collect(Collectors.toUnmodifiableList());
        this.handContributions = state.getPlayerStates().stream()
                .map(player -> player.getHandContribution())
                .collect(Collectors.toUnmodifiableList());
        this.currentBet = state.getCurrentBet();
        this.holeCards = revealed
                ? List.copyOf(state.viewFor(players.get(viewerSeat).getId())
                        .getPlayers().get(viewerSeat).getHoleCards())
                : List.of();
    }

    public List<Player> getPlayers() { return players; }
    public HandPositions getPositions() { return positions; }
    /** Current actor's stable seat, or -1 when there is no pending decision. */
    public int getCurrentActorSeat() { return currentActorSeat; }
    public int getViewerSeat() { return viewerSeat; }
    public long getPot() { return pot; }
    public int getRemainingCardCount() { return remainingCardCount; }
    public List<Card> getHoleCards() { return holeCards; }
    public String getHandId() { return handId; }
    public GamePhase getPhase() { return phase; }
    public List<Card> getCommunityCards() { return communityCards; }
    public List<PlayerStatus> getStatuses() { return statuses; }
    public long getRoundContribution(int seat) { return roundContributions.get(seat); }
    public long getHandContribution(int seat) { return handContributions.get(seat); }
    public long getCurrentBet() { return currentBet; }
}
