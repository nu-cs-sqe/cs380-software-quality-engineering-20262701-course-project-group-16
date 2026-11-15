package domain.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Updates responses and individual raise rights after full wagers or short all-ins. */
final class BettingRound {
    private BettingRound() { }

    static BettingProgress after(GameState before, List<PlayerState> players,
            AcceptedAction action) {
        boolean isWager = action.getType() == ResolvedActionType.BET
                || action.getType() == ResolvedActionType.RAISE;
        long roundTarget = isWager
                ? action.getRoundContributionAfter() : before.getCurrentBet();
        long raiseIncrease = roundTarget - before.getCurrentBet();
        long minimumFullRaise = before.getCurrentBet() == 0 ? before.getConfig().getBigBlind()
                : before.getLastFullRaiseSize();
        boolean fullWager = isWager && raiseIncrease >= minimumFullRaise;
        long lastFullRaiseSize = isWager
                ? Math.max(minimumFullRaise, raiseIncrease) : before.getLastFullRaiseSize();
        Set<String> activePlayers = activeIds(players);
        Map<String, Long> lastDecisions = decisionTargets(before, action, activePlayers);
        // Response obligations and raise rights differ after a short all-in.
        Set<String> pendingDecisions = pendingAfter(before, players, roundTarget, fullWager);
        Set<String> raiseEligiblePlayers = rightsAfter(before, activePlayers, lastDecisions,
                roundTarget, lastFullRaiseSize, fullWager);
        pendingDecisions.remove(action.getPlayerId());
        raiseEligiblePlayers.remove(action.getPlayerId());
        closeMatchedLoneActor(players, pendingDecisions, raiseEligiblePlayers);
        if (pendingDecisions.isEmpty()) {
            raiseEligiblePlayers.clear();
        }
        if (activePlayers.size() < 2) {
            roundTarget = players.stream().mapToLong(PlayerState::getRoundContribution)
                    .max().orElseThrow();
        }
        return new BettingProgress(roundTarget, lastFullRaiseSize, pendingDecisions,
                raiseEligiblePlayers, lastDecisions);
    }

    private static Set<String> activeIds(List<PlayerState> players) {
        return players.stream().filter(PlayerState::canAct)
                .map(player -> player.getPlayer().getId()).collect(Collectors.toSet());
    }

    private static Map<String, Long> decisionTargets(GameState before, AcceptedAction action,
            Set<String> activePlayers) {
        Map<String, Long> decisionTargets = new HashMap<>(
                before.getBettingProgress().getLastDecisionTargets());
        decisionTargets.keySet().retainAll(activePlayers);
        if (activePlayers.contains(action.getPlayerId())) {
            decisionTargets.put(action.getPlayerId(), action.getRoundContributionAfter());
        }
        return decisionTargets;
    }

    private static Set<String> pendingAfter(GameState before, List<PlayerState> players,
            long roundTarget, boolean fullWager) {
        Set<String> pendingDecisions =
                new HashSet<>(before.getBettingProgress().getPlayersToAct());
        pendingDecisions.retainAll(activeIds(players));
        for (PlayerState player : players) {
            if (player.canAct()
                    && (fullWager || player.getRoundContribution() < roundTarget)) {
                pendingDecisions.add(player.getPlayer().getId());
            }
        }
        return pendingDecisions;
    }

    private static Set<String> rightsAfter(GameState before, Set<String> activePlayers,
            Map<String, Long> decisionTargets, long roundTarget, long lastFullRaiseSize,
            boolean fullWager) {
        Set<String> raiseEligiblePlayers = new HashSet<>(fullWager ? activePlayers
                : before.getBettingProgress().getPlayersAllowedToRaise());
        raiseEligiblePlayers.retainAll(activePlayers);
        decisionTargets.forEach((id, level) -> {
            if (roundTarget - level >= lastFullRaiseSize) {
                raiseEligiblePlayers.add(id);
            }
        });
        return raiseEligiblePlayers;
    }

    private static void closeMatchedLoneActor(List<PlayerState> players,
            Set<String> pendingDecisions,
            Set<String> raiseEligiblePlayers) {
        List<PlayerState> activePlayers = players.stream().filter(PlayerState::canAct)
                .collect(Collectors.toList());
        if (activePlayers.size() == 1) {
            raiseEligiblePlayers.clear();
            PlayerState actor = activePlayers.get(0);
            long opposingBet = players.stream().filter(PlayerState::isParticipating)
                    .filter(player -> player != actor).mapToLong(PlayerState::getRoundContribution)
                    .max().orElse(0);
            if (actor.getRoundContribution() >= opposingBet) {
                pendingDecisions.clear();
            }
        }
    }
}
