package domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Compares eligible players without mutating state or paying the pot. */
public final class ShowdownResolver {
    private final HandEvaluator evaluator;

    public ShowdownResolver() {
        this(new PokerHandEvaluator());
    }

    public ShowdownResolver(HandEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * Requires a dealt, decision-free state. Contested results require SHOWDOWN;
     * a sole remaining participant can win before the board is complete.
     * PotSettlement uses the same claimants to award each contribution layer.
     * @throws IllegalStateException for an unready/completed hand or no eligible player
     */
    public ShowdownResult resolve(GameState state) {
        return resolve(state, Set.of());
    }

    /** An ordinary showdown muck withdraws the claim, while retaining the betting record. */
    public ShowdownResult resolve(GameState state, Set<String> withdrawn) {
        requireDecisionFreeHand(state);
        List<PlayerState> eligible = eligibleContenders(state, withdrawn);
        if (eligible.size() == 1) {
            return new ShowdownResult(state.getHandId(),
                    List.of(eligible.get(0).getPlayer().getId()), true);
        }
        requireShowdownPhase(state);
        return new ShowdownResult(state.getHandId(), contestedWinnerIds(state, eligible), false);
    }

    private static void requireDecisionFreeHand(GameState state) {
        if (state.getPhase() == GamePhase.SETUP || state.getPhase() == GamePhase.COMPLETION
                || !state.getBettingProgress().getPlayersToAct().isEmpty()
                || !state.getBettingProgress().getPlayersAllowedToRaise().isEmpty()) {
            throw new IllegalStateException(
                    "A result requires a dealt hand with no pending decisions.");
        }
    }

    static List<PlayerState> eligibleContenders(GameState state, Set<String> withdrawn) {
        List<PlayerState> eligible = state.getPlayerStates().stream()
                .filter(PlayerState::isParticipating).collect(Collectors.toList());
        if (!withdrawn.isEmpty()) {
            if (state.getPhase() != GamePhase.SHOWDOWN || eligible.stream()
                    .anyMatch(seat -> seat.getStatus() == PlayerStatus.ALL_IN)) {
                throw new IllegalStateException("Only a non-all-in showdown permits mucking.");
            }
            int original = eligible.size();
            eligible.removeIf(seat -> withdrawn.contains(seat.getPlayer().getId()));
            if (original - eligible.size() != withdrawn.size()) {
                throw new IllegalArgumentException("Only live hands can withdraw a claim.");
            }
        }
        if (eligible.isEmpty()) {
            throw new IllegalStateException("At least one eligible player is required.");
        }
        return eligible;
    }

    private static void requireShowdownPhase(GameState state) {
        if (state.getPhase() != GamePhase.SHOWDOWN) {
            throw new IllegalStateException("Contested comparison requires the SHOWDOWN phase.");
        }
    }

    private List<String> contestedWinnerIds(GameState state, List<PlayerState> eligible) {
        List<Card> best = holding(state, eligible.get(0));
        List<String> winners = new ArrayList<>();
        winners.add(eligible.get(0).getPlayer().getId());
        for (int index = 1; index < eligible.size(); index++) {
            PlayerState player = eligible.get(index);
            List<Card> candidate = holding(state, player);
            int comparison = evaluator.compare(candidate, best);
            if (comparison > 0) {
                winners.clear();
                winners.add(player.getPlayer().getId());
                best = candidate;
            } else if (comparison == 0) {
                winners.add(player.getPlayer().getId());
            }
        }
        return winners;
    }

    private static List<Card> holding(GameState state, PlayerState player) {
        List<Card> cards = new ArrayList<>(state.getCommunityCards());
        cards.addAll(player.getHoleCards());
        return cards;
    }
}
