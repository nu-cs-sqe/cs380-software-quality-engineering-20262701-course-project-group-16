package domain.model;

import java.util.Optional;

/** Validates round targets and computes deductions without changing any player. */
final class BettingAmounts {
    private BettingAmounts() { }

    static long chipsAdded(GameState state, PlayerState actor, ActionRequest request) {
        long call = callCost(state, actor);
        switch (request.getType()) {
            case FOLD:
                return 0;
            case CHECK:
                if (call != 0) {
                    throw new IllegalArgumentException("Call or fold when chips are owed.");
                }
                return 0;
            case CALL:
                if (call == 0) {
                    throw new IllegalArgumentException("Nothing is owed; check instead.");
                }
                return call;
            default:
                return wagerDelta(state, actor, request);
        }
    }

    static long callCost(GameState state, PlayerState actor) {
        return Math.min(state.getCurrentBet() - actor.getRoundContribution(),
                actor.getPlayer().getChipStack());
    }

    private static long wagerDelta(GameState state, PlayerState actor, ActionRequest request) {
        long target = request.getTargetAmount().orElseThrow();
        LegalActions.Wager limits = wager(state, actor).orElseThrow(() ->
                new IllegalArgumentException("This player cannot increase the wager."));
        if (request.getType() != limits.getType() || !limits.allows(target)) {
            throw new IllegalArgumentException("Choose a legal wager type and target.");
        }
        return target - actor.getRoundContribution();
    }

    static Optional<LegalActions.Wager> wager(GameState state, PlayerState actor) {
        if (!canWager(state, actor)) {
            return Optional.empty();
        }
        long current = state.getCurrentBet();
        boolean opening = current == 0;
        return Optional.of(new LegalActions.Wager(opening ? ActionType.BET : ActionType.RAISE,
                current, opening ? state.getConfig().getBigBlind() : state.getLastFullRaiseSize(),
                maximumTarget(actor)));
    }

    static boolean canWager(GameState state, PlayerState actor) {
        return !(state.getCurrentBet() == 0 && state.getPhase() == GamePhase.PREFLOP)
                && hasWagerRights(state, actor) && maximumTarget(actor) > state.getCurrentBet();
    }

    private static long maximumTarget(PlayerState actor) {
        return actor.getRoundContribution() + actor.getPlayer().getChipStack();
    }

    private static boolean hasWagerRights(GameState state, PlayerState actor) {
        return state.getBettingProgress().getPlayersAllowedToRaise()
                .contains(actor.getPlayer().getId())
                && state.getPlayerStates().stream().filter(PlayerState::canAct).count() > 1;
    }
}
