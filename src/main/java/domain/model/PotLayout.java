package domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Public contribution layers and refunds; retains no cards, deck or game snapshot. */
public final class PotLayout {
    private final List<Layer> layers;
    private final Map<String, Long> refunds;

    private PotLayout(List<Layer> layers, Map<String, Long> refunds) {
        this.layers = List.copyOf(layers);
        this.refunds = Map.copyOf(refunds);
    }

    public static PotLayout from(GameState state) {
        return contributionLayers(state.getPlayerStates());
    }

    private static PotLayout contributionLayers(List<PlayerState> players) {
        List<Layer> layers = new ArrayList<>();
        Map<String, Long> refunds = zeroRefunds(players);
        List<Long> levels = players.stream().map(PlayerState::getHandContribution)
                .filter(amount -> amount > 0).distinct().sorted().collect(Collectors.toList());
        long previous = 0;
        for (long level : levels) {
            List<PlayerState> contributors = players.stream()
                    .filter(player -> player.getHandContribution() >= level)
                    .collect(Collectors.toList());
            addLayer(contributors, Math.multiplyExact(level - previous, contributors.size()),
                    layers, refunds);
            previous = level;
        }
        return new PotLayout(layers, refunds);
    }

    private static Map<String, Long> zeroRefunds(List<PlayerState> players) {
        Map<String, Long> amounts = new LinkedHashMap<>();
        players.forEach(player -> amounts.put(player.getPlayer().getId(), 0L));
        return amounts;
    }

    private static void addLayer(List<PlayerState> contributors, long amount,
            List<Layer> layers, Map<String, Long> refunds) {
        if (contributors.size() == 1) {
            String id = contributors.get(0).getPlayer().getId();
            refunds.put(id, Math.addExact(refunds.get(id), amount));
            return;
        }
        Set<String> eligible = contributors.stream().filter(PlayerState::isParticipating)
                .map(player -> player.getPlayer().getId()).collect(Collectors.toSet());
        if (eligible.isEmpty()) {
            throw new IllegalStateException("A side pot requires an eligible player.");
        }
        layers.add(new Layer(amount, eligible));
    }

    public List<Layer> getLayers() { return layers; }
    public Map<String, Long> getRefunds() { return refunds; }

    /** Awardable pot slice; a unique-contributor layer is represented only as a refund. */
    public static final class Layer {
        private final long amount;
        private final Set<String> eligible;

        private Layer(long amount, Set<String> eligible) {
            this.amount = amount;
            this.eligible = Set.copyOf(eligible);
        }

        public long getAmount() { return amount; }
        public Set<String> getEligiblePlayers() { return eligible; }
    }
}
