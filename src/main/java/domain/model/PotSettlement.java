package domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Pure contribution-layer settlement; folded chips stay in pots, refunds stay separate. */
public final class PotSettlement {
    private final HandEvaluator evaluator;

    public PotSettlement() { this(new PokerHandEvaluator()); }

    public PotSettlement(HandEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    /**
     * Pays a decision-free showdown or uncontested hand, returning a completed snapshot.
     * Tied pots give odd chips clockwise after the dealer. The input is never modified.
     * @throws IllegalStateException unless the hand is ready and all pots have eligible winners
     * @throws NullPointerException if state is null
     */
    public Settlement settle(GameState state) {
        requireReady(state);
        Map<String, Long> payouts = zeroAmounts(state);
        Map<String, Long> refunds = zeroAmounts(state);
        Map<String, Long> contested = zeroAmounts(state);
        distributeLayers(state, payouts, refunds, contested);
        return new Settlement(completedState(state, payouts, refunds), payouts, refunds, contested);
    }

    private static void requireReady(GameState state) {
        Objects.requireNonNull(state, "state");
        long live = state.getPlayerStates().stream().filter(PlayerState::isParticipating).count();
        if (state.getPhase() == GamePhase.SETUP || state.getPhase() == GamePhase.COMPLETION
                || state.getCurrentActorId().isPresent()
                || !state.getBettingProgress().getPlayersAllowedToRaise().isEmpty()
                || live == 0 || (live > 1 && state.getPhase() != GamePhase.SHOWDOWN)) {
            throw new IllegalStateException("Finish the betting decisions before settling.");
        }
    }

    private static Map<String, Long> zeroAmounts(GameState state) {
        Map<String, Long> amounts = new LinkedHashMap<>();
        for (Player player : state.getPlayers()) {
            amounts.put(player.getId(), 0L);
        }
        return amounts;
    }

    private void distributeLayers(GameState state, Map<String, Long> payouts,
            Map<String, Long> refunds, Map<String, Long> contested) {
        List<Long> levels = state.getPlayerStates().stream().map(PlayerState::getHandContribution)
                .filter(amount -> amount > 0).distinct().sorted().collect(Collectors.toList());
        long previous = 0;
        for (long level : levels) {
            List<PlayerState> contributors = state.getPlayerStates().stream()
                    .filter(player -> player.getHandContribution() >= level)
                    .collect(Collectors.toList());
            long amount = Math.multiplyExact(level - previous, contributors.size());
            payLayer(state, contributors, amount, payouts, refunds, contested);
            previous = level;
        }
    }

    private void payLayer(GameState state, List<PlayerState> contributors, long amount,
            Map<String, Long> payouts, Map<String, Long> refunds, Map<String, Long> contested) {
        if (contributors.size() == 1) {
            add(refunds, contributors.get(0), amount);
            return;
        }
        List<PlayerState> eligible = contributors.stream().filter(PlayerState::isParticipating)
                .collect(Collectors.toList());
        if (eligible.isEmpty()) {
            throw new IllegalStateException("A side pot requires an eligible player.");
        }
        List<PlayerState> winners = winners(state, eligible);
        winners.sort(Comparator.comparingInt(player -> Math.floorMod(
                player.getPlayer().getSeat() - state.getDealerSeat() - 1, 6)));
        for (int index = 0; index < winners.size(); index++) {
            long share = amount / winners.size() + (index < amount % winners.size() ? 1 : 0);
            add(payouts, winners.get(index), share);
            if (eligible.size() > 1) {
                add(contested, winners.get(index), share);
            }
        }
    }

    private List<PlayerState> winners(GameState state, List<PlayerState> eligible) {
        List<PlayerState> winners = new ArrayList<>();
        winners.add(eligible.get(0));
        for (int index = 1; index < eligible.size(); index++) {
            PlayerState candidate = eligible.get(index);
            int comparison = evaluator.compare(holding(state, candidate),
                    holding(state, winners.get(0)));
            if (comparison > 0) {
                winners.clear();
            }
            if (comparison >= 0) {
                winners.add(candidate);
            }
        }
        return winners;
    }

    private static List<Card> holding(GameState state, PlayerState player) {
        List<Card> cards = new ArrayList<>(state.getCommunityCards());
        cards.addAll(player.getHoleCards());
        return cards;
    }

    private static void add(Map<String, Long> amounts, PlayerState player, long amount) {
        String id = player.getPlayer().getId();
        amounts.put(id, Math.addExact(amounts.get(id), amount));
    }

    private static GameState completedState(GameState before, Map<String, Long> payouts,
            Map<String, Long> refunds) {
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState state : before.getPlayerStates()) {
            Player old = state.getPlayer();
            long chips = Math.addExact(old.getChipStack(),
                    Math.addExact(payouts.get(old.getId()), refunds.get(old.getId())));
            Player paid = new Player(old.getId(), old.getSeat(), old.getType(), chips);
            players.add(new PlayerState(paid, chips == 0 ? PlayerStatus.ELIMINATED
                    : PlayerStatus.ACTIVE, chips == 0 ? List.of() : state.getHoleCards(), 0, 0));
        }
        return new GameState(before.getConfig(), before.getHandId(), GamePhase.COMPLETION,
                players, before.getPositions(), null, before.getCommunityCards(),
                before.getRemainingDeck(), new BettingProgress(0, before.getConfig().getBigBlind(),
                Set.of(), Set.of()), before.getBettingHistory());
    }
}
