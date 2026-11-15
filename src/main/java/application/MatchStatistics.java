package application;

import domain.model.AcceptedAction;
import domain.model.CompletedStatistics;
import domain.model.BettingStreet;
import domain.model.GameState;
import domain.model.PlayStats;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.ResolvedActionType;
import domain.model.Settlement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Completed match totals. Each update prepares independent collectors before publication. */
final class MatchStatistics {
    private final Map<String, PlayStats> players;

    private MatchStatistics(Map<String, PlayStats> players) {
        this.players = Map.copyOf(players);
    }

    static MatchStatistics start(GameState state) {
        Map<String, PlayStats> players = new LinkedHashMap<>();
        for (Player player : state.getPlayers()) {
            players.put(player.getId(), new PlayStats());
        }
        return new MatchStatistics(players);
    }

    MatchStatistics complete(GameState hand, Settlement paid, List<AcceptedAction> actions) {
        Map<String, PlayStats> next = prepareCollectors(hand);
        HandStatistics recorder = new HandStatistics(hand.getHandId(), next);
        for (AcceptedAction action : actions) {
            recorder.record(action);
        }
        completePlayers(hand, paid, actions, next);
        return new MatchStatistics(next);
    }

    private Map<String, PlayStats> prepareCollectors(GameState hand) {
        Map<String, PlayStats> next = new LinkedHashMap<>();
        for (PlayerState player : hand.getPlayerStates()) {
            String id = player.getPlayer().getId();
            PlayStats copy = players.get(id).copy();
            copy.beginHand(hand.getHandId(), !player.getHoleCards().isEmpty());
            next.put(id, copy);
        }
        return next;
    }

    private static void completePlayers(GameState hand, Settlement paid,
            List<AcceptedAction> actions, Map<String, PlayStats> next) {
        Set<String> preflopFolders = actions.stream()
                .filter(action -> action.getStreet() == BettingStreet.PREFLOP
                        && action.getType() == ResolvedActionType.FOLD)
                .map(AcceptedAction::getPlayerId).collect(Collectors.toSet());
        boolean contested = hand.getPlayerStates().stream()
                .filter(PlayerState::isParticipating).count() > 1;
        for (PlayerState player : hand.getPlayerStates()) {
            String id = player.getPlayer().getId();
            boolean sawFlop = !hand.getCommunityCards().isEmpty()
                    && !player.getHoleCards().isEmpty() && !preflopFolders.contains(id);
            next.get(id).completeHand(hand.getHandId(), sawFlop,
                    contested && player.isParticipating(), paid.getContestedPayouts().get(id),
                    paid.getRefunds().get(id));
        }
    }

    MatchStatistics addHand(MatchStatistics hand) {
        Map<String, PlayStats> next = new LinkedHashMap<>();
        players.forEach((id, stats) -> next.put(id, PlayStats.fromCompleted(
                stats.completedSnapshot().addHand(hand.players.get(id).completedSnapshot()))));
        return new MatchStatistics(next);
    }

    Map<String, CompletedStatistics> completedSnapshots() {
        Map<String, CompletedStatistics> snapshots = new LinkedHashMap<>();
        players.forEach((id, stats) -> snapshots.put(id, stats.completedSnapshot()));
        return Map.copyOf(snapshots);
    }

    Map<String, StatisticsView> snapshot() {
        Map<String, StatisticsView> views = new LinkedHashMap<>();
        players.forEach((id, stats) -> views.put(id, new StatisticsView(stats)));
        return Map.copyOf(views);
    }
}
