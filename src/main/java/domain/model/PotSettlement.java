package domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Pure contribution-layer settlement; folded chips stay in pots, refunds stay separate. */
public final class PotSettlement {
    private final HandEvaluator evaluator;

    public PotSettlement() { this(new PokerHandEvaluator()); }

    public PotSettlement(HandEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * Pays a decision-free showdown or uncontested hand, returning a completed snapshot.
     * Tied pots give odd chips clockwise after the dealer. The input is never modified.
     * @throws IllegalStateException unless the hand is ready and all pots have eligible winners
     */
    public Settlement settle(GameState state) {
        requireReady(state);
        Map<String, Long> payouts = zeroAmounts(state);
        PotLayout layout = PotLayout.from(state);
        Map<String, Long> refunds = layout.getRefunds();
        Map<String, Long> contested = zeroAmounts(state);
        distributeLayers(state, layout.getLayers(), payouts, contested);
        return new Settlement(completedState(state, payouts, refunds), payouts, refunds, contested);
    }

    private static void requireReady(GameState state) {
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

    private void distributeLayers(GameState state, List<PotLayout.Layer> layers,
            Map<String, Long> payouts, Map<String, Long> contested) {
        for (PotLayout.Layer layer : layers) {
            List<PlayerState> eligible = state.getPlayerStates().stream()
                    .filter(player -> layer.getEligiblePlayers()
                            .contains(player.getPlayer().getId()))
                    .collect(Collectors.toList());
            payLayer(state, eligible, layer.getAmount(), payouts, contested);
        }
    }

    private void payLayer(GameState state, List<PlayerState> eligible, long amount,
            Map<String, Long> payouts, Map<String, Long> contested) {
        List<PlayerState> winners = winners(state, eligible);
        // Odd chips go clockwise from the seat immediately left of the dealer.
        winners.sort(Comparator.comparingInt(player -> Math.floorMod(
                player.getPlayer().getSeat() - state.getDealerSeat() - 1, GameConfig.SEAT_COUNT)));
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

    private static PlayerStatus paidStatus(PlayerState state, long chips) {
        if (state.getStatus() == PlayerStatus.DEPARTED) {
            return PlayerStatus.DEPARTED;
        }
        return chips == 0 ? PlayerStatus.ELIMINATED : PlayerStatus.ACTIVE;
    }

    private static GameState completedState(GameState before, Map<String, Long> payouts,
            Map<String, Long> refunds) {
        List<PlayerState> players = paidPlayers(before, payouts, refunds);
        return GameState.create(before.getConfig(), before.getHandId(), GamePhase.COMPLETION,
                players, before.getPositions(), Optional.empty(), before.getCommunityCards(),
                before.getRemainingDeck(), new BettingProgress(0, before.getConfig().getBigBlind(),
                Set.of(), Set.of()), before.getBettingHistory());
    }

    private static List<PlayerState> paidPlayers(GameState before, Map<String, Long> payouts,
            Map<String, Long> refunds) {
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState state : before.getPlayerStates()) {
            Player old = state.getPlayer();
            long chips = Math.addExact(old.getChipStack(),
                    Math.addExact(payouts.get(old.getId()), refunds.get(old.getId())));
            Player paid = new Player(old.getId(), old.getSeat(), old.getType(), chips);
            PlayerStatus status = paidStatus(state, chips);
            players.add(new PlayerState(paid, status,
                    chips == 0 ? List.of() : state.getHoleCards(), 0, 0));
        }
        return players;
    }
}
