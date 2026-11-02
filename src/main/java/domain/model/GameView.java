package domain.model;

import domain.model.Card;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Player-specific read model; holds neither the internal GameState nor the remaining deck. */
public final class GameView {
    private final String viewerId;
    private final GameConfig config;
    private final String handId;
    private final GamePhase phase;
    private final HandPositions positions;
    private final Optional<String> actor;
    private final List<PlayerView> players;
    private final List<Card> community;
    private final long pot;
    private final BettingProgress betting;

    GameView(GameState state, String viewer) {
        this.viewerId = viewer;
        this.config = state.getConfig();
        this.handId = state.getHandId();
        this.phase = state.getPhase();
        this.positions = state.getPositions();
        this.actor = state.getCurrentActorId();
        this.players = state.getPlayerStates().stream()
                .map(player -> new PlayerView(player, player.getPlayer().getId().equals(viewer)))
                .collect(Collectors.toUnmodifiableList());
        this.community = state.getCommunityCards();
        this.pot = state.getPot();
        this.betting = state.getBettingProgress();
    }

    public String getViewerId() { return viewerId; }
    public GameConfig getConfig() { return config; }
    public String getHandId() { return handId; }
    public GamePhase getPhase() { return phase; }
    public HandPositions getPositions() { return positions; }
    public Optional<String> getCurrentActorId() { return actor; }
    public List<PlayerView> getPlayers() { return players; }
    public List<Card> getCommunityCards() { return community; }
    public long getPot() { return pot; }
    public long getCurrentBet() { return betting.getCurrentBet(); }
    public long getLastFullRaiseSize() { return betting.getLastFullRaiseSize(); }
    public Set<String> getPlayersToAct() { return betting.getPlayersToAct(); }
    public Set<String> getPlayersAllowedToRaise() { return betting.getPlayersAllowedToRaise(); }
}
