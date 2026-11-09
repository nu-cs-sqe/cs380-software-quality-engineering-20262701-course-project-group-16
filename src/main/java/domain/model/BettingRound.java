package domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Updates decisions and raise rights after a passive action or a full wager. */
final class BettingRound {
    private BettingRound() { }

    static BettingProgress after(GameState before, List<PlayerState> players,
            AcceptedAction action) {
        boolean wager = action.getType() == ResolvedActionType.BET
                || action.getType() == ResolvedActionType.RAISE;
        Set<String> active = players.stream().filter(PlayerState::canAct)
                .map(player -> player.getPlayer().getId()).collect(Collectors.toSet());
        Set<String> pending = new HashSet<>(wager ? active
                : before.getBettingProgress().getPlayersToAct());
        Set<String> raising = new HashSet<>(wager ? active
                : before.getBettingProgress().getPlayersAllowedToRaise());
        pending.remove(action.getPlayerId());
        raising.remove(action.getPlayerId());
        closeMatchedLoneActor(players, pending, raising);
        if (players.stream().filter(PlayerState::isParticipating).count() <= 1
                || pending.isEmpty()) {
            pending.clear();
            raising.clear();
        }
        long target = wager ? action.getRoundContributionAfter() : before.getCurrentBet();
        if (active.size() < 2) {
            target = players.stream().mapToLong(PlayerState::getRoundContribution)
                    .max().orElseThrow();
        }
        long increment = wager ? action.getRoundContributionAfter() - before.getCurrentBet()
                : before.getLastFullRaiseSize();
        return new BettingProgress(target, increment, pending, raising);
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
