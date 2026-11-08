package application;

import domain.model.AcceptedAction;
import domain.model.GameState;
import domain.model.PlayStats;
import domain.model.PlayerState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Adapts accepted engine observations to pending per-player statistics. */
final class HandStatistics {
    private final String handId;
    private final Map<String, PlayStats> players;

    // The registry is copied; its collectors are intentionally shared with their owner.
    HandStatistics(String handId, Map<String, PlayStats> players) {
        this.handId = handId;
        this.players = Map.copyOf(players);
    }

    static Consumer<AcceptedAction> start(GameState state) {
        return new HandStatistics(state.getHandId(), createCollectors(state))::record;
    }

    private static Map<String, PlayStats> createCollectors(GameState state) {
        Map<String, PlayStats> collectors = new LinkedHashMap<>();
        for (PlayerState player : state.getPlayerStates()) {
            PlayStats stats = new PlayStats();
            stats.beginHand(state.getHandId(), !player.getHoleCards().isEmpty());
            collectors.put(player.getPlayer().getId(), stats);
        }
        return collectors;
    }

    void record(AcceptedAction action) {
        if (!handId.equals(action.getHandId())) {
            throw new IllegalArgumentException("The observation belongs to another hand.");
        }
        PlayStats collector = players.get(action.getPlayerId());
        if (collector == null) {
            throw new IllegalArgumentException("The observation requires a player in this hand.");
        }
        collector.recordAction(action.toStatsAction());
    }
}
