package domain.model;

/** Validates round targets and computes deductions without changing any player. */
final class BettingAmounts {
    private BettingAmounts() { }

    static long chipsAdded(GameState state, PlayerState actor, ActionRequest request) {
        long owed = state.getCurrentBet() - actor.getRoundContribution();
        switch (request.getType()) {
            case FOLD:
                return 0;
            case CHECK:
                if (owed != 0) {
                    throw new IllegalArgumentException("Call or fold when chips are owed.");
                }
                return 0;
            case CALL:
                if (owed == 0) {
                    throw new IllegalArgumentException("Nothing is owed; check instead.");
                }
                return Math.min(owed, actor.getPlayer().getChipStack());
            default:
                return wagerDelta(state, actor, request);
        }
    }

    private static long wagerDelta(GameState state, PlayerState actor, ActionRequest request) {
        boolean opening = state.getCurrentBet() == 0;
        if ((request.getType() == ActionType.BET) != opening
                || (opening && state.getPhase() == GamePhase.PREFLOP)) {
            throw new IllegalArgumentException("Bet into an unopened round; otherwise raise.");
        }
        long target = request.getTargetAmount().orElseThrow();
        long maximum = actor.getRoundContribution() + actor.getPlayer().getChipStack();
        if (!hasWagerRights(state, actor) || target > maximum || target <= state.getCurrentBet()) {
            throw new IllegalArgumentException(
                    "The wager exceeds this player's available limits.");
        }
        long minimumIncrement = opening ? state.getConfig().getBigBlind()
                : state.getLastFullRaiseSize();
        if (target - state.getCurrentBet() < minimumIncrement) {
            throw new IllegalArgumentException("The wager is below the full bet or raise minimum.");
        }
        return target - actor.getRoundContribution();
    }

    static boolean canMakeFullWager(GameState state, PlayerState actor) {
        long maximum = actor.getRoundContribution() + actor.getPlayer().getChipStack();
        long increment = state.getCurrentBet() == 0 ? state.getConfig().getBigBlind()
                : state.getLastFullRaiseSize();
        return hasWagerRights(state, actor) && maximum > state.getCurrentBet()
                && maximum - state.getCurrentBet() >= increment;
    }

    private static boolean hasWagerRights(GameState state, PlayerState actor) {
        return state.getBettingProgress().getPlayersAllowedToRaise()
                .contains(actor.getPlayer().getId())
                && state.getPlayerStates().stream().filter(PlayerState::canAct).count() > 1;
    }
}
