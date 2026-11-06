package domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Compares eligible players without mutating state or paying the pot. */
public final class ShowdownResolver {
    private final HandEvaluator evaluator;

    public ShowdownResolver() {
        this(new PokerHandEvaluator());
    }

    /** @throws NullPointerException if evaluator is null */
    public ShowdownResolver(HandEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    /**
     * Requires a dealt, decision-free state. Contested results require SHOWDOWN;
     * a sole remaining participant can win before the board is complete.
     * The result ranks contenders only: side-pot eligibility and payouts are later work.
     * @throws NullPointerException if state is null
     * @throws IllegalStateException for an unready/completed hand or no eligible player
     */
    public ShowdownResult resolve(GameState state) {
        Objects.requireNonNull(state, "state");
        if (state.getPhase() == GamePhase.SETUP || state.getPhase() == GamePhase.COMPLETION
                || !state.getBettingProgress().getPlayersToAct().isEmpty()
                || !state.getBettingProgress().getPlayersAllowedToRaise().isEmpty()) {
            throw new IllegalStateException(
                "A result requires a dealt hand with no pending decisions.");
        }
        List<PlayerState> eligible = state.getPlayerStates().stream()
                .filter(PlayerState::isParticipating).collect(Collectors.toList());
        if (eligible.isEmpty()) {
            throw new IllegalStateException("At least one eligible player is required.");
        }
        if (eligible.size() == 1) {
            return new ShowdownResult(state.getHandId(),
                    List.of(eligible.get(0).getPlayer().getId()), true);
        }
        if (state.getPhase() != GamePhase.SHOWDOWN) {
            throw new IllegalStateException("Contested comparison requires the SHOWDOWN phase.");
        }
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
        return new ShowdownResult(state.getHandId(), winners, false);
    }

    private static List<Card> holding(GameState state, PlayerState player) {
        List<Card> cards = new ArrayList<>(state.getCommunityCards());
        cards.addAll(player.getHoleCards());
        return cards;
    }
}
