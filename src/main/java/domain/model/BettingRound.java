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
        boolean wager = action.getType() == ResolvedActionType.BET
                || action.getType() == ResolvedActionType.RAISE;
        long target = wager ? action.getRoundContributionAfter() : before.getCurrentBet();
        long increase = target - before.getCurrentBet();
        long minimum = before.getCurrentBet() == 0 ? before.getConfig().getBigBlind()
                : before.getLastFullRaiseSize();
        boolean full = wager && increase >= minimum;
        long increment = wager ? Math.max(minimum, increase) : before.getLastFullRaiseSize();
        Set<String> active = activeIds(players);
        Map<String, Long> decisions = decisionTargets(before, action, active);
        Set<String> pending = pendingAfter(before, players, target, full);
        Set<String> raising = rightsAfter(before, active, decisions, target, increment, full);
        pending.remove(action.getPlayerId());
        raising.remove(action.getPlayerId());
        closeMatchedLoneActor(players, pending, raising);
        if (pending.isEmpty()) {
            raising.clear();
        }
        if (active.size() < 2) {
            target = players.stream().mapToLong(PlayerState::getRoundContribution)
                    .max().orElseThrow();
        }
        return new BettingProgress(target, increment, pending, raising, decisions);
    }

    private static Set<String> activeIds(List<PlayerState> players) {
        return players.stream().filter(PlayerState::canAct)
                .map(player -> player.getPlayer().getId()).collect(Collectors.toSet());
    }

    private static Map<String, Long> decisionTargets(GameState before, AcceptedAction action,
            Set<String> active) {
        Map<String, Long> decisions = new HashMap<>(
                before.getBettingProgress().getLastDecisionTargets());
        decisions.keySet().retainAll(active);
        if (active.contains(action.getPlayerId())) {
            decisions.put(action.getPlayerId(), action.getRoundContributionAfter());
        }
        return decisions;
    }

    private static Set<String> pendingAfter(GameState before, List<PlayerState> players,
            long target, boolean full) {
        Set<String> pending = new HashSet<>(before.getBettingProgress().getPlayersToAct());
        pending.retainAll(activeIds(players));
        for (PlayerState player : players) {
            if (player.canAct() && (full || player.getRoundContribution() < target)) {
                pending.add(player.getPlayer().getId());
            }
        }
        return pending;
    }

    private static Set<String> rightsAfter(GameState before, Set<String> active,
            Map<String, Long> decisions, long target, long increment, boolean full) {
        Set<String> raising = new HashSet<>(full ? active
                : before.getBettingProgress().getPlayersAllowedToRaise());
        raising.retainAll(active);
        decisions.forEach((id, level) -> {
            if (target - level >= increment) {
                raising.add(id);
            }
        });
        return raising;
    }

    private static void closeMatchedLoneActor(List<PlayerState> players, Set<String> pending,
            Set<String> raising) {
        List<PlayerState> active = players.stream().filter(PlayerState::canAct)
                .collect(Collectors.toList());
        if (active.size() == 1) {
            raising.clear();
            PlayerState actor = active.get(0);
            long opposingBet = players.stream().filter(PlayerState::isParticipating)
                    .filter(player -> player != actor).mapToLong(PlayerState::getRoundContribution)
                    .max().orElse(0);
            if (actor.getRoundContribution() >= opposingBet) {
                pending.clear();
            }
        }
    }
}
